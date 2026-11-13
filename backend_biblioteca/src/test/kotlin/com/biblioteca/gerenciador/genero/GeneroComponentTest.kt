package com.biblioteca.gerenciador.genero

import com.biblioteca.gerenciador.config.ClockFixoConfig
import com.biblioteca.gerenciador.config.ComponentTestBase
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get

class GeneroComponentTest : ComponentTestBase() {

    @Test
    fun `cria genero`() {
        postJson("/api/v1/genres", """{ "nome": "  Ficção Científica  " }""").andExpect {
            status { isCreated() }
            header { exists("Location") }
            jsonPath("$.id") { isNumber() }
            jsonPath("$.nome") { value("Ficção Científica") }
        }
    }

    @Test
    fun `nao permite genero com nome repetido, ignorando maiusculas`() {
        dados.genero("Suspense")

        postJson("/api/v1/genres", """{ "nome": "SUSPENSE" }""").andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("GENRE_ALREADY_EXISTS") }
        }
    }

    @Test
    fun `nome obrigatorio retorna erro no formato padrao`() {
        postJson("/api/v1/genres", "{}").andExpect {
            status { isBadRequest() }
            jsonPath("$.status") { value(400) }
            jsonPath("$.code") { value("VALIDATION_ERROR") }
            jsonPath("$.fields[0].field") { value("nome") }
            jsonPath("$.timestamp") { value(ClockFixoConfig.AGORA.toString()) }
            jsonPath("$.path") { value("/api/v1/genres") }
        }
    }

    @Test
    fun `lista generos em ordem alfabetica`() {
        dados.genero("Terror")
        dados.genero("Aventura")

        mockMvc.get("/api/v1/genres").andExpect {
            status { isOk() }
            jsonPath("$[0].nome") { value("Aventura") }
            jsonPath("$[1].nome") { value("Terror") }
        }
    }

    @Test
    fun `renomeia genero`() {
        val genero = dados.genero("Policial")

        putJson("/api/v1/genres/${genero.id}", """{ "nome": "Policial Noir" }""").andExpect {
            status { isOk() }
            jsonPath("$.nome") { value("Policial Noir") }
        }
    }

    @Test
    fun `renomear genero inexistente retorna 404`() {
        putJson("/api/v1/genres/9999", """{ "nome": "Qualquer" }""").andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("GENRE_NOT_FOUND") }
        }
    }

    @Test
    fun `renomear para nome de outro genero retorna 409`() {
        dados.genero("Drama")
        val genero = dados.genero("Comédia")

        putJson("/api/v1/genres/${genero.id}", """{ "nome": "drama" }""").andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("GENRE_ALREADY_EXISTS") }
        }
    }
}

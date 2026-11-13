package com.biblioteca.gerenciador.exemplar

import com.biblioteca.gerenciador.config.ComponentTestBase
import com.biblioteca.gerenciador.enums.CondicaoExemplar
import com.biblioteca.gerenciador.enums.StatusExemplar
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch

class ExemplarComponentTest : ComponentTestBase() {

    private fun patchJson(url: String, json: String) = mockMvc.patch(url) {
        contentType = org.springframework.http.MediaType.APPLICATION_JSON
        content = json
    }

    // ---------- cadastro ----------

    @Test
    fun `adiciona exemplares com codigo sequencial`() {
        val livro = dados.livro()
        val prefixo = "LIV%06d".format(livro.id)

        postJson("/api/v1/books/${livro.id}/copies", """{ "quantidade": 3 }""").andExpect {
            status { isCreated() }
            jsonPath("$.length()") { value(3) }
            jsonPath("$[*].codigo") { value(contains("$prefixo-EX01", "$prefixo-EX02", "$prefixo-EX03")) }
            jsonPath("$[0].status") { value("DISPONIVEL") }
            jsonPath("$[0].condicao") { value("NOVO") }
            jsonPath("$[0].livroId") { value(livro.id!!.toInt()) }
        }
    }

    @Test
    fun `sequencia continua a partir dos exemplares existentes`() {
        val livro = dados.livro()
        dados.exemplar(livro)
        dados.exemplar(livro)

        postJson("/api/v1/books/${livro.id}/copies", """{ "quantidade": 1 }""").andExpect {
            jsonPath("$[0].codigo") { value("LIV%06d-EX03".format(livro.id)) }
        }
    }

    @Test
    fun `exemplar cadastrado danificado ja entra em manutencao`() {
        val livro = dados.livro()

        postJson("/api/v1/books/${livro.id}/copies", """{ "condicao": "DANIFICADO" }""").andExpect {
            status { isCreated() }
            jsonPath("$[0].status") { value("MANUTENCAO") }
        }
    }

    @Test
    fun `quantidade fora do limite retorna 400`() {
        val livro = dados.livro()

        postJson("/api/v1/books/${livro.id}/copies", """{ "quantidade": 51 }""").andExpect {
            status { isBadRequest() }
            jsonPath("$.fields[0].field") { value("quantidade") }
        }
    }

    @Test
    fun `condicao inexistente retorna 400`() {
        val livro = dados.livro()

        postJson("/api/v1/books/${livro.id}/copies", """{ "condicao": "OTIMO" }""").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_BODY") }
        }
    }

    @Test
    fun `livro inexistente ou inativo retorna 404`() {
        postJson("/api/v1/books/9999/copies", "{}").andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("BOOK_NOT_FOUND") }
        }
    }

    @Test
    fun `lista exemplares do livro ordenados por codigo`() {
        val livro = dados.livro()
        dados.exemplar(livro, codigo = "B-2")
        dados.exemplar(livro, codigo = "A-1")
        dados.exemplar(dados.livro(), codigo = "OUTRO-LIVRO")

        mockMvc.get("/api/v1/books/${livro.id}/copies").andExpect {
            status { isOk() }
            jsonPath("$[*].codigo") { value(contains("A-1", "B-2")) }
        }
    }

    // ---------- alteração (PATCH) ----------

    @Test
    fun `altera condicao e envia para manutencao`() {
        val exemplar = dados.exemplar(dados.livro())

        patchJson("/api/v1/copies/${exemplar.id}", """{ "condicao": "MAL_CONSERVADO", "status": "MANUTENCAO" }""")
            .andExpect {
                status { isOk() }
                jsonPath("$.condicao") { value("MAL_CONSERVADO") }
                jsonPath("$.status") { value("MANUTENCAO") }
            }
    }

    @Test
    fun `altera somente o codigo`() {
        val exemplar = dados.exemplar(dados.livro())

        patchJson("/api/v1/copies/${exemplar.id}", """{ "codigo": "ACERVO-ANTIGO-01" }""").andExpect {
            status { isOk() }
            jsonPath("$.codigo") { value("ACERVO-ANTIGO-01") }
            jsonPath("$.status") { value("DISPONIVEL") }
        }
    }

    @Test
    fun `codigo repetido retorna 409`() {
        val livro = dados.livro()
        dados.exemplar(livro, codigo = "JA-EXISTE")
        val exemplar = dados.exemplar(livro)

        patchJson("/api/v1/copies/${exemplar.id}", """{ "codigo": "JA-EXISTE" }""").andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("COPY_CODE_ALREADY_EXISTS") }
        }
    }

    @Test
    fun `exemplar baixado nao volta a circular`() {
        val exemplar = dados.exemplar(dados.livro(), status = StatusExemplar.BAIXADO)

        patchJson("/api/v1/copies/${exemplar.id}", """{ "status": "DISPONIVEL" }""").andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("INVALID_STATUS_TRANSITION") }
        }
    }

    @Test
    fun `nao permite emprestar pelo PATCH`() {
        val exemplar = dados.exemplar(dados.livro())

        patchJson("/api/v1/copies/${exemplar.id}", """{ "status": "EMPRESTADO" }""").andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("INVALID_STATUS_TRANSITION") }
        }
    }

    @Test
    fun `exemplar emprestado nao pode ser alterado`() {
        val exemplar = dados.exemplar(dados.livro(), status = StatusExemplar.EMPRESTADO)

        patchJson("/api/v1/copies/${exemplar.id}", """{ "condicao": "EXCELENTE" }""").andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("COPY_ON_LOAN") }
        }
    }

    @Test
    fun `exemplar danificado nao pode ficar disponivel`() {
        val exemplar = dados.exemplar(dados.livro())

        patchJson("/api/v1/copies/${exemplar.id}", """{ "condicao": "DANIFICADO" }""").andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("DAMAGED_COPY_UNAVAILABLE") }
        }
    }

    @Test
    fun `exemplar danificado em manutencao continua em manutencao`() {
        val exemplar = dados.exemplar(
            dados.livro(),
            status = StatusExemplar.MANUTENCAO,
            condicao = CondicaoExemplar.DANIFICADO,
        )

        patchJson("/api/v1/copies/${exemplar.id}", """{ "condicao": "CONSERVADO", "status": "DISPONIVEL" }""")
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("DISPONIVEL") }
            }
    }

    @Test
    fun `exemplar inexistente retorna 404`() {
        mockMvc.get("/api/v1/copies/9999").andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("COPY_NOT_FOUND") }
        }
    }
}

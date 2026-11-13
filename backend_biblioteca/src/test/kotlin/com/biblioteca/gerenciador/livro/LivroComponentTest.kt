package com.biblioteca.gerenciador.livro

import com.biblioteca.gerenciador.config.ComponentTestBase
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get

class LivroComponentTest : ComponentTestBase() {

    @Test
    fun `cadastra livro com generos`() {
        val romance = dados.genero("Romance")
        val classico = dados.genero("Clássico")

        postJson(
            "/api/v1/books",
            """
            {
              "titulo": "Dom Casmurro",
              "autor": "Machado de Assis",
              "isbn": "978-85-359-0277-1",
              "editora": "Penguin",
              "anoPublicacao": 1899,
              "generoIds": [${romance.id}, ${classico.id}]
            }
            """,
        ).andExpect {
            status { isCreated() }
            jsonPath("$.id") { isNumber() }
            jsonPath("$.titulo") { value("Dom Casmurro") }
            jsonPath("$.generos[*].nome") { value(containsInAnyOrder("Romance", "Clássico")) }
            jsonPath("$.dataCadastro") { exists() }
            jsonPath("$.cadastradoPor") { value("Administrador") } // UsuarioLogado provisório
        }
    }

    @Test
    fun `valida campos obrigatorios`() {
        postJson("/api/v1/books", """{ "titulo": "" }""").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_ERROR") }
            jsonPath("$.fields[*].field") { value(containsInAnyOrder("titulo", "autor", "generoIds")) }
        }
    }

    @Test
    fun `genero inexistente retorna 404`() {
        postJson("/api/v1/books", """{ "titulo": "X", "autor": "Y", "generoIds": [9999] }""").andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("GENRE_NOT_FOUND") }
        }
    }

    @Test
    fun `nao permite isbn repetido`() {
        val genero = dados.genero()
        dados.livro(isbn = "123", generos = setOf(genero))

        postJson("/api/v1/books", """{ "titulo": "Outro", "autor": "Y", "isbn": "123", "generoIds": [${genero.id}] }""")
            .andExpect {
                status { isConflict() }
                jsonPath("$.code") { value("ISBN_ALREADY_EXISTS") }
            }
    }

    @Test
    fun `pesquisa por parte do titulo e por genero, paginado`() {
        val fantasia = dados.genero("Fantasia")
        val drama = dados.genero("Drama")
        dados.livro(titulo = "O Senhor dos Anéis", autor = "Tolkien", generos = setOf(fantasia))
        dados.livro(titulo = "O Hobbit", autor = "Tolkien", generos = setOf(fantasia))
        dados.livro(titulo = "Senhora", autor = "José de Alencar", generos = setOf(drama))

        mockMvc.get("/api/v1/books?titulo=senhor").andExpect {
            status { isOk() }
            jsonPath("$.totalElements") { value(2) }
            jsonPath("$.content[*].titulo") { value(containsInAnyOrder("O Senhor dos Anéis", "Senhora")) }
        }

        mockMvc.get("/api/v1/books?generoId=${fantasia.id}&size=1").andExpect {
            status { isOk() }
            jsonPath("$.content.length()") { value(1) }
            jsonPath("$.content[0].titulo") { value("O Hobbit") } // ordenação padrão por título
            jsonPath("$.totalElements") { value(2) }
            jsonPath("$.totalPages") { value(2) }
            jsonPath("$.size") { value(1) }
        }
    }

    @Test
    fun `atualiza livro`() {
        val livro = dados.livro(titulo = "Titulo errado")
        val genero = dados.genero("Biografia")

        putJson("/api/v1/books/${livro.id}", """{ "titulo": "Titulo certo", "autor": "Autor", "generoIds": [${genero.id}] }""")
            .andExpect {
                status { isOk() }
                jsonPath("$.titulo") { value("Titulo certo") }
                jsonPath("$.generos.length()") { value(1) }
                jsonPath("$.generos[0].nome") { value("Biografia") }
            }
    }

    @Test
    fun `livro inativado some das consultas`() {
        val livro = dados.livro(titulo = "Livro antigo")

        mockMvc.delete("/api/v1/books/${livro.id}").andExpect { status { isNoContent() } }

        mockMvc.get("/api/v1/books/${livro.id}").andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("BOOK_NOT_FOUND") }
        }
        mockMvc.get("/api/v1/books?titulo=antigo").andExpect {
            jsonPath("$.totalElements") { value(0) }
        }
    }

    @Test
    fun `ordenacao por campo inexistente retorna 400`() {
        mockMvc.get("/api/v1/books?sort=campoQueNaoExiste").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_SORT") }
        }
    }

    @Test
    fun `atualizar para isbn de outro livro retorna 409`() {
        val genero = dados.genero()
        dados.livro(isbn = "111", generos = setOf(genero))
        val livro = dados.livro(isbn = "222", generos = setOf(genero))

        putJson("/api/v1/books/${livro.id}", """{ "titulo": "X", "autor": "Y", "isbn": "111", "generoIds": [${genero.id}] }""")
            .andExpect {
                status { isConflict() }
                jsonPath("$.code") { value("ISBN_ALREADY_EXISTS") }
            }
    }

    @Test
    fun `atualizar mantendo o proprio isbn e permitido`() {
        val genero = dados.genero()
        val livro = dados.livro(isbn = "333", generos = setOf(genero))

        putJson("/api/v1/books/${livro.id}", """{ "titulo": "Novo", "autor": "Y", "isbn": "333", "generoIds": [${genero.id}] }""")
            .andExpect { status { isOk() } }
    }
}

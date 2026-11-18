package com.biblioteca.gerenciador.livro

import com.biblioteca.gerenciador.config.ComponentTestBase
import com.biblioteca.gerenciador.domain.Livro
import com.biblioteca.gerenciador.enums.StatusExemplar
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get

/** Disponibilidade calculada a partir dos exemplares (RF03, RF04, RF05, RN13). */
class LivroDisponibilidadeComponentTest : ComponentTestBase() {

    private lateinit var comDisponivel: Livro      // 2 disponíveis, 1 emprestado, 1 baixado
    private lateinit var todosEmprestados: Livro   // 1 emprestado
    private lateinit var semExemplares: Livro

    @BeforeEach
    fun cenario() {
        comDisponivel = dados.livro(titulo = "A - Com disponível")
        dados.exemplar(comDisponivel)
        dados.exemplar(comDisponivel)
        dados.exemplar(comDisponivel, status = StatusExemplar.EMPRESTADO)
        dados.exemplar(comDisponivel, status = StatusExemplar.BAIXADO)

        todosEmprestados = dados.livro(titulo = "B - Todos emprestados")
        dados.exemplar(todosEmprestados, status = StatusExemplar.EMPRESTADO)

        semExemplares = dados.livro(titulo = "C - Sem exemplares")
    }

    @Test
    fun `response traz as quantidades calculadas`() {
        mockMvc.get("/api/v1/books/${comDisponivel.id}").andExpect {
            status { isOk() }
            jsonPath("$.totalExemplares") { value(3) } // o baixado não conta
            jsonPath("$.exemplaresDisponiveis") { value(2) }
            jsonPath("$.exemplaresEmprestados") { value(1) }
        }
        mockMvc.get("/api/v1/books/${semExemplares.id}").andExpect {
            jsonPath("$.totalExemplares") { value(0) }
            jsonPath("$.exemplaresDisponiveis") { value(0) }
        }
    }

    @Test
    fun `filtra livros com exemplares disponiveis`() {
        mockMvc.get("/api/v1/books?disponibilidade=DISPONIVEL").andExpect {
            jsonPath("$.content[*].titulo") { value(contains("A - Com disponível")) }
        }
    }

    @Test
    fun `filtra livros sem exemplares disponiveis`() {
        mockMvc.get("/api/v1/books?disponibilidade=INDISPONIVEL").andExpect {
            jsonPath("$.content[*].titulo") { value(contains("B - Todos emprestados", "C - Sem exemplares")) }
        }
    }

    @Test
    fun `filtra livros com exemplares emprestados`() {
        mockMvc.get("/api/v1/books?disponibilidade=EMPRESTADO").andExpect {
            jsonPath("$.content[*].titulo") { value(containsInAnyOrder("A - Com disponível", "B - Todos emprestados")) }
        }
    }

    @Test
    fun `filtro de disponibilidade invalido retorna 400`() {
        mockMvc.get("/api/v1/books?disponibilidade=TALVEZ").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_PARAMETER") }
        }
    }

    @Test
    fun `nao inativa livro com exemplar emprestado`() {
        mockMvc.delete("/api/v1/books/${todosEmprestados.id}").andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("BOOK_HAS_ACTIVE_LOANS") }
        }
    }

    @Test
    fun `catalogo publico mostra so dados publicos`() {
        mockMvc.get("/api/v1/catalog/books?titulo=com dispon").andExpect {
            status { isOk() }
            jsonPath("$.content[0].titulo") { value("A - Com disponível") }
            jsonPath("$.content[0].exemplaresDisponiveis") { value(2) }
            jsonPath("$.content[0].disponivel") { value(true) }
            jsonPath("$.content[0].dataCadastro") { doesNotExist() }
            jsonPath("$.content[0].totalExemplares") { doesNotExist() }
        }
    }

    @Test
    fun `catalogo pode mostrar apenas disponiveis`() {
        mockMvc.get("/api/v1/catalog/books?apenasDisponiveis=true").andExpect {
            jsonPath("$.totalElements") { value(1) }
            jsonPath("$.content[0].titulo") { value("A - Com disponível") }
        }
    }
}

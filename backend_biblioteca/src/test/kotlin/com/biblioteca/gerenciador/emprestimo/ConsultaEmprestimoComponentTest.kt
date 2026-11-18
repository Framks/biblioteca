package com.biblioteca.gerenciador.emprestimo

import com.biblioteca.gerenciador.config.ClockFixoConfig.Companion.AGORA
import com.biblioteca.gerenciador.config.ClockFixoConfig.Companion.HOJE
import com.biblioteca.gerenciador.config.ComponentTestBase
import com.biblioteca.gerenciador.config.DadosTeste.Companion.CPFS_VALIDOS
import com.biblioteca.gerenciador.domain.Emprestimo
import com.biblioteca.gerenciador.domain.Livro
import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.enums.CondicaoExemplar
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get

/** Consulta de empréstimos (RF07, RF12) e situação calculada (RN06). */
class ConsultaEmprestimoComponentTest : ComponentTestBase() {

    private lateinit var ana: Usuario
    private lateinit var bruno: Usuario
    private lateinit var duna: Livro
    private lateinit var ativo: Emprestimo      // Ana, Duna, no prazo
    private lateinit var atrasado: Emprestimo   // Bruno, 5 dias atrasado
    private lateinit var devolvido: Emprestimo  // Ana, já devolvido

    @BeforeEach
    fun cenario() {
        ana = dados.leitor(nome = "Ana", cpf = CPFS_VALIDOS[0])
        bruno = dados.leitor(nome = "Bruno", cpf = CPFS_VALIDOS[1])
        duna = dados.livro(titulo = "Duna")

        ativo = dados.emprestimo(ana, dados.exemplar(duna))
        atrasado = dados.emprestimo(bruno, dados.exemplar(dados.livro(titulo = "Iracema")), dataPrevista = HOJE.minusDays(5))
        devolvido = dados.emprestimo(ana, dados.exemplar(dados.livro(titulo = "Senhora")))
        devolvido.devolver(devolvido.itens[0].id!!, CondicaoExemplar.CONSERVADO, dados.admin(), HOJE, AGORA)
    }

    @Test
    fun `situacao calculada no detalhe`() {
        mockMvc.get("/api/v1/loans/${atrasado.id}").andExpect {
            status { isOk() }
            jsonPath("$.situacao") { value("ATRASADO") }
            jsonPath("$.itens[0].situacao") { value("ATRASADO") }
            jsonPath("$.itens[0].diasAtraso") { value(5) }
        }
    }

    @Test
    fun `filtra por situacao`() {
        mockMvc.get("/api/v1/loans?situacao=ATIVO").andExpect {
            jsonPath("$.content[*].id") { value(containsInAnyOrder(ativo.id!!.toInt())) }
        }
        mockMvc.get("/api/v1/loans?situacao=ATRASADO").andExpect {
            jsonPath("$.content[*].id") { value(containsInAnyOrder(atrasado.id!!.toInt())) }
        }
        mockMvc.get("/api/v1/loans?situacao=DEVOLVIDO").andExpect {
            jsonPath("$.content[*].id") { value(containsInAnyOrder(devolvido.id!!.toInt())) }
        }
        mockMvc.get("/api/v1/loans").andExpect { jsonPath("$.totalElements") { value(3) } }
    }

    @Test
    fun `filtra por leitor e por livro`() {
        mockMvc.get("/api/v1/loans?leitorId=${ana.id}").andExpect {
            jsonPath("$.content[*].id") { value(containsInAnyOrder(ativo.id!!.toInt(), devolvido.id!!.toInt())) }
        }
        mockMvc.get("/api/v1/loans?livroId=${duna.id}").andExpect {
            jsonPath("$.content[*].id") { value(containsInAnyOrder(ativo.id!!.toInt())) }
        }
    }

    @Test
    fun `historico do leitor`() {
        mockMvc.get("/api/v1/readers/${ana.id}/loans").andExpect {
            status { isOk() }
            jsonPath("$.totalElements") { value(2) }
        }
        mockMvc.get("/api/v1/readers/${ana.id}/loans?situacao=DEVOLVIDO").andExpect {
            jsonPath("$.content[*].id") { value(containsInAnyOrder(devolvido.id!!.toInt())) }
        }
        mockMvc.get("/api/v1/readers/9999/loans").andExpect { status { isNotFound() } }
    }

    @Test
    fun `situacao invalida retorna 400`() {
        mockMvc.get("/api/v1/loans?situacao=PERDIDO").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_PARAMETER") }
        }
    }

    @Test
    fun `emprestimo inexistente retorna 404`() {
        mockMvc.get("/api/v1/loans/9999").andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("LOAN_NOT_FOUND") }
        }
    }
}

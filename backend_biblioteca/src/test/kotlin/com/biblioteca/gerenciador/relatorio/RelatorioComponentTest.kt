package com.biblioteca.gerenciador.relatorio

import com.biblioteca.gerenciador.config.ClockFixoConfig.Companion.HOJE
import com.biblioteca.gerenciador.config.ComponentTestBase
import com.biblioteca.gerenciador.config.DadosTeste.Companion.CPFS_VALIDOS
import com.biblioteca.gerenciador.domain.Livro
import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.enums.CondicaoExemplar
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get
import java.time.Instant
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

/** Relatórios de circulação (RF13) e exportação CSV (RF14). Hoje = 13/11/2026. */
class RelatorioComponentTest : ComponentTestBase() {

    private lateinit var ana: Usuario
    private lateinit var bruno: Usuario
    private lateinit var duna: Livro
    private lateinit var iracema: Livro

    /**
     * - Ana: Duna em setembro (devolvido) e Duna de novo em novembro (no prazo)
     * - Bruno: Iracema e Senhora em novembro, ambos 5 dias atrasados
     * - Carla: suspensa
     */
    @BeforeEach
    fun cenario() {
        ana = dados.leitor(nome = "Ana", cpf = CPFS_VALIDOS[0])
        bruno = dados.leitor(nome = "Bruno", cpf = CPFS_VALIDOS[1])
        dados.suspender(dados.leitor(nome = "Carla", cpf = CPFS_VALIDOS[2]), dias = 3)
        duna = dados.livro(titulo = "Duna", autor = "Frank Herbert")
        iracema = dados.livro(titulo = "Iracema", autor = "José de Alencar")

        val antigo = dados.emprestimo(ana, dados.exemplar(duna), dataEmprestimo = Instant.parse("2026-09-15T15:00:00Z"))
        antigo.devolver(antigo.itens[0].id!!, CondicaoExemplar.CONSERVADO, dados.admin(), HOJE, Instant.parse("2026-09-20T15:00:00Z"))

        dados.emprestimo(ana, dados.exemplar(duna))
        dados.emprestimo(
            bruno,
            dados.exemplar(iracema),
            dados.exemplar(dados.livro(titulo = "Senhora", autor = "José de Alencar")),
            dataPrevista = HOJE.minusDays(5),
        )
    }

    @Test
    fun `resumo da circulacao`() {
        mockMvc.get("/api/v1/reports/summary").andExpect {
            status { isOk() }
            jsonPath("$.dataReferencia") { value(HOJE.toString()) }
            jsonPath("$.emprestimosAtivos") { value(2) }
            jsonPath("$.emprestimosAtrasados") { value(1) }
            jsonPath("$.emprestimosNoPrazo") { value(1) }
            jsonPath("$.exemplaresEmprestados") { value(3) }
            jsonPath("$.exemplaresAtrasados") { value(2) }
            jsonPath("$.leitoresComAtraso") { value(1) }
            jsonPath("$.leitoresSuspensos") { value(1) }
            jsonPath("$.emprestimosNoMes") { value(2) }
        }
    }

    @Test
    fun `emprestimos por mes inclui meses sem movimento`() {
        mockMvc.get("/api/v1/reports/loans-per-month?inicio=2026-09&fim=2026-11").andExpect {
            status { isOk() }
            jsonPath("$[*].mes") { value(contains("2026-09", "2026-10", "2026-11")) }
            jsonPath("$[*].quantidade") { value(contains(1, 0, 2)) }
        }
    }

    @Test
    fun `sem periodo mostra os ultimos 12 meses`() {
        mockMvc.get("/api/v1/reports/loans-per-month").andExpect {
            jsonPath("$.length()") { value(12) }
            jsonPath("$[0].mes") { value("2025-12") }
            jsonPath("$[11].mes") { value("2026-11") }
        }
    }

    @Test
    fun `periodo invalido retorna 400`() {
        mockMvc.get("/api/v1/reports/loans-per-month?inicio=2026-11&fim=2026-01").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_PERIOD") }
        }
        mockMvc.get("/api/v1/reports/loans-per-month?inicio=nov").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_PARAMETER") }
        }
    }

    @Test
    fun `livros mais emprestados`() {
        mockMvc.get("/api/v1/reports/top-books").andExpect {
            status { isOk() }
            jsonPath("$[0].titulo") { value("Duna") }
            jsonPath("$[0].emprestimos") { value(2) }
            jsonPath("$[0].posicao") { value(1) }
            jsonPath("$.length()") { value(3) }
        }
        mockMvc.get("/api/v1/reports/top-books?de=2026-11-01&limite=1").andExpect {
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].emprestimos") { value(1) }
        }
    }

    @Test
    fun `limite fora da faixa retorna 400`() {
        mockMvc.get("/api/v1/reports/top-books?limite=0").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_ERROR") }
            jsonPath("$.fields[0].field") { value("limite") }
        }
    }

    @Test
    fun `leitores em atraso`() {
        mockMvc.get("/api/v1/reports/overdue-readers").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].nome") { value("Bruno") }
            jsonPath("$[0].exemplaresAtrasados") { value(2) }
            jsonPath("$[0].maiorAtrasoDias") { value(5) }
            jsonPath("$[0].vencidoDesde") { value(HOJE.minusDays(5).toString()) }
            jsonPath("$[0].titulos") { value(contains("Iracema", "Senhora")) }
        }
    }

    @Test
    fun `exporta CSV que o Excel abre com acentos`() {
        val resposta = mockMvc.get("/api/v1/reports/loans-per-month/export?inicio=2026-09&fim=2026-11").andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith("text/csv") }
            header { string("Content-Disposition", containsString("attachment; filename=\"emprestimos-por-mes.csv\"")) }
        }.andReturn().response

        val bytes = resposta.contentAsByteArray
        assertContentEquals(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()), bytes.copyOfRange(0, 3)) // BOM UTF-8
        val texto = String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        assertTrue(texto.startsWith("mês;empréstimos\r\n"), texto)
        assertTrue("2026-11;2\r\n" in texto, texto)
    }

    @Test
    fun `CSV escapa campos com ponto e virgula e aspas`() {
        dados.emprestimo(ana, dados.exemplar(dados.livro(titulo = "Livro; com \"aspas\"")))

        val texto = mockMvc.get("/api/v1/reports/top-books/export").andReturn().response.contentAsString
        assertTrue("\"Livro; com \"\"aspas\"\"\"" in texto, texto)
    }

    @Test
    fun `demais exportacoes`() {
        mockMvc.get("/api/v1/reports/summary/export").andExpect {
            status { isOk() }
            content { string(containsString("empréstimos atrasados;1")) }
        }
        mockMvc.get("/api/v1/reports/overdue-readers/export").andExpect {
            status { isOk() }
            content { string(containsString("Bruno;;;2;5;${HOJE.minusDays(5)};Iracema | Senhora")) }
        }
    }
}

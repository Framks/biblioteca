package com.biblioteca.gerenciador.emprestimo

import com.biblioteca.gerenciador.config.ClockFixoConfig.Companion.AGORA
import com.biblioteca.gerenciador.config.ClockFixoConfig.Companion.HOJE
import com.biblioteca.gerenciador.config.ComponentTestBase
import com.biblioteca.gerenciador.domain.Emprestimo
import com.biblioteca.gerenciador.domain.Usuario
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.LocalDate

/** Devolução (RF10, RN06–RN09) e renovação (RF11, RN10, RN11). */
class DevolucaoRenovacaoComponentTest : ComponentTestBase() {

    private lateinit var leitor: Usuario

    @BeforeEach
    fun cenario() {
        leitor = dados.leitor(nome = "Ana")
    }

    private fun emprestimoDe(vararg titulos: String, dataPrevista: LocalDate = HOJE.plusDays(14)): Emprestimo =
        dados.emprestimo(leitor, *titulos.map { dados.exemplar(dados.livro(titulo = it)) }.toTypedArray(), dataPrevista = dataPrevista)

    private fun devolver(emprestimo: Emprestimo, vararg itens: Pair<Int, String>) = postJson(
        "/api/v1/loans/${emprestimo.id}/return",
        """{ "itens": [${itens.joinToString { (i, c) -> """{ "itemId": ${emprestimo.itens[i].id}, "condicao": "$c" }""" }}] }""",
    )

    // ---------- devolução ----------

    @Test
    fun `devolucao no prazo encerra o emprestimo e libera o exemplar`() {
        val emprestimo = emprestimoDe("Dom Casmurro")

        devolver(emprestimo, 0 to "CONSERVADO").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("ENCERRADO") }
            jsonPath("$.situacao") { value("DEVOLVIDO") }
            jsonPath("$.itens[0].dataDevolucaoReal") { value(AGORA.toString()) }
            jsonPath("$.itens[0].condicaoDevolucao") { value("CONSERVADO") }
            jsonPath("$.itens[0].renovacoesRestantes") { value(0) }
        }
        mockMvc.get("/api/v1/copies/${emprestimo.itens[0].exemplar.id}").andExpect {
            jsonPath("$.status") { value("DISPONIVEL") }
        }
        mockMvc.get("/api/v1/readers/${leitor.id}").andExpect { jsonPath("$.suspenso") { value(false) } }
    }

    @Test
    fun `RN09 - devolucao parcial mantem o emprestimo aberto`() {
        val emprestimo = emprestimoDe("Livro A", "Livro B")

        devolver(emprestimo, 0 to "CONSERVADO").andExpect {
            jsonPath("$.status") { value("ABERTO") }
            jsonPath("$.itens[0].situacao") { value("DEVOLVIDO") }
            jsonPath("$.itens[1].situacao") { value("ATIVO") }
        }
        devolver(emprestimo, 1 to "CONSERVADO").andExpect {
            jsonPath("$.status") { value("ENCERRADO") }
        }
    }

    @Test
    fun `RN07 - devolucao com atraso suspende o leitor um dia por dia de atraso`() {
        val emprestimo = emprestimoDe("Atrasado", dataPrevista = HOJE.minusDays(3))

        devolver(emprestimo, 0 to "CONSERVADO").andExpect { status { isOk() } }

        mockMvc.get("/api/v1/readers/${leitor.id}").andExpect {
            jsonPath("$.suspenso") { value(true) }
            jsonPath("$.suspensoAte") { value(HOJE.plusDays(2).toString()) } // 3 dias: hoje, amanhã e depois
        }
    }

    @Test
    fun `RN07 - fator de suspensao vem da configuracao`() {
        putJson("/api/v1/settings/suspensao.dias_por_dia_atraso", """{ "valor": 2 }""").andExpect { status { isOk() } }
        val emprestimo = emprestimoDe("Atrasado", dataPrevista = HOJE.minusDays(3))

        devolver(emprestimo, 0 to "CONSERVADO")

        mockMvc.get("/api/v1/readers/${leitor.id}").andExpect {
            jsonPath("$.suspensoAte") { value(HOJE.plusDays(5).toString()) } // 3 dias x 2 = 6 dias
        }
    }

    @Test
    fun `RN08 - exemplar devolvido danificado vai para manutencao`() {
        val emprestimo = emprestimoDe("Rasgado")

        devolver(emprestimo, 0 to "DANIFICADO").andExpect { status { isOk() } }

        mockMvc.get("/api/v1/copies/${emprestimo.itens[0].exemplar.id}").andExpect {
            jsonPath("$.status") { value("MANUTENCAO") }
            jsonPath("$.condicao") { value("DANIFICADO") }
        }
    }

    @Test
    fun `item ja devolvido nao pode ser devolvido de novo`() {
        val emprestimo = emprestimoDe("Livro")
        devolver(emprestimo, 0 to "CONSERVADO")

        devolver(emprestimo, 0 to "CONSERVADO").andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("ITEM_ALREADY_RETURNED") }
        }
    }

    @Test
    fun `item de outro emprestimo retorna 404`() {
        val emprestimo = emprestimoDe("Livro A")
        val outro = emprestimoDe("Livro B")

        postJson(
            "/api/v1/loans/${emprestimo.id}/return",
            """{ "itens": [{ "itemId": ${outro.itens[0].id}, "condicao": "CONSERVADO" }] }""",
        ).andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("LOAN_ITEM_NOT_FOUND") }
        }
    }

    @Test
    fun `devolucao exige condicao de cada item`() {
        val emprestimo = emprestimoDe("Livro")

        postJson("/api/v1/loans/${emprestimo.id}/return", """{ "itens": [{ "itemId": ${emprestimo.itens[0].id} }] }""")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.fields[0].field") { value("itens[0].condicao") }
            }
    }

    @Test
    fun `RN13 - leitor com exemplar nao devolvido nao pode ser inativado`() {
        emprestimoDe("Livro")

        mockMvc.delete("/api/v1/readers/${leitor.id}").andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("READER_HAS_ACTIVE_LOANS") }
        }
    }

    // ---------- renovação ----------

    @Test
    fun `RN10 - renova por mais um prazo padrao a partir de hoje`() {
        val emprestimo = emprestimoDe("Livro", dataPrevista = HOJE.plusDays(2))

        mockMvc.post("/api/v1/loans/items/${emprestimo.itens[0].id}/renew").andExpect {
            status { isOk() }
            jsonPath("$.itens[0].dataDevolucaoPrevista") { value(HOJE.plusDays(14).toString()) }
            jsonPath("$.itens[0].renovacoes") { value(1) }
            jsonPath("$.itens[0].renovacoesRestantes") { value(1) }
        }
    }

    @Test
    fun `RN10 - renovacao nunca encurta o prazo atual`() {
        val emprestimo = emprestimoDe("Livro", dataPrevista = HOJE.plusDays(30))

        mockMvc.post("/api/v1/loans/items/${emprestimo.itens[0].id}/renew").andExpect {
            jsonPath("$.itens[0].dataDevolucaoPrevista") { value(HOJE.plusDays(30).toString()) }
        }
    }

    @Test
    fun `RN10 - limite de renovacoes`() {
        val item = emprestimoDe("Livro").itens[0]
        repeat(2) { mockMvc.post("/api/v1/loans/items/${item.id}/renew").andExpect { status { isOk() } } }

        mockMvc.post("/api/v1/loans/items/${item.id}/renew").andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("RENEWAL_LIMIT_REACHED") }
        }
    }

    @Test
    fun `RN11 - item atrasado nao pode ser renovado`() {
        val item = emprestimoDe("Livro", dataPrevista = HOJE.minusDays(1)).itens[0]

        mockMvc.post("/api/v1/loans/items/${item.id}/renew").andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("OVERDUE_ITEM_CANNOT_BE_RENEWED") }
        }
    }

    @Test
    fun `RN05 - leitor com outro item atrasado nao pode renovar`() {
        emprestimoDe("Atrasado", dataPrevista = HOJE.minusDays(1))
        val emDia = emprestimoDe("Em dia").itens[0]

        mockMvc.post("/api/v1/loans/items/${emDia.id}/renew").andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("READER_HAS_OVERDUE_LOANS") }
        }
    }

    @Test
    fun `RN05 - leitor suspenso nao pode renovar`() {
        val item = emprestimoDe("Livro").itens[0]
        dados.suspender(leitor, dias = 2)

        mockMvc.post("/api/v1/loans/items/${item.id}/renew").andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("READER_SUSPENDED") }
        }
    }

    @Test
    fun `item inexistente retorna 404`() {
        mockMvc.post("/api/v1/loans/items/9999/renew").andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("LOAN_ITEM_NOT_FOUND") }
        }
    }
}

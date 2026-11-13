package com.biblioteca.gerenciador.configuracao

import com.biblioteca.gerenciador.config.ComponentTestBase
import org.hamcrest.Matchers.hasItems
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get

/** Parâmetros de negócio (RF19). */
class ConfiguracaoComponentTest : ComponentTestBase() {

    @Test
    fun `lista as configuracoes com os valores padrao`() {
        mockMvc.get("/api/v1/settings").andExpect {
            status { isOk() }
            jsonPath("$[*].chave") {
                value(hasItems("emprestimo.max_itens_por_leitor", "emprestimo.prazo_padrao_dias", "emprestimo.max_renovacoes"))
            }
            jsonPath("$[?(@.chave == 'emprestimo.max_itens_por_leitor')].valor") { value(hasItems(3)) }
        }
    }

    @Test
    fun `alteracao passa a valer nas regras`() {
        putJson("/api/v1/settings/emprestimo.max_itens_por_leitor", """{ "valor": 1 }""").andExpect {
            status { isOk() }
            jsonPath("$.valor") { value(1) }
        }
        val leitor = dados.leitor()
        val e1 = dados.exemplar(dados.livro(titulo = "A"))
        val e2 = dados.exemplar(dados.livro(titulo = "B"))

        postJson("/api/v1/loans", """{ "leitorId": ${leitor.id}, "exemplarIds": [${e1.id}, ${e2.id}] }""").andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("LOAN_LIMIT_EXCEEDED") }
        }
    }

    @Test
    fun `valor deve ser positivo`() {
        putJson("/api/v1/settings/emprestimo.max_renovacoes", """{ "valor": 0 }""").andExpect {
            status { isBadRequest() }
            jsonPath("$.fields[0].field") { value("valor") }
        }
    }

    @Test
    fun `chave inexistente retorna 404`() {
        putJson("/api/v1/settings/nao.existe", """{ "valor": 1 }""").andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("SETTING_NOT_FOUND") }
        }
    }
}

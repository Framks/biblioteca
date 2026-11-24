package com.biblioteca.gerenciador.emprestimo

import com.biblioteca.gerenciador.config.ClockFixoConfig.Companion.AGORA
import com.biblioteca.gerenciador.config.ClockFixoConfig.Companion.HOJE
import com.biblioteca.gerenciador.config.ComponentTestBase
import com.biblioteca.gerenciador.config.DadosTeste.Companion.CPFS_VALIDOS
import com.biblioteca.gerenciador.domain.Exemplar
import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.enums.StatusExemplar
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get

/** Registro de empréstimo (RF09) e regras RN01–RN05. */
class RegistroEmprestimoComponentTest : ComponentTestBase() {

    private lateinit var leitor: Usuario

    @BeforeEach
    fun cenario() {
        leitor = dados.leitor(nome = "Ana", cpf = CPFS_VALIDOS[0])
    }

    private fun exemplarDisponivel(titulo: String = "Livro ${System.nanoTime()}"): Exemplar =
        dados.exemplar(dados.livro(titulo = titulo))

    private fun emprestar(vararg exemplares: Exemplar, extra: String = "") =
        postJson(
            "/api/v1/loans",
            """{ "leitorId": ${leitor.id}, "exemplarIds": [${exemplares.joinToString { it.id.toString() }}] $extra }""",
        )

    @Test
    fun `registra emprestimo com prazo padrao`() {
        val e1 = exemplarDisponivel("Dom Casmurro")
        val e2 = exemplarDisponivel("Iracema")

        emprestar(e1, e2, extra = """, "observacao": "  Leitor pediu sacola  " """).andExpect {
            status { isCreated() }
            header { exists("Location") }
            jsonPath("$.leitor.nome") { value("Ana") }
            jsonPath("$.operador.nome") { value("Administrador") }
            jsonPath("$.dataEmprestimo") { value(AGORA.toString()) }
            jsonPath("$.status") { value("ABERTO") }
            jsonPath("$.situacao") { value("ATIVO") }
            jsonPath("$.observacao") { value("Leitor pediu sacola") }
            jsonPath("$.itens.length()") { value(2) }
            jsonPath("$.itens[0].tituloLivro") { value("Dom Casmurro") }
            jsonPath("$.itens[0].dataDevolucaoPrevista") { value(HOJE.plusDays(14).toString()) }
            jsonPath("$.itens[0].situacao") { value("ATIVO") }
            jsonPath("$.itens[0].renovacoesRestantes") { value(2) }
            jsonPath("$.itens[0].condicaoRetirada") { value("CONSERVADO") }
        }

        mockMvc.get("/api/v1/copies/${e1.id}").andExpect { jsonPath("$.status") { value("EMPRESTADO") } }
    }

    @Test
    fun `aceita prazo personalizado ate o maximo`() {
        emprestar(exemplarDisponivel(), extra = """, "prazoDias": 30""").andExpect {
            status { isCreated() }
            jsonPath("$.itens[0].dataDevolucaoPrevista") { value(HOJE.plusDays(30).toString()) }
        }
    }

    @Test
    fun `RN02 - prazo acima do maximo retorna 422`() {
        emprestar(exemplarDisponivel(), extra = """, "prazoDias": 31""").andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("LOAN_TERM_EXCEEDED") }
        }
    }

    @Test
    fun `RN01 - respeita o limite de exemplares por leitor`() {
        dados.emprestimo(leitor, exemplarDisponivel(), exemplarDisponivel())

        emprestar(exemplarDisponivel(), exemplarDisponivel()).andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("LOAN_LIMIT_EXCEEDED") }
        }
        emprestar(exemplarDisponivel()).andExpect { status { isCreated() } } // 2 + 1 = 3, no limite
    }

    @Test
    fun `RN03 - exemplar em manutencao nao pode ser emprestado`() {
        val emManutencao = dados.exemplar(dados.livro(), status = StatusExemplar.MANUTENCAO)

        emprestar(emManutencao).andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("COPY_NOT_AVAILABLE") }
        }
    }

    @Test
    fun `RN03 - exemplar ja emprestado nao pode ser emprestado de novo`() {
        val exemplar = exemplarDisponivel()
        dados.emprestimo(dados.leitor(cpf = CPFS_VALIDOS[1]), exemplar)

        emprestar(exemplar).andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("COPY_NOT_AVAILABLE") }
        }
    }

    @Test
    fun `RN04 - nao empresta dois exemplares do mesmo livro na mesma requisicao`() {
        val livro = dados.livro(titulo = "Duna")

        emprestar(dados.exemplar(livro), dados.exemplar(livro)).andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("SAME_BOOK_TWICE") }
        }
    }

    @Test
    fun `RN04 - nao empresta livro que o leitor ja tem em maos`() {
        val livro = dados.livro(titulo = "Duna")
        dados.emprestimo(leitor, dados.exemplar(livro))

        emprestar(dados.exemplar(livro)).andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("SAME_BOOK_TWICE") }
        }
    }

    @Test
    fun `RN05 - leitor com item atrasado nao pode pegar novo emprestimo`() {
        dados.emprestimo(leitor, exemplarDisponivel(), dataPrevista = HOJE.minusDays(1))

        emprestar(exemplarDisponivel()).andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("READER_HAS_OVERDUE_LOANS") }
        }
    }

    @Test
    fun `item que vence hoje ainda nao esta atrasado`() {
        dados.emprestimo(leitor, exemplarDisponivel(), dataPrevista = HOJE)

        emprestar(exemplarDisponivel()).andExpect { status { isCreated() } }
    }

    @Test
    fun `RN05 - leitor suspenso nao pode pegar emprestimo`() {
        dados.suspender(leitor, dias = 3)

        emprestar(exemplarDisponivel()).andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("READER_SUSPENDED") }
        }
    }

    /**
     * Confere todos os exemplares antes de emprestar qualquer um.
     * (O rollback em si não dá para testar aqui: o teste e a requisição compartilham a mesma transação.)
     */
    @Test
    fun `um exemplar indisponivel impede o emprestimo dos demais`() {
        val disponivel = exemplarDisponivel()
        val emManutencao = dados.exemplar(dados.livro(), status = StatusExemplar.MANUTENCAO)

        emprestar(disponivel, emManutencao).andExpect { status { isUnprocessableContent() } }

        mockMvc.get("/api/v1/copies/${disponivel.id}").andExpect { jsonPath("$.status") { value("DISPONIVEL") } }
    }

    @Test
    fun `leitor ou exemplar inexistente retorna 404`() {
        postJson("/api/v1/loans", """{ "leitorId": 9999, "exemplarIds": [${exemplarDisponivel().id}] }""").andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("READER_NOT_FOUND") }
        }
        postJson("/api/v1/loans", """{ "leitorId": ${leitor.id}, "exemplarIds": [9999] }""").andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("COPY_NOT_FOUND") }
        }
    }

    @Test
    fun `valida campos obrigatorios`() {
        postJson("/api/v1/loans", """{ "prazoDias": 0 }""").andExpect {
            status { isBadRequest() }
            jsonPath("$.fields[*].field") {
                value(org.hamcrest.Matchers.containsInAnyOrder("leitorId", "exemplarIds", "prazoDias"))
            }
        }
    }
}

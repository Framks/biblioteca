package com.biblioteca.gerenciador.seguranca

import com.biblioteca.gerenciador.config.ComponentTestBase
import com.biblioteca.gerenciador.config.DadosTeste.Companion.CPFS_VALIDOS
import com.biblioteca.gerenciador.domain.Perfil
import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.repositories.PerfilRepository
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

/** Autorização por permissão (seção 7). */
class PermissoesComponentTest : ComponentTestBase() {

    @Autowired
    private lateinit var perfilRepository: PerfilRepository

    private lateinit var leitora: Usuario
    private lateinit var outroLeitor: Usuario
    private lateinit var funcionario: Usuario

    @BeforeEach
    fun cenario() {
        leitora = dados.usuarioComAcesso(Perfil.LEITOR, "ana@email.com", nome = "Ana", cpf = CPFS_VALIDOS[0])
        outroLeitor = dados.leitor(nome = "Bruno", cpf = CPFS_VALIDOS[1])
        funcionario = dados.usuarioComAcesso(Perfil.FUNCIONARIO, "func@biblioteca.local", nome = "Funcionário")
    }

    private val novoLivro get() = """{ "titulo": "Novo", "autor": "X", "generoIds": [${dados.genero("G ${System.nanoTime()}").id}] }"""

    // ---------- leitor ----------

    @Test
    fun `leitor consulta o acervo mas nao altera nada`() {
        mockMvc.get("/api/v1/books") { with(comoUsuario(leitora)) }.andExpect { status { isOk() } }

        postJson("/api/v1/books", novoLivro, comoUsuario(leitora)).andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("FORBIDDEN") }
        }
        mockMvc.get("/api/v1/readers") { with(comoUsuario(leitora)) }.andExpect { status { isForbidden() } }
        mockMvc.get("/api/v1/loans") { with(comoUsuario(leitora)) }.andExpect { status { isForbidden() } }
        mockMvc.get("/api/v1/reports/summary") { with(comoUsuario(leitora)) }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `leitor ve apenas os proprios emprestimos`() {
        dados.emprestimo(leitora, dados.exemplar(dados.livro(titulo = "Da Ana")))
        dados.emprestimo(outroLeitor, dados.exemplar(dados.livro(titulo = "Do Bruno")))

        mockMvc.get("/api/v1/loans/me") { with(comoUsuario(leitora)) }.andExpect {
            status { isOk() }
            jsonPath("$.content[*].itens[0].tituloLivro") { value(contains("Da Ana")) }
        }
    }

    @Test
    fun `leitor renova o proprio item mas nao o de outro`() {
        val meu = dados.emprestimo(leitora, dados.exemplar(dados.livro()))
        val alheio = dados.emprestimo(outroLeitor, dados.exemplar(dados.livro()))

        mockMvc.post("/api/v1/loans/items/${meu.itens[0].id}/renew") { with(comoUsuario(leitora)) }.andExpect {
            status { isOk() }
        }
        mockMvc.post("/api/v1/loans/items/${alheio.itens[0].id}/renew") { with(comoUsuario(leitora)) }.andExpect {
            status { isForbidden() }
        }
    }

    // ---------- funcionário ----------

    @Test
    fun `funcionario opera o acervo e a circulacao mas nao administra`() {
        postJson("/api/v1/books", novoLivro, comoUsuario(funcionario)).andExpect { status { isCreated() } }
        mockMvc.get("/api/v1/reports/summary") { with(comoUsuario(funcionario)) }.andExpect { status { isOk() } }

        mockMvc.get("/api/v1/users") { with(comoUsuario(funcionario)) }.andExpect { status { isForbidden() } }
        mockMvc.get("/api/v1/roles") { with(comoUsuario(funcionario)) }.andExpect { status { isForbidden() } }
        mockMvc.get("/api/v1/audit") { with(comoUsuario(funcionario)) }.andExpect { status { isForbidden() } }
        putJson("/api/v1/settings/emprestimo.max_renovacoes", """{ "valor": 5 }""", comoUsuario(funcionario))
            .andExpect { status { isForbidden() } }
        mockMvc.get("/api/v1/settings") { with(comoUsuario(funcionario)) }.andExpect { status { isOk() } }
    }

    @Test
    fun `funcionario sem renovacao propria nao acessa loans me`() {
        mockMvc.get("/api/v1/loans/me") { with(comoUsuario(funcionario)) }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `permissao retirada do perfil vale na proxima requisicao`() {
        val perfil = perfilRepository.findByNome(Perfil.FUNCIONARIO)!!
        val semCadastroDeLivro = perfil.permissoes.map { it.codigo }.filter { it != "book_create" }
        putJson(
            "/api/v1/roles/${perfil.id}/permissions",
            """{ "permissoes": [${semCadastroDeLivro.joinToString { "\"$it\"" }}] }""",
        ).andExpect { status { isOk() } }

        postJson("/api/v1/books", novoLivro, comoUsuario(funcionario)).andExpect { status { isForbidden() } }
    }
}

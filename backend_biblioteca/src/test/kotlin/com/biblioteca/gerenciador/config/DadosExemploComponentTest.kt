package com.biblioteca.gerenciador.config

import com.biblioteca.gerenciador.config.ClockFixoConfig.Companion.HOJE
import com.biblioteca.gerenciador.repositories.UsuarioRepository
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.get
import kotlin.test.assertFalse

/**
 * Dados de exemplo do ambiente local (DadosExemplo). Banco H2 próprio: os registros criados
 * na inicialização não podem aparecer nos outros testes.
 */
@TestPropertySource(
    properties = [
        "app.dados-exemplo=true",
        "spring.datasource.url=jdbc:h2:mem:dados_exemplo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
    ],
)
class DadosExemploComponentTest : ComponentTestBase() {

    @Autowired
    private lateinit var dadosExemplo: DadosExemplo

    @Autowired
    private lateinit var usuarioRepository: UsuarioRepository

    private fun login(email: String, senha: String) =
        postJson("/api/v1/auth/login", """{ "email": "$email", "senha": "$senha" }""", anonimo())

    @Test
    fun `uma conta de cada tipo consegue fazer login`() {
        login("admin@biblioteca.local", "admin-teste-123").andExpect { jsonPath("$.usuario.perfil") { value("ADMIN") } }
        login("funcionario@biblioteca.local", "func12345").andExpect { jsonPath("$.usuario.perfil") { value("FUNCIONARIO") } }
        login("leitor@biblioteca.local", "leitor12345").andExpect { jsonPath("$.usuario.perfil") { value("LEITOR") } }
        mockMvc.get("/api/v1/catalog/books") { with(anonimo()) }.andExpect { jsonPath("$.totalElements") { value(12) } } // visitante
    }

    @Test
    fun `acervo e leitores`() {
        mockMvc.get("/api/v1/genres").andExpect { jsonPath("$.length()") { value(8) } }
        mockMvc.get("/api/v1/books?size=50").andExpect { jsonPath("$.totalElements") { value(12) } }
        mockMvc.get("/api/v1/books?disponibilidade=INDISPONIVEL").andExpect {
            jsonPath("$.content[*].titulo") { value(containsInAnyOrder("Eu", "O Alienista", "O Cortiço", "O Hobbit")) }
        }
        mockMvc.get("/api/v1/readers").andExpect {
            jsonPath("$.content[*].nome") { value(contains("Ana Souza", "Bruno Lima", "Carla Mendes", "Diego Rocha", "Lucas Almeida")) }
        }
        mockMvc.get("/api/v1/users").andExpect {
            jsonPath("$.content[*].nome") { value(contains("Administrador", "Fernanda Lima")) }
        }
    }

    @Test
    fun `emprestimos em situacoes variadas`() {
        mockMvc.get("/api/v1/reports/summary").andExpect {
            jsonPath("$.emprestimosAtivos") { value(4) }
            jsonPath("$.emprestimosAtrasados") { value(1) }
            jsonPath("$.exemplaresEmprestados") { value(6) }
            jsonPath("$.leitoresSuspensos") { value(1) }
        }
        mockMvc.get("/api/v1/reports/overdue-readers").andExpect {
            jsonPath("$[0].nome") { value("Bruno Lima") }
            jsonPath("$[0].maiorAtrasoDias") { value(5) }
        }
        mockMvc.get("/api/v1/readers?busca=carla").andExpect {
            jsonPath("$.content[0].suspenso") { value(true) }
            jsonPath("$.content[0].suspensoAte") { value(HOJE.plusDays(2).toString()) }
        }
        mockMvc.get("/api/v1/loans?situacao=DEVOLVIDO").andExpect { jsonPath("$.totalElements") { value(6) } }
    }

    @Test
    fun `leitor de exemplo ve os proprios emprestimos`() {
        val lucas = usuarioRepository.findByEmail("leitor@biblioteca.local")!!

        mockMvc.get("/api/v1/loans/me?situacao=ATIVO") { with(comoUsuario(lucas)) }.andExpect {
            jsonPath("$.totalElements") { value(2) }
            jsonPath("$.content[*].itens[0].tituloLivro") { value(containsInAnyOrder("O Hobbit", "Memórias Póstumas de Brás Cubas")) }
            jsonPath("$.content[?(@.itens[0].tituloLivro == 'Memórias Póstumas de Brás Cubas')].itens[0].renovacoes") { value(contains(1)) }
        }
    }

    @Test
    fun `rodar de novo nao duplica nada`() {
        assertFalse(dadosExemplo.criarSeNecessario())

        mockMvc.get("/api/v1/books?size=50").andExpect { jsonPath("$.totalElements") { value(12) } }
    }
}

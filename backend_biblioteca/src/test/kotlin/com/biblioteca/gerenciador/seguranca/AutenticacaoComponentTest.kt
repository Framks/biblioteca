package com.biblioteca.gerenciador.seguranca

import com.biblioteca.gerenciador.config.ComponentTestBase
import com.biblioteca.gerenciador.config.DadosTeste.Companion.CPFS_VALIDOS
import com.biblioteca.gerenciador.domain.Perfil
import com.jayway.jsonpath.JsonPath
import jakarta.servlet.http.Cookie
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasItems
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.RequestPostProcessor
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Login, refresh, logout e proteção da sessão (seção 6, RF15, RF16, RF20). */
class AutenticacaoComponentTest : ComponentTestBase() {

    private val email = "leitor@email.com"
    private val senha = "senha-correta-123"

    @BeforeEach
    fun cenario() {
        dados.usuarioComAcesso(Perfil.LEITOR, email, senha, nome = "Leitora Ana", cpf = CPFS_VALIDOS[0])
    }

    // ---------- helpers ----------

    private fun login(email: String = this.email, senha: String = this.senha) =
        postJson("/api/v1/auth/login", """{ "email": "$email", "senha": "$senha" }""", anonimo())

    private fun logar(): MvcResult = login().andExpect { status { isOk() } }.andReturn()

    private fun MvcResult.accessToken(): String = JsonPath.read(response.contentAsString, "$.accessToken")

    private fun MvcResult.refreshToken(): String = response.getCookie("refresh_token")!!.value

    /** O login reaproveita o device_id do navegador (aqui, o cookie padrão dos testes). */
    private fun MvcResult.deviceId(): String = response.getCookie("device_id")!!.value

    private fun comToken(token: String) = RequestPostProcessor {
        it.removeHeader(HttpHeaders.AUTHORIZATION)
        it.addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
        it
    }

    private fun comCookies(vararg cookies: Cookie) = RequestPostProcessor { it.setCookies(*cookies); it }

    private fun refresh(sessao: MvcResult, refreshToken: String = sessao.refreshToken()) =
        mockMvc.post("/api/v1/auth/refresh") {
            with(anonimo())
            with(comCookies(Cookie("device_id", sessao.deviceId()), Cookie("refresh_token", refreshToken)))
        }

    private fun eu(token: String, vararg extras: RequestPostProcessor) = mockMvc.get("/api/v1/auth/me") {
        with(comToken(token))
        extras.forEach { with(it) }
    }

    // ---------- login (6.1, 6.2) ----------

    @Test
    fun `login devolve access token, cookies seguros e permissoes`() {
        val resultado = login().andExpect {
            status { isOk() }
            jsonPath("$.tokenType") { value("Bearer") }
            jsonPath("$.expiresIn") { value(15 * 60) }
            jsonPath("$.usuario.nome") { value("Leitora Ana") }
            jsonPath("$.usuario.perfil") { value("LEITOR") }
            jsonPath("$.usuario.permissoes") { value(hasItems("book_read", "loan_read_own", "loan_renew_own")) }
            jsonPath("$.refreshToken") { doesNotExist() } // só no cookie
        }.andReturn()

        val refresh = resultado.response.getCookie("refresh_token")!!
        assertTrue(refresh.isHttpOnly)
        assertEquals("/api/v1/auth", refresh.path)
        assertTrue(resultado.response.getHeaders(HttpHeaders.SET_COOKIE).all { "SameSite=Strict" in it })
        assertTrue(resultado.response.getCookie("device_id")!!.isHttpOnly)

        eu(resultado.accessToken()).andExpect {
            status { isOk() }
            jsonPath("$.email") { value(email) }
        }
    }

    @Test
    fun `email inexistente e senha errada recebem a mesma resposta`() {
        login(email = "ninguem@email.com").andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("INVALID_CREDENTIALS") }
            jsonPath("$.message") { value("E-mail ou senha inválidos.") }
        }
        login(senha = "senha-errada").andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("INVALID_CREDENTIALS") }
            jsonPath("$.message") { value("E-mail ou senha inválidos.") }
        }
    }

    @Test
    fun `login ignora maiusculas no email`() {
        login(email = "LEITOR@Email.com").andExpect { status { isOk() } }
    }

    @Test
    fun `6_1 - bloqueia o login apos 5 tentativas erradas`() {
        repeat(5) { login(senha = "errada-$it").andExpect { status { isUnauthorized() } } }

        login().andExpect {
            status { isLocked() }
            jsonPath("$.code") { value("ACCOUNT_LOCKED") }
        }
    }

    // ---------- acesso sem login ----------

    @Test
    fun `rota protegida sem token retorna 401 e rotas publicas continuam abertas`() {
        mockMvc.get("/api/v1/books") { with(anonimo()) }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("UNAUTHENTICATED") }
            jsonPath("$.path") { value("/api/v1/books") }
        }
        mockMvc.get("/api/v1/catalog/books") { with(anonimo()) }.andExpect { status { isOk() } }
        mockMvc.get("/api/v1/genres") { with(anonimo()) }.andExpect { status { isOk() } }
    }

    @Test
    fun `token adulterado retorna 401`() {
        val token = logar().accessToken()

        eu(token.dropLast(2) + "xx").andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("INVALID_TOKEN") }
        }
    }

    // ---------- refresh com rotação (6.3) ----------

    @Test
    fun `6_3 - refresh devolve novo access token e troca o refresh token`() {
        val sessao = logar()

        val renovado = refresh(sessao).andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { exists() }
        }.andReturn()

        assertNotEquals(sessao.refreshToken(), renovado.refreshToken())
        eu(renovado.accessToken()).andExpect { status { isOk() } }
    }

    @Test
    fun `6_3 - reuso de refresh token revoga a sessao inteira`() {
        val sessao = logar()
        refresh(sessao).andExpect { status { isOk() } }

        refresh(sessao).andExpect { // mesmo refresh token, de novo: alguém tem uma cópia
            status { isUnauthorized() }
            jsonPath("$.code") { value("REFRESH_TOKEN_REUSED") }
        }
        eu(sessao.accessToken()).andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("SESSION_REVOKED") }
        }
    }

    @Test
    fun `refresh sem cookie retorna 401`() {
        mockMvc.post("/api/v1/auth/refresh") { with(anonimo()) }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("INVALID_REFRESH_TOKEN") }
        }
    }

    // ---------- logout e blocklist (6.4) ----------

    @Test
    fun `6_4 - apos o logout o token vai para a blocklist`() {
        val sessao = logar()

        mockMvc.post("/api/v1/auth/logout") { with(comToken(sessao.accessToken())) }.andExpect {
            status { isNoContent() }
            header { string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("refresh_token=;")) }
        }

        eu(sessao.accessToken()).andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("TOKEN_REVOKED") }
        }
        refresh(sessao).andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("SESSION_REVOKED") }
        }
    }

    // ---------- proteção contra uso em outro lugar (6.5) ----------

    @Test
    fun `6_5 - token copiado para outro dispositivo e barrado e a sessao e encerrada`() {
        val sessao = logar()

        // Mesmo JWT, mas vindo de outra máquina (sem o cookie device_id do navegador original).
        eu(sessao.accessToken(), comCookies(Cookie("device_id", "dispositivo-do-atacante-123456"))).andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("SESSION_REVOKED") }
        }
        // A sessão foi encerrada também para o dono: quem tinha o token perde o acesso.
        eu(sessao.accessToken()).andExpect { status { isUnauthorized() } }

        mockMvc.get("/api/v1/audit?acao=SESSAO_REVOGADA").andExpect {
            jsonPath("$.content[*].detalhes") { value(hasItem(org.hamcrest.Matchers.containsString("DISPOSITIVO_DIFERENTE"))) }
        }
    }

    @Test
    fun `6_5 - token sem cookie de dispositivo e barrado`() {
        val sessao = logar()

        eu(sessao.accessToken(), comCookies()).andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("SESSION_REVOKED") }
        }
    }

    @Test
    fun `6_5 - troca de navegador no meio da sessao e barrada`() {
        val sessao = logar()

        eu(sessao.accessToken(), RequestPostProcessor {
            it.removeHeader(HttpHeaders.USER_AGENT)
            it.addHeader(HttpHeaders.USER_AGENT, "Outro-Navegador/1.0")
            it
        }).andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("SESSION_REVOKED") }
        }
    }

    @Test
    fun `6_5 - mudanca so de IP e permitida e fica na auditoria`() {
        val sessao = logar()

        eu(sessao.accessToken(), RequestPostProcessor { it.remoteAddr = "200.10.20.30"; it }).andExpect { status { isOk() } }

        mockMvc.get("/api/v1/audit?acao=IP_ALTERADO").andExpect {
            jsonPath("$.content[0].detalhes") { value(org.hamcrest.Matchers.containsString("127.0.0.1 -> 200.10.20.30")) }
            jsonPath("$.content[0].usuario.nome") { value("Leitora Ana") }
        }
    }

    // ---------- senha e sessões (RF20) ----------

    @Test
    fun `trocar a propria senha encerra as outras sessoes`() {
        val sessaoA = logar()
        val sessaoB = logar()

        putJson(
            "/api/v1/auth/password",
            """{ "senhaAtual": "$senha", "novaSenha": "nova-senha-456" }""",
            comToken(sessaoA.accessToken()),
        ).andExpect { status { isNoContent() } }

        eu(sessaoA.accessToken()).andExpect { status { isOk() } }
        eu(sessaoB.accessToken()).andExpect { status { isUnauthorized() } }
        login(senha = "nova-senha-456").andExpect { status { isOk() } }
        login().andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `senha atual errada nao troca a senha`() {
        val sessao = logar()

        putJson("/api/v1/auth/password", """{ "senhaAtual": "chute", "novaSenha": "nova-senha-456" }""", comToken(sessao.accessToken()))
            .andExpect {
                status { isUnprocessableContent() }
                jsonPath("$.code") { value("CURRENT_PASSWORD_MISMATCH") }
            }
    }

    @Test
    fun `lista e encerra as proprias sessoes`() {
        val atual = logar()
        val outra = logar()

        val lista = mockMvc.get("/api/v1/auth/sessions") { with(comToken(atual.accessToken())) }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[?(@.atual == true)].userAgent") { value(hasItem("MockMvc-Teste")) }
        }.andReturn()
        val idOutra: String = JsonPath.read<List<String>>(lista.response.contentAsString, "$[?(@.atual == false)].id").first()

        mockMvc.delete("/api/v1/auth/sessions/$idOutra") { with(comToken(atual.accessToken())) }.andExpect { status { isNoContent() } }

        eu(outra.accessToken()).andExpect { status { isUnauthorized() } }
        mockMvc.get("/api/v1/auth/sessions") { with(comToken(atual.accessToken())) }.andExpect { jsonPath("$.length()") { value(1) } }
    }

    @Test
    fun `nao encerra sessao de outro usuario sem permissao`() {
        dados.usuarioComAcesso(Perfil.LEITOR, "outro@email.com", senha, cpf = CPFS_VALIDOS[1])
        val doOutro = login(email = "outro@email.com").andReturn()
        val idDoOutro: String = JsonPath.read<List<String>>(
            mockMvc.get("/api/v1/auth/sessions") { with(comToken(doOutro.accessToken())) }.andReturn().response.contentAsString,
            "$[*].id",
        ).first()
        val minha = logar()

        mockMvc.delete("/api/v1/auth/sessions/$idDoOutro") { with(comToken(minha.accessToken())) }.andExpect {
            status { isNotFound() }
        }
        assertNotNull(eu(doOutro.accessToken()).andExpect { status { isOk() } })
    }
}

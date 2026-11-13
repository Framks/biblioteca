package com.biblioteca.gerenciador.config

import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.repositories.UsuarioRepository
import com.biblioteca.gerenciador.security.Cookies
import com.biblioteca.gerenciador.security.TokenService
import com.biblioteca.gerenciador.services.Relogio
import com.biblioteca.gerenciador.services.SessaoService
import jakarta.servlet.http.Cookie
import org.springframework.boot.test.context.TestComponent
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.transaction.support.TransactionTemplate

/**
 * Autenticação nos testes: por padrão, TODA requisição do MockMvc vai como o administrador inicial,
 * com uma sessão real (token JWT + cookie device_id + User-Agent), passando pelo filtro de segurança de verdade.
 * Para testar como outro usuário ou sem login, use comoUsuario(...) / anonimo() da ComponentTestBase.
 */
@TestConfiguration
class SegurancaTesteConfig {

    @Bean
    fun autenticarComoAdminPorPadrao(credenciais: CredenciaisTeste) = MockMvcBuilderCustomizer { builder ->
        val admin = credenciais.doAdmin()
        val padrao = MockMvcRequestBuilders.get("/")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${admin.accessToken}")
            .header(HttpHeaders.USER_AGENT, CredenciaisTeste.USER_AGENT)
            .cookie(Cookie(Cookies.DEVICE_ID, admin.deviceId))
        (builder as DefaultMockMvcBuilder).defaultRequest<DefaultMockMvcBuilder>(padrao)
    }
}

/** Cria sessões e tokens reais para os testes. */
@TestComponent
class CredenciaisTeste(
    private val sessaoService: SessaoService,
    private val tokenService: TokenService,
    private val usuarioRepository: UsuarioRepository,
    private val relogio: Relogio,
    private val transacao: TransactionTemplate,
) {
    private var admin: Credenciais? = null

    /** Sessão do administrador, criada (e gravada de verdade) uma vez por contexto de teste. */
    fun doAdmin(): Credenciais = admin ?: transacao.execute {
        criar(usuarioRepository.findByEmail("admin@biblioteca.local")!!, "dispositivo-admin-dos-testes")
    }!!.also { admin = it }

    fun criar(usuario: Usuario, deviceId: String = "dispositivo-${usuario.id}-${System.nanoTime()}"): Credenciais {
        val aberta = sessaoService.abrir(usuario, "127.0.0.1", USER_AGENT, deviceId, relogio.agora())
        val token = tokenService.emitir(usuario, aberta.sessao.id)
        return Credenciais(token.valor, deviceId, aberta.refreshToken)
    }

    data class Credenciais(val accessToken: String, val deviceId: String, val refreshToken: String) {
        /** Aplica estas credenciais na requisição (substitui as do administrador padrão). */
        fun aplicar(): RequestPostProcessor = RequestPostProcessor { request ->
            request.removeHeader(HttpHeaders.AUTHORIZATION)
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            request.setCookies(Cookie(Cookies.DEVICE_ID, deviceId))
            request
        }
    }

    companion object {
        const val USER_AGENT = "MockMvc-Teste"

        /** Remove a autenticação padrão (requisição de visitante). */
        fun anonimo(): RequestPostProcessor = RequestPostProcessor { request: MockHttpServletRequest ->
            request.removeHeader(HttpHeaders.AUTHORIZATION)
            request
        }
    }
}

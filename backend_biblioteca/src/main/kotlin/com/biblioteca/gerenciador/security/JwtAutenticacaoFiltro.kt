package com.biblioteca.gerenciador.security

import com.biblioteca.gerenciador.exceptions.NaoAutenticadoException
import com.biblioteca.gerenciador.services.Relogio
import com.biblioteca.gerenciador.services.SessaoService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * Autentica cada requisição que traz "Authorization: Bearer <jwt>":
 *  1. assinatura e validade do JWT;
 *  2. jti fora da blocklist (6.4);
 *  3. sessão ativa, usada do mesmo dispositivo (cookie device_id) e navegador do login (6.5).
 * Se tudo estiver certo, coloca o usuário e suas permissões no SecurityContext; senão responde 401.
 * Sem o header, segue anônimo: as rotas protegidas são barradas depois, pelo SecurityConfig.
 */
class JwtAutenticacaoFiltro(
    private val tokenService: TokenService,
    private val sessaoService: SessaoService,
    private val relogio: Relogio,
    private val respostas: RespostaErroSeguranca,
) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val token = request.getHeader(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith(PREFIXO) }
            ?.removePrefix(PREFIXO)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        if (token != null) {
            try {
                SecurityContextHolder.getContext().authentication = autenticar(token, request)
            } catch (e: NaoAutenticadoException) {
                SecurityContextHolder.clearContext()
                respostas.escrever(request, response, HttpStatus.UNAUTHORIZED, e.codigo, e.message!!)
                return
            }
        }
        chain.doFilter(request, response)
    }

    private fun autenticar(token: String, request: HttpServletRequest): UsernamePasswordAuthenticationToken {
        val jwt = try {
            tokenService.validar(token)
        } catch (e: JwtException) {
            throw NaoAutenticadoException("INVALID_TOKEN", "Token inválido ou expirado.")
        }
        val sessaoId = jwt.getClaimAsString(TokenService.CLAIM_SESSAO)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        val jti: String? = jwt.id
        if (sessaoId == null || jti == null) throw NaoAutenticadoException("INVALID_TOKEN", "Token inválido ou expirado.")

        if (sessaoService.tokenBloqueado(jti)) {
            throw NaoAutenticadoException("TOKEN_REVOKED", "Este token foi revogado. Faça login novamente.")
        }
        val dados = sessaoService.validarRequisicao(
            sessaoId = sessaoId,
            deviceId = Cookies.ler(request, Cookies.DEVICE_ID),
            userAgent = request.getHeader(HttpHeaders.USER_AGENT).orEmpty(),
            ip = request.remoteAddr,
            agora = relogio.agora(),
        )
        val principal = UsuarioAutenticado(
            usuarioId = dados.usuarioId,
            sessaoId = sessaoId,
            jti = jti,
            tokenExpiraEm = jwt.expiresAt!!,
            perfil = dados.perfil,
            permissoes = dados.permissoes,
        )
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, dados.permissoes.map(::SimpleGrantedAuthority))
    }

    private companion object {
        const val PREFIXO = "Bearer "
    }
}

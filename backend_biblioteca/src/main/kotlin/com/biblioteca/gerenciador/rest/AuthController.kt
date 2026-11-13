package com.biblioteca.gerenciador.rest

import com.biblioteca.gerenciador.facades.AuthFacade
import com.biblioteca.gerenciador.rest.requests.AlterarSenhaRequest
import com.biblioteca.gerenciador.rest.requests.LoginRequest
import com.biblioteca.gerenciador.rest.responses.LoginResponse
import com.biblioteca.gerenciador.rest.responses.SessaoResponse
import com.biblioteca.gerenciador.rest.responses.UsuarioLogadoResponse
import com.biblioteca.gerenciador.security.Cookies
import com.biblioteca.gerenciador.services.Relogio
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Autenticação (seção 6). Fluxo do frontend:
 *  1. POST /auth/login -> guarda o accessToken em memória; os cookies refresh_token e device_id ficam no navegador.
 *  2. Envia "Authorization: Bearer <accessToken>" e os cookies (credentials: "include") em toda chamada.
 *  3. Recebeu 401 com INVALID_TOKEN (token expirado)? Chama POST /auth/refresh e repete a chamada.
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val facade: AuthFacade,
    private val cookies: Cookies,
    private val relogio: Relogio,
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest, http: HttpServletRequest): ResponseEntity<LoginResponse> {
        val resultado = facade.login(request, http.remoteAddr, userAgent(http), Cookies.ler(http, Cookies.DEVICE_ID))
        return comCookies(resultado)
    }

    /** Lê o cookie refresh_token, devolve um novo access token e troca o refresh token (rotação). */
    @PostMapping("/refresh")
    fun renovar(http: HttpServletRequest): ResponseEntity<LoginResponse> {
        val resultado = facade.renovar(
            refreshToken = Cookies.ler(http, Cookies.REFRESH_TOKEN),
            deviceId = Cookies.ler(http, Cookies.DEVICE_ID),
            userAgent = userAgent(http),
        )
        return comCookies(resultado)
    }

    @PostMapping("/logout")
    fun logout(): ResponseEntity<Unit> {
        facade.logout()
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, cookies.apagarRefreshToken().toString())
            .build()
    }

    @GetMapping("/me")
    fun eu(): UsuarioLogadoResponse = facade.eu()

    @PutMapping("/password")
    fun alterarSenha(@Valid @RequestBody request: AlterarSenhaRequest): ResponseEntity<Unit> {
        facade.alterarSenha(request)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasAuthority('session_manage_own')")
    fun sessoes(): List<SessaoResponse> = facade.minhasSessoes()

    @DeleteMapping("/sessions/{id}")
    @PreAuthorize("hasAnyAuthority('session_manage_own', 'session_manage')")
    fun encerrarSessao(@PathVariable id: UUID): ResponseEntity<Unit> {
        facade.encerrarSessao(id)
        return ResponseEntity.noContent().build()
    }

    private fun comCookies(resultado: AuthFacade.ResultadoAutenticacao): ResponseEntity<LoginResponse> =
        ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookies.refreshToken(resultado.refreshToken, resultado.refreshExpiraEm, relogio.agora()).toString())
            .header(HttpHeaders.SET_COOKIE, cookies.deviceId(resultado.deviceId).toString())
            .body(resultado.resposta)

    private fun userAgent(http: HttpServletRequest) = http.getHeader(HttpHeaders.USER_AGENT).orEmpty()
}

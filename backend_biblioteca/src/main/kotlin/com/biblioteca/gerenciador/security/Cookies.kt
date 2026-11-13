package com.biblioteca.gerenciador.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

/**
 * Cookies de segurança (seção 6.2). Ambos HttpOnly (JavaScript não lê, então um XSS não consegue roubá-los)
 * e SameSite=Strict (o navegador não os envia em requisições vindas de outros sites).
 *
 * - refresh_token: só vai para /api/v1/auth (refresh e logout).
 * - device_id: identifica o navegador; vai em toda chamada à API e prende a sessão a ele (seção 6.5).
 */
@Component
class Cookies(
    /** Secure = só trafega em HTTPS. Desligado em dev (http://localhost). */
    @Value("\${app.seguranca.cookie-seguro}") private val seguro: Boolean,
) {

    fun refreshToken(valor: String, expiraEm: Instant, agora: Instant): ResponseCookie =
        criar(REFRESH_TOKEN, valor, CAMINHO_AUTH, Duration.between(agora, expiraEm))

    fun deviceId(valor: String): ResponseCookie = criar(DEVICE_ID, valor, CAMINHO_API, Duration.ofDays(365))

    fun apagarRefreshToken(): ResponseCookie = criar(REFRESH_TOKEN, "", CAMINHO_AUTH, Duration.ZERO)

    private fun criar(nome: String, valor: String, caminho: String, duracao: Duration) =
        ResponseCookie.from(nome, valor)
            .httpOnly(true)
            .secure(seguro)
            .sameSite("Strict")
            .path(caminho)
            .maxAge(duracao)
            .build()

    companion object {
        const val REFRESH_TOKEN = "refresh_token"
        const val DEVICE_ID = "device_id"
        private const val CAMINHO_AUTH = "/api/v1/auth"
        private const val CAMINHO_API = "/api"

        fun ler(request: HttpServletRequest, nome: String): String? =
            request.cookies?.firstOrNull { it.name == nome }?.value?.takeIf { it.isNotBlank() }
    }
}

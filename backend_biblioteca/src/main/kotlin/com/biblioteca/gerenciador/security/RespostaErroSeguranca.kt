package com.biblioteca.gerenciador.security

import com.biblioteca.gerenciador.rest.responses.ErroResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.Instant

/**
 * Erros de segurança que acontecem antes do controller (no filtro), no mesmo formato de erro da API (seção 10.2).
 * Os que acontecem dentro do controller (ex.: @PreAuthorize) passam pelo GlobalExceptionHandler.
 */
@Component
class RespostaErroSeguranca(
    private val jsonMapper: JsonMapper,
    private val clock: Clock,
) : AuthenticationEntryPoint, AccessDeniedHandler {

    /** Rota protegida chamada sem token. */
    override fun commence(request: HttpServletRequest, response: HttpServletResponse, ex: AuthenticationException) =
        escrever(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Faça login para acessar este recurso.")

    override fun handle(request: HttpServletRequest, response: HttpServletResponse, ex: AccessDeniedException) =
        escrever(request, response, HttpStatus.FORBIDDEN, "FORBIDDEN", "Você não tem permissão para esta ação.")

    fun escrever(request: HttpServletRequest, response: HttpServletResponse, status: HttpStatus, codigo: String, mensagem: String) {
        val corpo = ErroResponse(
            status = status.value(),
            code = codigo,
            message = mensagem,
            timestamp = Instant.now(clock),
            path = request.requestURI,
        )
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        jsonMapper.writeValue(response.outputStream, corpo)
    }
}

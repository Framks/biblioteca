package com.biblioteca.gerenciador.exceptions

import com.biblioteca.gerenciador.rest.responses.CampoErro
import com.biblioteca.gerenciador.rest.responses.ErroResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.core.PropertyReferenceException
import org.springframework.http.HttpStatus
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.time.Clock
import java.time.Instant

/** Converte qualquer exceção no formato de erro padrão da API. */
@RestControllerAdvice
class GlobalExceptionHandler(private val clock: Clock) {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun api(ex: ApiException, req: HttpServletRequest) =
        erro(ex.status, ex.codigo, ex.message ?: "", req)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validacao(ex: MethodArgumentNotValidException, req: HttpServletRequest): ResponseEntity<ErroResponse> {
        val campos = ex.bindingResult.fieldErrors.map { CampoErro(it.field, it.defaultMessage ?: "inválido") }
        return erro(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dados inválidos.", req, campos)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun corpoInvalido(ex: HttpMessageNotReadableException, req: HttpServletRequest) =
        erro(HttpStatus.BAD_REQUEST, "INVALID_BODY", "Corpo da requisição ausente ou mal formatado.", req)

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun parametroInvalido(ex: MethodArgumentTypeMismatchException, req: HttpServletRequest) =
        erro(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "Valor inválido para o parâmetro '${ex.name}'.", req)

    @ExceptionHandler(PropertyReferenceException::class)
    fun ordenacaoInvalida(ex: PropertyReferenceException, req: HttpServletRequest) =
        erro(HttpStatus.BAD_REQUEST, "INVALID_SORT", "Não é possível ordenar por '${ex.propertyName}'.", req)

    @ExceptionHandler(NoResourceFoundException::class)
    fun rotaInexistente(ex: NoResourceFoundException, req: HttpServletRequest) =
        erro(HttpStatus.NOT_FOUND, "NOT_FOUND", "Rota não encontrada.", req)

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun metodoNaoSuportado(ex: HttpRequestMethodNotSupportedException, req: HttpServletRequest) =
        erro(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Método ${ex.method} não suportado nesta rota.", req)

    /** Duas pessoas alteraram o mesmo registro ao mesmo tempo (ex.: o mesmo exemplar emprestado em dois balcões). */
    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    fun concorrencia(ex: ObjectOptimisticLockingFailureException, req: HttpServletRequest) =
        erro(HttpStatus.CONFLICT, "CONCURRENT_UPDATE", "O registro foi alterado por outra pessoa. Atualize e tente novamente.", req)

    /** Rede de segurança: constraint do banco violada sem ter sido validada antes no service. */
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun integridade(ex: DataIntegrityViolationException, req: HttpServletRequest): ResponseEntity<ErroResponse> {
        log.warn("Violação de integridade não tratada em {} {}: {}", req.method, req.requestURI, ex.mostSpecificCause.message)
        return erro(HttpStatus.CONFLICT, "DATA_CONFLICT", "A operação conflita com dados existentes.", req)
    }

    @ExceptionHandler(Exception::class)
    fun inesperado(ex: Exception, req: HttpServletRequest): ResponseEntity<ErroResponse> {
        log.error("Erro inesperado em {} {}", req.method, req.requestURI, ex)
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Erro interno. Tente novamente mais tarde.", req)
    }

    private fun erro(
        status: HttpStatus,
        codigo: String,
        mensagem: String,
        req: HttpServletRequest,
        campos: List<CampoErro> = emptyList(),
    ) = ResponseEntity.status(status).body(
        ErroResponse(
            status = status.value(),
            code = codigo,
            message = mensagem,
            fields = campos,
            timestamp = Instant.now(clock),
            path = req.requestURI,
        ),
    )
}

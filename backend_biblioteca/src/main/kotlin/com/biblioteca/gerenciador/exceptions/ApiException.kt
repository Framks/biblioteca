package com.biblioteca.gerenciador.exceptions

import org.springframework.http.HttpStatus

/**
 * Erro esperado da aplicação. O GlobalExceptionHandler converte no formato de erro padrão (seção 10.2).
 * [codigo] é estável e pode ser usado pelo frontend (ex.: BOOK_NOT_FOUND); [message] é para o usuário.
 */
abstract class ApiException(
    val status: HttpStatus,
    val codigo: String,
    mensagem: String,
) : RuntimeException(mensagem)

/** 404: recurso não existe (ou está inativo). */
class NaoEncontradoException(codigo: String, mensagem: String) :
    ApiException(HttpStatus.NOT_FOUND, codigo, mensagem)

/** 409: conflito com dado existente (ex.: duplicidade). */
class ConflitoException(codigo: String, mensagem: String) :
    ApiException(HttpStatus.CONFLICT, codigo, mensagem)

/** 422: regra de negócio violada (ex.: limite de empréstimos). */
class NegocioException(codigo: String, mensagem: String) :
    ApiException(HttpStatus.UNPROCESSABLE_CONTENT, codigo, mensagem)

/** 400: requisição inválida que não é erro de validação de campo (ex.: ordenação não permitida). */
class RequisicaoInvalidaException(codigo: String, mensagem: String) :
    ApiException(HttpStatus.BAD_REQUEST, codigo, mensagem)

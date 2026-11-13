package com.biblioteca.gerenciador.facades

import com.biblioteca.gerenciador.mappers.aplicarEm
import com.biblioteca.gerenciador.mappers.toResponse
import com.biblioteca.gerenciador.rest.requests.AtualizarExemplarRequest
import com.biblioteca.gerenciador.rest.requests.NovosExemplaresRequest
import com.biblioteca.gerenciador.rest.responses.ExemplarResponse
import com.biblioteca.gerenciador.services.ExemplarService
import com.biblioteca.gerenciador.services.LivroService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Facade: ponto de entrada dos controllers.
 * Coordena os services, converte DTO <-> entidade e define a transação.
 */
@Component
class ExemplarFacade(
    private val exemplarService: ExemplarService,
    private val livroService: LivroService,
) {

    @Transactional(readOnly = true)
    fun listarPorLivro(livroId: Long): List<ExemplarResponse> =
        exemplarService.listarPorLivro(livroService.buscarAtivo(livroId)).map { it.toResponse() }

    @Transactional(readOnly = true)
    fun buscar(id: Long): ExemplarResponse = exemplarService.buscar(id).toResponse()

    @Transactional
    fun criar(livroId: Long, request: NovosExemplaresRequest): List<ExemplarResponse> {
        val livro = livroService.buscarAtivo(livroId)
        return exemplarService.criar(livro, request.quantidade, request.condicao).map { it.toResponse() }
    }

    @Transactional
    fun atualizar(id: Long, request: AtualizarExemplarRequest): ExemplarResponse {
        val exemplar = exemplarService.buscar(id)
        request.aplicarEm(exemplar)
        return exemplarService.salvar(exemplar).toResponse()
    }
}

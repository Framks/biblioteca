package com.biblioteca.gerenciador.facades

import com.biblioteca.gerenciador.mappers.toEntity
import com.biblioteca.gerenciador.mappers.toResponse
import com.biblioteca.gerenciador.rest.requests.GeneroRequest
import com.biblioteca.gerenciador.rest.responses.GeneroResponse
import com.biblioteca.gerenciador.services.GeneroService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Facade: ponto de entrada dos controllers.
 * Coordena os services, converte DTO <-> entidade e define a transação.
 */
@Component
class GeneroFacade(private val generoService: GeneroService) {

    @Transactional(readOnly = true)
    fun listar(): List<GeneroResponse> = generoService.listar().map { it.toResponse() }

    @Transactional
    fun criar(request: GeneroRequest): GeneroResponse =
        generoService.salvar(request.toEntity()).toResponse()

    @Transactional
    fun atualizar(id: Long, request: GeneroRequest): GeneroResponse {
        val genero = generoService.buscar(id)
        genero.renomear(request.nome)
        return generoService.salvar(genero).toResponse()
    }
}

package com.biblioteca.gerenciador.facades

import com.biblioteca.gerenciador.enums.AcaoAuditoria
import com.biblioteca.gerenciador.mappers.toEntity
import com.biblioteca.gerenciador.mappers.toResponse
import com.biblioteca.gerenciador.rest.requests.GeneroRequest
import com.biblioteca.gerenciador.rest.responses.GeneroResponse
import com.biblioteca.gerenciador.services.AuditoriaService
import com.biblioteca.gerenciador.services.GeneroService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Facade: ponto de entrada dos controllers.
 * Coordena os services, converte DTO <-> entidade e define a transação.
 */
@Component
class GeneroFacade(
    private val generoService: GeneroService,
    private val auditoria: AuditoriaService,
) {

    @Transactional(readOnly = true)
    fun listar(): List<GeneroResponse> = generoService.listar().map { it.toResponse() }

    @Transactional
    fun criar(request: GeneroRequest): GeneroResponse {
        val genero = generoService.salvar(request.toEntity())
        auditoria.registrar(AcaoAuditoria.GENERO_CRIADO, "Genero", genero.id, genero.nome)
        return genero.toResponse()
    }

    @Transactional
    fun atualizar(id: Long, request: GeneroRequest): GeneroResponse {
        val genero = generoService.buscar(id)
        genero.renomear(request.nome)
        generoService.salvar(genero)
        auditoria.registrar(AcaoAuditoria.GENERO_ATUALIZADO, "Genero", genero.id, genero.nome)
        return genero.toResponse()
    }
}

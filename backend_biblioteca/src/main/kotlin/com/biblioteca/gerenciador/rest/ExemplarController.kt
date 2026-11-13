package com.biblioteca.gerenciador.rest

import com.biblioteca.gerenciador.facades.ExemplarFacade
import com.biblioteca.gerenciador.rest.requests.AtualizarExemplarRequest
import com.biblioteca.gerenciador.rest.requests.NovosExemplaresRequest
import com.biblioteca.gerenciador.rest.responses.ExemplarResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class ExemplarController(private val facade: ExemplarFacade) {

    @GetMapping("/books/{livroId}/copies")
    fun listarPorLivro(@PathVariable livroId: Long): List<ExemplarResponse> = facade.listarPorLivro(livroId)

    @PostMapping("/books/{livroId}/copies")
    @ResponseStatus(HttpStatus.CREATED)
    fun criar(
        @PathVariable livroId: Long,
        @Valid @RequestBody request: NovosExemplaresRequest,
    ): List<ExemplarResponse> = facade.criar(livroId, request)

    @GetMapping("/copies/{id}")
    fun buscar(@PathVariable id: Long): ExemplarResponse = facade.buscar(id)

    @PatchMapping("/copies/{id}")
    fun atualizar(
        @PathVariable id: Long,
        @Valid @RequestBody request: AtualizarExemplarRequest,
    ): ExemplarResponse = facade.atualizar(id, request)
}

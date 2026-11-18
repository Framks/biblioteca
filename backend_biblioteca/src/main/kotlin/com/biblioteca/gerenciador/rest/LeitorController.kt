package com.biblioteca.gerenciador.rest

import com.biblioteca.gerenciador.facades.LeitorFacade
import com.biblioteca.gerenciador.rest.requests.AcessoLeitorRequest
import com.biblioteca.gerenciador.rest.requests.LeitorRequest
import com.biblioteca.gerenciador.rest.responses.LeitorResponse
import com.biblioteca.gerenciador.rest.responses.PageResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/v1/readers")
class LeitorController(private val facade: LeitorFacade) {

    /** GET /readers?busca=maria&page=0&size=20&sort=nome,asc. A busca procura no nome, e-mail e documento. */
    @PreAuthorize("hasAuthority('reader_read')")
    @GetMapping
    fun pesquisar(
        @RequestParam(required = false) busca: String?,
        @PageableDefault(size = 20, sort = ["nome"]) pageable: Pageable,
    ): PageResponse<LeitorResponse> = facade.pesquisar(busca, pageable.validarOrdenacao("nome", "dataCadastro"))

    @PreAuthorize("hasAuthority('reader_read')")
    @GetMapping("/{id}")
    fun buscar(@PathVariable id: Long): LeitorResponse = facade.buscar(id)

    @PreAuthorize("hasAuthority('reader_create')")
    @PostMapping
    fun criar(@Valid @RequestBody request: LeitorRequest): ResponseEntity<LeitorResponse> {
        val criado = facade.criar(request)
        return ResponseEntity.created(URI.create("/api/v1/readers/${criado.id}")).body(criado)
    }

    @PreAuthorize("hasAuthority('reader_update')")
    @PutMapping("/{id}")
    fun atualizar(@PathVariable id: Long, @Valid @RequestBody request: LeitorRequest): LeitorResponse =
        facade.atualizar(id, request)

    @PreAuthorize("hasAuthority('reader_update')")
    @PostMapping("/{id}/access")
    fun habilitarAcesso(@PathVariable id: Long, @Valid @RequestBody request: AcessoLeitorRequest): LeitorResponse =
        facade.habilitarAcesso(id, request)

    @PreAuthorize("hasAuthority('reader_delete')")
    @DeleteMapping("/{id}")
    fun inativar(@PathVariable id: Long): ResponseEntity<Unit> {
        facade.inativar(id)
        return ResponseEntity.noContent().build()
    }
}

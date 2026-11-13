package com.biblioteca.gerenciador.rest

import com.biblioteca.gerenciador.domain.FiltroLivro
import com.biblioteca.gerenciador.enums.FiltroDisponibilidade
import com.biblioteca.gerenciador.facades.LivroFacade
import com.biblioteca.gerenciador.rest.requests.LivroRequest
import com.biblioteca.gerenciador.rest.responses.LivroResponse
import com.biblioteca.gerenciador.rest.responses.PageResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
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
@RequestMapping("/api/v1/books")
class LivroController(private val facade: LivroFacade) {

    /** GET /books?titulo=&autor=&isbn=&generoId=&disponibilidade=DISPONIVEL&page=0&size=20&sort=titulo,asc */
    @GetMapping
    fun pesquisar(
        @RequestParam(required = false) titulo: String?,
        @RequestParam(required = false) autor: String?,
        @RequestParam(required = false) isbn: String?,
        @RequestParam(required = false) generoId: Long?,
        @RequestParam(required = false) disponibilidade: FiltroDisponibilidade?,
        @PageableDefault(size = 20, sort = ["titulo"]) pageable: Pageable,
    ): PageResponse<LivroResponse> = facade.pesquisar(
        FiltroLivro(titulo, autor, isbn, generoId, disponibilidade),
        pageable.validarOrdenacao(*ORDENACAO_LIVRO),
    )

    @GetMapping("/{id}")
    fun buscar(@PathVariable id: Long): LivroResponse = facade.buscar(id)

    @PostMapping
    fun criar(@Valid @RequestBody request: LivroRequest): ResponseEntity<LivroResponse> {
        val criado = facade.criar(request)
        return ResponseEntity.created(URI.create("/api/v1/books/${criado.id}")).body(criado)
    }

    @PutMapping("/{id}")
    fun atualizar(@PathVariable id: Long, @Valid @RequestBody request: LivroRequest): LivroResponse =
        facade.atualizar(id, request)

    @DeleteMapping("/{id}")
    fun inativar(@PathVariable id: Long): ResponseEntity<Unit> {
        facade.inativar(id)
        return ResponseEntity.noContent().build()
    }
}

/** Campos pelos quais a listagem de livros pode ser ordenada. */
val ORDENACAO_LIVRO = arrayOf("titulo", "autor", "anoPublicacao", "dataCadastro")

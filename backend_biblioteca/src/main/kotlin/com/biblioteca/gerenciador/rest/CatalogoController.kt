package com.biblioteca.gerenciador.rest

import com.biblioteca.gerenciador.domain.FiltroLivro
import com.biblioteca.gerenciador.enums.FiltroDisponibilidade
import com.biblioteca.gerenciador.facades.LivroFacade
import com.biblioteca.gerenciador.rest.responses.CatalogoLivroResponse
import com.biblioteca.gerenciador.rest.responses.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** Catálogo público para visitantes (RF05). Continuará sem login na fase de segurança. */
@RestController
@RequestMapping("/api/v1/catalog")
class CatalogoController(private val facade: LivroFacade) {

    /** GET /catalog/books?titulo=&autor=&generoId=&apenasDisponiveis=true */
    @GetMapping("/books")
    fun pesquisar(
        @RequestParam(required = false) titulo: String?,
        @RequestParam(required = false) autor: String?,
        @RequestParam(required = false) generoId: Long?,
        @RequestParam(defaultValue = "false") apenasDisponiveis: Boolean,
        @PageableDefault(size = 20, sort = ["titulo"]) pageable: Pageable,
    ): PageResponse<CatalogoLivroResponse> = facade.pesquisarCatalogo(
        FiltroLivro(
            titulo = titulo,
            autor = autor,
            generoId = generoId,
            disponibilidade = if (apenasDisponiveis) FiltroDisponibilidade.DISPONIVEL else null,
        ),
        pageable.validarOrdenacao(*ORDENACAO_LIVRO),
    )
}

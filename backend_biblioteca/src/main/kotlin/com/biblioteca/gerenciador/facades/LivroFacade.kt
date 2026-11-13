package com.biblioteca.gerenciador.facades

import com.biblioteca.gerenciador.domain.ContagemExemplares
import com.biblioteca.gerenciador.domain.FiltroLivro
import com.biblioteca.gerenciador.domain.Livro
import com.biblioteca.gerenciador.exceptions.NegocioException
import com.biblioteca.gerenciador.mappers.aplicarEm
import com.biblioteca.gerenciador.mappers.toCatalogoResponse
import com.biblioteca.gerenciador.mappers.toEntity
import com.biblioteca.gerenciador.mappers.toResponse
import com.biblioteca.gerenciador.rest.requests.LivroRequest
import com.biblioteca.gerenciador.rest.responses.CatalogoLivroResponse
import com.biblioteca.gerenciador.rest.responses.LivroResponse
import com.biblioteca.gerenciador.rest.responses.PageResponse
import com.biblioteca.gerenciador.services.ExemplarService
import com.biblioteca.gerenciador.services.GeneroService
import com.biblioteca.gerenciador.services.LivroService
import com.biblioteca.gerenciador.services.UsuarioLogado
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Facade: ponto de entrada dos controllers.
 * Coordena os services, converte DTO <-> entidade e define a transação.
 */
@Component
class LivroFacade(
    private val livroService: LivroService,
    private val generoService: GeneroService,
    private val exemplarService: ExemplarService,
    private val usuarioLogado: UsuarioLogado,
) {

    @Transactional(readOnly = true)
    fun pesquisar(filtro: FiltroLivro, pageable: Pageable): PageResponse<LivroResponse> {
        val pagina = livroService.pesquisar(filtro, pageable)
        val contagens = contar(pagina.content)
        return PageResponse.de(pagina) { it.toResponse(contagens.de(it)) }
    }

    /** Catálogo público (RF05). */
    @Transactional(readOnly = true)
    fun pesquisarCatalogo(filtro: FiltroLivro, pageable: Pageable): PageResponse<CatalogoLivroResponse> {
        val pagina = livroService.pesquisar(filtro, pageable)
        val contagens = contar(pagina.content)
        return PageResponse.de(pagina) { it.toCatalogoResponse(contagens.de(it)) }
    }

    @Transactional(readOnly = true)
    fun buscar(id: Long): LivroResponse = responder(livroService.buscarAtivo(id))

    @Transactional
    fun criar(request: LivroRequest): LivroResponse {
        val generos = generoService.buscarTodos(request.generoIds)
        return responder(livroService.salvar(request.toEntity(generos, usuarioLogado.obter())))
    }

    @Transactional
    fun atualizar(id: Long, request: LivroRequest): LivroResponse {
        val livro = livroService.buscarAtivo(id)
        val generos = generoService.buscarTodos(request.generoIds)
        request.aplicarEm(livro, generos)
        return responder(livroService.salvar(livro))
    }

    /** RN13: não é possível inativar livro com exemplar emprestado. */
    @Transactional
    fun inativar(id: Long) {
        val livro = livroService.buscarAtivo(id)
        if (exemplarService.possuiEmprestado(livro)) {
            throw NegocioException("BOOK_HAS_ACTIVE_LOANS", "O livro possui exemplares emprestados e não pode ser inativado.")
        }
        livroService.inativar(livro)
    }

    private fun responder(livro: Livro) = livro.toResponse(contar(listOf(livro)).de(livro))

    private fun contar(livros: List<Livro>) = exemplarService.contar(livros.mapNotNull { it.id })

    private fun Map<Long, ContagemExemplares>.de(livro: Livro) = this[livro.id] ?: ContagemExemplares.VAZIA
}

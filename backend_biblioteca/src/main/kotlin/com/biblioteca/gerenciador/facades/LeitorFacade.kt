package com.biblioteca.gerenciador.facades

import com.biblioteca.gerenciador.domain.Perfil
import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.exceptions.NegocioException
import com.biblioteca.gerenciador.mappers.aplicarEm
import com.biblioteca.gerenciador.mappers.toEntity
import com.biblioteca.gerenciador.mappers.toLeitorResponse
import com.biblioteca.gerenciador.rest.requests.AcessoLeitorRequest
import com.biblioteca.gerenciador.rest.requests.LeitorRequest
import com.biblioteca.gerenciador.rest.responses.LeitorResponse
import com.biblioteca.gerenciador.rest.responses.PageResponse
import com.biblioteca.gerenciador.services.EmprestimoService
import com.biblioteca.gerenciador.services.PerfilService
import com.biblioteca.gerenciador.services.Relogio
import com.biblioteca.gerenciador.services.UsuarioService
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Facade: ponto de entrada dos controllers.
 * Coordena os services, converte DTO <-> entidade e define a transação.
 */
@Component
class LeitorFacade(
    private val usuarioService: UsuarioService,
    private val perfilService: PerfilService,
    private val emprestimoService: EmprestimoService,
    private val relogio: Relogio,
) {

    @Transactional(readOnly = true)
    fun pesquisar(busca: String?, pageable: Pageable): PageResponse<LeitorResponse> =
        PageResponse.de(usuarioService.pesquisarLeitores(busca, pageable)) { responder(it) }

    @Transactional(readOnly = true)
    fun buscar(id: Long): LeitorResponse = responder(usuarioService.buscarLeitor(id))

    @Transactional
    fun criar(request: LeitorRequest): LeitorResponse {
        val perfilLeitor = perfilService.buscarPorNome(Perfil.LEITOR)
        return responder(usuarioService.salvar(request.toEntity(perfilLeitor)))
    }

    @Transactional
    fun atualizar(id: Long, request: LeitorRequest): LeitorResponse {
        val leitor = usuarioService.buscarLeitor(id)
        request.aplicarEm(leitor)
        return responder(usuarioService.salvar(leitor))
    }

    @Transactional
    fun habilitarAcesso(id: Long, request: AcessoLeitorRequest): LeitorResponse {
        val leitor = usuarioService.buscarLeitor(id)
        return responder(usuarioService.definirSenha(leitor, request.senha))
    }

    /** RN13: não inativa leitor com exemplar ainda não devolvido. */
    @Transactional
    fun inativar(id: Long) {
        val leitor = usuarioService.buscarLeitor(id)
        if (emprestimoService.possuiItemPendente(leitor)) {
            throw NegocioException("READER_HAS_ACTIVE_LOANS", "O leitor possui exemplares não devolvidos e não pode ser inativado.")
        }
        usuarioService.inativar(leitor)
    }

    private fun responder(leitor: Usuario) = leitor.toLeitorResponse(relogio.hoje())
}

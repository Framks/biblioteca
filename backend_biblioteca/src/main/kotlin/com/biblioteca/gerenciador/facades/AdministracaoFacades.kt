package com.biblioteca.gerenciador.facades

import com.biblioteca.gerenciador.domain.Perfil
import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.enums.AcaoAuditoria
import com.biblioteca.gerenciador.enums.MotivoRevogacao
import com.biblioteca.gerenciador.exceptions.NegocioException
import com.biblioteca.gerenciador.mappers.semBrancos
import com.biblioteca.gerenciador.mappers.toResponse
import com.biblioteca.gerenciador.mappers.toUsuarioResponse
import com.biblioteca.gerenciador.rest.requests.AtualizarUsuarioRequest
import com.biblioteca.gerenciador.rest.requests.CriarUsuarioRequest
import com.biblioteca.gerenciador.rest.requests.PermissoesPerfilRequest
import com.biblioteca.gerenciador.rest.requests.RedefinirSenhaRequest
import com.biblioteca.gerenciador.rest.responses.AuditoriaResponse
import com.biblioteca.gerenciador.rest.responses.PageResponse
import com.biblioteca.gerenciador.rest.responses.PerfilResponse
import com.biblioteca.gerenciador.rest.responses.PermissaoResponse
import com.biblioteca.gerenciador.rest.responses.SessaoResponse
import com.biblioteca.gerenciador.rest.responses.UsuarioResponse
import com.biblioteca.gerenciador.services.AuditoriaService
import com.biblioteca.gerenciador.services.PerfilService
import com.biblioteca.gerenciador.services.Relogio
import com.biblioteca.gerenciador.services.SessaoService
import com.biblioteca.gerenciador.services.UsuarioLogado
import com.biblioteca.gerenciador.services.UsuarioService
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate

/** Funcionários e administradores (RF17). */
@Component
class UsuarioFacade(
    private val usuarioService: UsuarioService,
    private val perfilService: PerfilService,
    private val sessaoService: SessaoService,
    private val auditoria: AuditoriaService,
    private val usuarioLogado: UsuarioLogado,
    private val relogio: Relogio,
) {

    @Transactional(readOnly = true)
    fun pesquisar(busca: String?, pageable: Pageable): PageResponse<UsuarioResponse> {
        val agora = relogio.agora()
        return PageResponse.de(usuarioService.pesquisarEquipe(busca, pageable)) { it.toUsuarioResponse(agora) }
    }

    @Transactional(readOnly = true)
    fun buscar(id: Long): UsuarioResponse = usuarioService.buscarMembroEquipe(id).toUsuarioResponse(relogio.agora())

    @Transactional
    fun criar(request: CriarUsuarioRequest): UsuarioResponse {
        val usuario = Usuario(
            nome = request.nome.trim(),
            perfil = perfilService.buscarPorNome(request.perfil),
            email = request.email.trim().lowercase(),
            telefone = request.telefone.semBrancos(),
        )
        usuarioService.salvar(usuario)
        usuarioService.definirSenha(usuario, request.senha)
        auditoria.registrar(AcaoAuditoria.USUARIO_CRIADO, "Usuario", usuario.id, "perfil ${request.perfil}")
        return usuario.toUsuarioResponse(relogio.agora())
    }

    @Transactional
    fun atualizar(id: Long, request: AtualizarUsuarioRequest): UsuarioResponse {
        val usuario = usuarioService.buscarMembroEquipe(id)
        if (usuario.perfil.nome == Perfil.ADMIN && request.perfil != Perfil.ADMIN) exigirOutroAdministrador()
        usuario.atualizarDados(
            nome = request.nome.trim(),
            email = request.email.trim().lowercase(),
            tipoDocumento = usuario.tipoDocumento,
            documento = usuario.documento,
            telefone = request.telefone.semBrancos(),
            endereco = usuario.endereco,
        )
        usuario.alterarPerfil(perfilService.buscarPorNome(request.perfil))
        usuarioService.salvar(usuario)
        auditoria.registrar(AcaoAuditoria.USUARIO_ATUALIZADO, "Usuario", usuario.id, "perfil ${request.perfil}")
        return usuario.toUsuarioResponse(relogio.agora())
    }

    /** Define nova senha e derruba todas as sessões do usuário. */
    @Transactional
    fun redefinirSenha(id: Long, request: RedefinirSenhaRequest) {
        val usuario = usuarioService.buscarMembroEquipe(id)
        usuarioService.definirSenha(usuario, request.senha)
        usuario.registrarLoginComSucesso() // desbloqueia, se estava bloqueado
        sessaoService.revogarTodas(usuario, MotivoRevogacao.SENHA_ALTERADA, relogio.agora())
        auditoria.registrar(AcaoAuditoria.USUARIO_SENHA_REDEFINIDA, "Usuario", usuario.id)
    }

    @Transactional
    fun inativar(id: Long) {
        val usuario = usuarioService.buscarMembroEquipe(id)
        if (usuario.id == usuarioLogado.autenticado().usuarioId) {
            throw NegocioException("CANNOT_DEACTIVATE_SELF", "Você não pode inativar o seu próprio usuário.")
        }
        if (usuario.perfil.nome == Perfil.ADMIN) exigirOutroAdministrador()
        usuarioService.inativar(usuario)
        sessaoService.revogarTodas(usuario, MotivoRevogacao.USUARIO_INATIVADO, relogio.agora())
        auditoria.registrar(AcaoAuditoria.USUARIO_INATIVADO, "Usuario", usuario.id)
    }

    @Transactional(readOnly = true)
    fun sessoes(id: Long): List<SessaoResponse> =
        sessaoService.listarAtivas(usuarioService.buscarMembroEquipe(id), relogio.agora()).map { it.toResponse(null) }

    /** Sempre precisa sobrar pelo menos um administrador ativo. */
    private fun exigirOutroAdministrador() {
        if (usuarioService.contarAdministradoresAtivos() <= 1) {
            throw NegocioException("LAST_ADMIN", "Este é o único administrador ativo; cadastre outro antes.")
        }
    }
}

/** Perfis e permissões (RF18). */
@Component
class PerfilFacade(
    private val perfilService: PerfilService,
    private val auditoria: AuditoriaService,
) {

    @Transactional(readOnly = true)
    fun listar(): List<PerfilResponse> = perfilService.listar().map { it.toResponse() }

    @Transactional(readOnly = true)
    fun listarPermissoes(): List<PermissaoResponse> = perfilService.listarPermissoes().map { it.toResponse() }

    /** Vale na próxima requisição de quem tem o perfil: as permissões são lidas do banco a cada requisição. */
    @Transactional
    fun atualizarPermissoes(id: Long, request: PermissoesPerfilRequest): PerfilResponse {
        val perfil = perfilService.atualizarPermissoes(perfilService.buscar(id), request.permissoes)
        auditoria.registrar(AcaoAuditoria.PERFIL_PERMISSOES_ALTERADAS, "Perfil", perfil.id, request.permissoes.sorted().joinToString())
        return perfil.toResponse()
    }
}

/** Consulta da auditoria (RF21). */
@Component
class AuditoriaFacade(
    private val auditoria: AuditoriaService,
    private val relogio: Relogio,
) {

    @Transactional(readOnly = true)
    fun pesquisar(acao: AcaoAuditoria?, usuarioId: Long?, de: LocalDate?, ate: LocalDate?, pageable: Pageable): PageResponse<AuditoriaResponse> {
        val inicio = de?.atStartOfDay(relogio.zona)?.toInstant() ?: Instant.EPOCH
        val fim = ate?.plusDays(1)?.atStartOfDay(relogio.zona)?.toInstant() ?: Instant.parse("9999-12-31T00:00:00Z")
        return PageResponse.de(auditoria.pesquisar(acao, usuarioId, inicio, fim, pageable)) { it.toResponse() }
    }
}

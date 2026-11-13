package com.biblioteca.gerenciador.facades

import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.enums.AcaoAuditoria
import com.biblioteca.gerenciador.enums.MotivoRevogacao
import com.biblioteca.gerenciador.exceptions.ApiException
import com.biblioteca.gerenciador.exceptions.ContaBloqueadaException
import com.biblioteca.gerenciador.exceptions.NaoAutenticadoException
import com.biblioteca.gerenciador.exceptions.NaoEncontradoException
import com.biblioteca.gerenciador.exceptions.NegocioException
import com.biblioteca.gerenciador.mappers.toResponse
import com.biblioteca.gerenciador.mappers.toUsuarioLogadoResponse
import com.biblioteca.gerenciador.rest.requests.AlterarSenhaRequest
import com.biblioteca.gerenciador.rest.requests.LoginRequest
import com.biblioteca.gerenciador.rest.responses.LoginResponse
import com.biblioteca.gerenciador.rest.responses.SessaoResponse
import com.biblioteca.gerenciador.rest.responses.UsuarioLogadoResponse
import com.biblioteca.gerenciador.security.Segredos
import com.biblioteca.gerenciador.security.TokenService
import com.biblioteca.gerenciador.services.AuditoriaService
import com.biblioteca.gerenciador.services.ConfiguracaoService
import com.biblioteca.gerenciador.services.Relogio
import com.biblioteca.gerenciador.services.SessaoService
import com.biblioteca.gerenciador.services.UsuarioLogado
import com.biblioteca.gerenciador.services.UsuarioService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Login, renovação de token, logout e sessões (RF15, RF16, RF20, seção 6).
 *
 * login e refresh usam noRollbackFor: ao recusar a requisição, o que foi registrado antes
 * (tentativa errada, bloqueio da conta, revogação da sessão, auditoria) precisa continuar gravado.
 */
@Component
class AuthFacade(
    private val usuarioService: UsuarioService,
    private val sessaoService: SessaoService,
    private val tokenService: TokenService,
    private val configuracao: ConfiguracaoService,
    private val auditoria: AuditoriaService,
    private val usuarioLogado: UsuarioLogado,
    private val relogio: Relogio,
) {

    @Transactional(noRollbackFor = [ApiException::class])
    fun login(request: LoginRequest, ip: String, userAgent: String, deviceIdAtual: String?): ResultadoAutenticacao {
        val agora = relogio.agora()
        val usuario = usuarioService.buscarPorEmail(request.email)
            ?.takeIf { it.ativo && it.acessoHabilitado }
            ?: run {
                auditoria.registrar(AcaoAuditoria.LOGIN_FALHA, detalhes = "e-mail: ${request.email.take(255)}", usuario = null)
                throw credenciaisInvalidas()
            }

        if (usuario.estaBloqueado(agora)) {
            auditoria.registrar(AcaoAuditoria.LOGIN_CONTA_BLOQUEADA, usuario = usuario)
            throw contaBloqueada(usuario)
        }
        if (!usuarioService.senhaConfere(usuario, request.senha)) {
            val bloqueou = usuario.registrarFalhaLogin(
                maxTentativas = configuracao.inteiro(MAX_TENTATIVAS),
                minutosBloqueio = configuracao.inteiro(MINUTOS_BLOQUEIO),
                agora = agora,
            )
            auditoria.registrar(AcaoAuditoria.LOGIN_FALHA, usuario = usuario)
            if (bloqueou) auditoria.registrar(AcaoAuditoria.CONTA_BLOQUEADA, detalhes = "até ${usuario.bloqueadoAte}", usuario = usuario)
            throw credenciaisInvalidas()
        }

        usuario.registrarLoginComSucesso()
        // Reaproveita o device_id do navegador, se já houver; senão cria um.
        val deviceId = deviceIdAtual?.takeIf { it.length in 20..100 } ?: Segredos.gerar()
        val aberta = sessaoService.abrir(usuario, ip, userAgent, deviceId, agora)
        auditoria.registrar(AcaoAuditoria.LOGIN_SUCESSO, "Sessao", null, "sessão ${aberta.sessao.id}", usuario)
        return autenticado(usuario, aberta.sessao.id, aberta.refreshToken, aberta.sessao.expiraEm, deviceId, agora)
    }

    @Transactional(noRollbackFor = [ApiException::class])
    fun renovar(refreshToken: String?, deviceId: String?, userAgent: String): ResultadoAutenticacao {
        if (refreshToken == null) throw NaoAutenticadoException("INVALID_REFRESH_TOKEN", "Sessão expirada. Faça login novamente.")
        val agora = relogio.agora()
        val aberta = sessaoService.rotacionar(refreshToken, deviceId, userAgent, agora)
        val usuario = aberta.sessao.usuario
        return autenticado(usuario, aberta.sessao.id, aberta.refreshToken, aberta.sessao.expiraEm, deviceId!!, agora)
    }

    /** RF16: encerra a sessão e coloca o access token atual na blocklist até ele expirar. */
    @Transactional
    fun logout() {
        val atual = usuarioLogado.autenticado()
        val agora = relogio.agora()
        sessaoService.revogar(sessaoService.buscar(atual.sessaoId), MotivoRevogacao.LOGOUT, agora)
        sessaoService.bloquearToken(atual.jti, atual.tokenExpiraEm, MotivoRevogacao.LOGOUT)
        auditoria.registrar(AcaoAuditoria.LOGOUT)
    }

    @Transactional(readOnly = true)
    fun eu(): UsuarioLogadoResponse = usuarioLogado.obter().toUsuarioLogadoResponse()

    @Transactional(readOnly = true)
    fun minhasSessoes(): List<SessaoResponse> {
        val atual = usuarioLogado.autenticado().sessaoId
        return sessaoService.listarAtivas(usuarioLogado.obter(), relogio.agora()).map { it.toResponse(atual) }
    }

    /** RF20: encerra uma sessão própria; com session_manage, de qualquer usuário. */
    @Transactional
    fun encerrarSessao(id: UUID) {
        val atual = usuarioLogado.autenticado()
        val sessao = sessaoService.buscar(id)
        val propria = sessao.usuario.id == atual.usuarioId
        if (!propria && !atual.possui("session_manage")) {
            throw NaoEncontradoException("SESSION_NOT_FOUND", "Sessão não encontrada.") // não revela sessões alheias
        }
        val agora = relogio.agora()
        sessaoService.revogar(sessao, if (propria) MotivoRevogacao.ENCERRADA_PELO_USUARIO else MotivoRevogacao.ENCERRADA_PELO_ADMIN, agora)
        if (sessao.id == atual.sessaoId) sessaoService.bloquearToken(atual.jti, atual.tokenExpiraEm, MotivoRevogacao.ENCERRADA_PELO_USUARIO)
    }

    /** Troca a própria senha e encerra as demais sessões (quem tinha a senha antiga perde o acesso). */
    @Transactional
    fun alterarSenha(request: AlterarSenhaRequest) {
        val usuario = usuarioLogado.obter()
        if (!usuarioService.senhaConfere(usuario, request.senhaAtual)) {
            throw NegocioException("CURRENT_PASSWORD_MISMATCH", "A senha atual não confere.")
        }
        usuarioService.definirSenha(usuario, request.novaSenha)
        sessaoService.revogarTodas(usuario, MotivoRevogacao.SENHA_ALTERADA, relogio.agora(), excetoSessaoId = usuarioLogado.autenticado().sessaoId)
        auditoria.registrar(AcaoAuditoria.SENHA_ALTERADA, "Usuario", usuario.id)
    }

    private fun autenticado(
        usuario: Usuario,
        sessaoId: UUID,
        refreshToken: String,
        refreshExpiraEm: Instant,
        deviceId: String,
        agora: Instant,
    ): ResultadoAutenticacao {
        val access = tokenService.emitir(usuario, sessaoId)
        return ResultadoAutenticacao(
            resposta = LoginResponse(
                accessToken = access.valor,
                expiraEm = access.expiraEm,
                expiresIn = Duration.between(agora, access.expiraEm).seconds,
                usuario = usuario.toUsuarioLogadoResponse(),
            ),
            refreshToken = refreshToken,
            refreshExpiraEm = refreshExpiraEm,
            deviceId = deviceId,
        )
    }

    /** Mesma mensagem para e-mail inexistente e senha errada: não revela quais e-mails estão cadastrados (6.1). */
    private fun credenciaisInvalidas() = NaoAutenticadoException("INVALID_CREDENTIALS", "E-mail ou senha inválidos.")

    private fun contaBloqueada(usuario: Usuario) = ContaBloqueadaException(
        "Muitas tentativas de login erradas. Tente novamente após ${usuario.bloqueadoAte?.atZone(relogio.zona)?.toLocalTime()?.withNano(0)}.",
    )

    /** O controller usa para escrever os cookies; a [resposta] vai no corpo. */
    data class ResultadoAutenticacao(
        val resposta: LoginResponse,
        val refreshToken: String,
        val refreshExpiraEm: Instant,
        val deviceId: String,
    )

    private companion object {
        const val MAX_TENTATIVAS = "login.max_tentativas"
        const val MINUTOS_BLOQUEIO = "login.bloqueio_minutos"
    }
}

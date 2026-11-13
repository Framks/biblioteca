package com.biblioteca.gerenciador.services

import com.biblioteca.gerenciador.domain.RefreshToken
import com.biblioteca.gerenciador.domain.Sessao
import com.biblioteca.gerenciador.domain.TokenRevogado
import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.enums.AcaoAuditoria
import com.biblioteca.gerenciador.enums.MotivoRevogacao
import com.biblioteca.gerenciador.exceptions.ApiException
import com.biblioteca.gerenciador.exceptions.NaoAutenticadoException
import com.biblioteca.gerenciador.exceptions.NaoEncontradoException
import com.biblioteca.gerenciador.repositories.RefreshTokenRepository
import com.biblioteca.gerenciador.repositories.SessaoRepository
import com.biblioteca.gerenciador.repositories.TokenRevogadoRepository
import com.biblioteca.gerenciador.security.Segredos
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Sessões, refresh tokens e blocklist (seção 6).
 *
 * Exceção à regra "a facade define a transação": este service também é chamado pelo filtro de segurança,
 * que roda antes de qualquer facade. Os métodos que revogam a sessão e depois recusam a requisição usam
 * noRollbackFor, senão o rollback desfaria a própria revogação.
 */
@Service
class SessaoService(
    private val sessaoRepository: SessaoRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val tokenRevogadoRepository: TokenRevogadoRepository,
    private val configuracao: ConfiguracaoService,
    private val auditoria: AuditoriaService,
) {

    /** Abre uma sessão no login, presa ao [deviceId] e ao [userAgent] de quem logou. */
    @Transactional
    fun abrir(usuario: Usuario, ip: String, userAgent: String, deviceId: String, agora: Instant): SessaoAberta {
        val sessao = sessaoRepository.save(
            Sessao(
                id = UUID.randomUUID(),
                usuario = usuario,
                deviceIdHash = Segredos.sha256(deviceId),
                ip = ip,
                userAgent = userAgent.take(500),
                criadoEm = agora,
                expiraEm = agora.plus(Duration.ofDays(configuracao.inteiro(REFRESH_DIAS).toLong())),
            ),
        )
        return SessaoAberta(sessao, emitirRefreshToken(sessao, agora))
    }

    /**
     * 6.3: troca o refresh token por um novo (rotação). Se um token já trocado for apresentado de novo,
     * alguém tem uma cópia: a sessão inteira é revogada.
     */
    @Transactional(noRollbackFor = [ApiException::class])
    fun rotacionar(refreshToken: String, deviceId: String?, userAgent: String, agora: Instant): SessaoAberta {
        val atual = refreshTokenRepository.findByTokenHash(Segredos.sha256(refreshToken))
            ?: throw NaoAutenticadoException("INVALID_REFRESH_TOKEN", "Sessão expirada. Faça login novamente.")
        val sessao = atual.sessao

        if (atual.jaUsado) {
            revogar(sessao, MotivoRevogacao.REUSO_REFRESH_TOKEN, agora)
            auditoria.registrar(AcaoAuditoria.REFRESH_TOKEN_REUSADO, "Sessao", null, "sessão ${sessao.id}", sessao.usuario)
            throw NaoAutenticadoException(
                "REFRESH_TOKEN_REUSED",
                "Reuso de refresh token detectado; a sessão foi encerrada por segurança. Faça login novamente.",
            )
        }
        if (!sessao.ativa(agora) || !atual.expiraEm.isAfter(agora)) {
            throw NaoAutenticadoException("SESSION_REVOKED", "Sessão encerrada. Faça login novamente.")
        }
        exigirMesmoDispositivo(sessao, deviceId, userAgent, agora)

        atual.marcarUsado(agora)
        return SessaoAberta(sessao, emitirRefreshToken(sessao, agora))
    }

    /**
     * 6.5: chamado pelo filtro em toda requisição autenticada. A sessão precisa estar ativa e ser usada
     * do mesmo dispositivo e navegador do login; senão é revogada na hora.
     * Mudança só de IP (ex.: Wi-Fi para 4G) é permitida e fica registrada na auditoria.
     */
    @Transactional(noRollbackFor = [ApiException::class])
    fun validarRequisicao(sessaoId: UUID, deviceId: String?, userAgent: String, ip: String, agora: Instant): DadosSessao {
        val sessao = sessaoRepository.findByIdOrNull(sessaoId)?.takeIf { it.ativa(agora) }
            ?: throw NaoAutenticadoException("SESSION_REVOKED", "Sessão encerrada. Faça login novamente.")
        exigirMesmoDispositivo(sessao, deviceId, userAgent, agora)

        val usuario = sessao.usuario
        if (!usuario.ativo) {
            revogar(sessao, MotivoRevogacao.USUARIO_INATIVADO, agora)
            throw NaoAutenticadoException("SESSION_REVOKED", "Usuário inativo.")
        }
        if (ip != sessao.ip) {
            auditoria.registrar(AcaoAuditoria.IP_ALTERADO, "Sessao", null, "sessão ${sessao.id}: ${sessao.ip} -> $ip", usuario)
            sessao.ip = ip
        }
        sessao.registrarAcesso(agora)
        return DadosSessao(
            usuarioId = usuario.id!!,
            perfil = usuario.perfil.nome,
            permissoes = usuario.perfil.permissoes.map { it.codigo }.toSet(),
        )
    }

    @Transactional
    fun revogar(sessao: Sessao, motivo: MotivoRevogacao, agora: Instant) {
        if (sessao.revogadaEm != null) return
        sessao.revogar(motivo, agora)
        sessaoRepository.save(sessao)
        auditoria.registrar(AcaoAuditoria.SESSAO_REVOGADA, "Sessao", null, "sessão ${sessao.id}: $motivo", sessao.usuario)
    }

    /** Encerra todas as sessões do usuário, exceto [excetoSessaoId] (ex.: a atual, ao trocar a própria senha). */
    @Transactional
    fun revogarTodas(usuario: Usuario, motivo: MotivoRevogacao, agora: Instant, excetoSessaoId: UUID? = null) {
        listarAtivas(usuario, agora).filter { it.id != excetoSessaoId }.forEach { revogar(it, motivo, agora) }
    }

    fun listarAtivas(usuario: Usuario, agora: Instant): List<Sessao> =
        sessaoRepository.findByUsuarioAndRevogadaEmIsNullAndExpiraEmAfterOrderByCriadoEmDesc(usuario, agora)

    fun buscar(id: UUID): Sessao =
        sessaoRepository.findByIdOrNull(id) ?: throw NaoEncontradoException("SESSION_NOT_FOUND", "Sessão não encontrada.")

    /** 6.4: impede o uso do access token até ele expirar (ex.: após o logout). */
    @Transactional
    fun bloquearToken(jti: String, expiraEm: Instant, motivo: MotivoRevogacao) {
        if (!tokenRevogadoRepository.existsById(jti)) tokenRevogadoRepository.save(TokenRevogado(jti, expiraEm, motivo.name))
    }

    fun tokenBloqueado(jti: String): Boolean = tokenRevogadoRepository.existsById(jti)

    /** Limpeza periódica: tokens vencidos não precisam mais ser guardados. */
    @Transactional
    fun limparExpirados(agora: Instant): Int =
        tokenRevogadoRepository.apagarExpirados(agora) + refreshTokenRepository.apagarExpirados(agora)

    private fun exigirMesmoDispositivo(sessao: Sessao, deviceId: String?, userAgent: String, agora: Instant) {
        val motivo = when {
            deviceId == null || Segredos.sha256(deviceId) != sessao.deviceIdHash -> MotivoRevogacao.DISPOSITIVO_DIFERENTE
            userAgent.take(500) != sessao.userAgent -> MotivoRevogacao.NAVEGADOR_DIFERENTE
            else -> return
        }
        revogar(sessao, motivo, agora)
        throw NaoAutenticadoException(
            "SESSION_REVOKED",
            "Uso da sessão em outro dispositivo ou navegador detectado; a sessão foi encerrada por segurança.",
        )
    }

    private fun emitirRefreshToken(sessao: Sessao, agora: Instant): String {
        val valor = Segredos.gerar()
        refreshTokenRepository.save(
            RefreshToken(sessao = sessao, tokenHash = Segredos.sha256(valor), emitidoEm = agora, expiraEm = sessao.expiraEm),
        )
        return valor
    }

    data class SessaoAberta(val sessao: Sessao, val refreshToken: String)

    data class DadosSessao(val usuarioId: Long, val perfil: String, val permissoes: Set<String>)

    companion object {
        private const val REFRESH_DIAS = "jwt.refresh_dias"
    }
}

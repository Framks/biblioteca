package com.biblioteca.gerenciador.domain

import com.biblioteca.gerenciador.enums.MotivoRevogacao
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Duration
import java.time.Instant
import java.util.UUID

/** Sessão de login (seção 6.5): vinculada ao dispositivo e ao navegador de quem fez o login. */
@Entity
@Table(name = "sessao")
class Sessao(
    @Id
    val id: UUID,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    val usuario: Usuario,

    /** SHA-256 do cookie device_id. */
    val deviceIdHash: String,
    var ip: String,
    val userAgent: String,
    val criadoEm: Instant,
    val expiraEm: Instant,
) {
    var ultimoAcesso: Instant = criadoEm
        protected set

    var revogadaEm: Instant? = null
        protected set

    var motivoRevogacao: String? = null
        protected set

    fun ativa(agora: Instant): Boolean = revogadaEm == null && agora.isBefore(expiraEm)

    fun revogar(motivo: MotivoRevogacao, agora: Instant) {
        if (revogadaEm != null) return
        revogadaEm = agora
        motivoRevogacao = motivo.name
    }

    /** Atualiza o último acesso no máximo uma vez por minuto (evita uma escrita no banco a cada requisição). */
    fun registrarAcesso(agora: Instant) {
        if (Duration.between(ultimoAcesso, agora) > Duration.ofMinutes(1)) ultimoAcesso = agora
    }

    override fun equals(other: Any?) = other is Sessao && other.id == id

    override fun hashCode() = id.hashCode()
}

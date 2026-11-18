package com.biblioteca.gerenciador.domain

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

/** Refresh token rotativo (seção 6.3). Só o hash é gravado; o valor fica no cookie HttpOnly do cliente. */
@Entity
@Table(name = "refresh_token")
class RefreshToken(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sessao_id")
    val sessao: Sessao,

    val tokenHash: String,
    val emitidoEm: Instant,
    val expiraEm: Instant,
) : EntidadeBase() {

    var usadoEm: Instant? = null
        protected set

    val jaUsado: Boolean
        get() = usadoEm != null

    fun marcarUsado(agora: Instant) {
        usadoEm = agora
    }
}

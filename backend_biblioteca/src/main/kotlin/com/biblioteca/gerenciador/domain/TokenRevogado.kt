package com.biblioteca.gerenciador.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/** Access token revogado antes de expirar (seção 6.4). */
@Entity
@Table(name = "token_blocklist")
class TokenRevogado(
    @Id
    val jti: String,
    val expiraEm: Instant,
    val motivo: String,
) {
    override fun equals(other: Any?) = other is TokenRevogado && other.jti == jti

    override fun hashCode() = jti.hashCode()
}

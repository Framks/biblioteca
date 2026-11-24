package com.biblioteca.gerenciador.domain

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

/** Registro de auditoria (RF21): quem fez o quê, quando e de onde. */
@Entity
@Table(name = "auditoria")
class Auditoria(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    val usuario: Usuario?,

    val acao: String,
    val entidade: String?,
    val entidadeId: Long?,
    val detalhes: String?,
    val ip: String?,
    val dataHora: Instant,
) : EntidadeBase()

package com.biblioteca.gerenciador.domain

import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

/** Toda entidade tem id gerado pelo banco. Igualdade é pelo id (padrão seguro para JPA). */
@MappedSuperclass
abstract class EntidadeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as EntidadeBase
        return id != null && id == other.id
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
}

/** Entidade com datas de cadastro e atualização preenchidas automaticamente. */
@MappedSuperclass
abstract class EntidadeAuditada : EntidadeBase() {

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    var dataCadastro: Instant? = null

    @UpdateTimestamp
    @Column(nullable = false)
    var dataAtualizacao: Instant? = null
}

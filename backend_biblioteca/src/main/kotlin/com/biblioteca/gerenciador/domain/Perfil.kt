package com.biblioteca.gerenciador.domain

import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table

/** Agrupa permissões; cada usuário tem um perfil (seção 7). */
@Entity
@Table(name = "perfil")
class Perfil(
    var nome: String,
    var descricao: String? = null,

    @ManyToMany
    @JoinTable(
        name = "perfil_permissao",
        joinColumns = [JoinColumn(name = "perfil_id")],
        inverseJoinColumns = [JoinColumn(name = "permissao_id")],
    )
    var permissoes: MutableSet<Permissao> = mutableSetOf(),
) : EntidadeBase() {

    fun possui(codigoPermissao: String): Boolean = permissoes.any { it.codigo == codigoPermissao }

    companion object {
        // Perfis criados pela migration V3.
        const val VISITANTE = "VISITANTE"
        const val LEITOR = "LEITOR"
        const val FUNCIONARIO = "FUNCIONARIO"
        const val ADMIN = "ADMIN"
    }
}

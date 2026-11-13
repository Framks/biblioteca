package com.biblioteca.gerenciador.domain

import com.biblioteca.gerenciador.enums.StatusRegistro
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/** Obra do acervo (RF01). Os exemplares físicos ficam em Exemplar. */
@Entity
@Table(name = "livro")
class Livro(
    var titulo: String,
    var autor: String,
    var isbn: String? = null,
    var editora: String? = null,
    var anoPublicacao: Int? = null,

    @ManyToMany
    @JoinTable(
        name = "livro_genero",
        joinColumns = [JoinColumn(name = "livro_id")],
        inverseJoinColumns = [JoinColumn(name = "genero_id")],
    )
    var generos: MutableSet<Genero> = mutableSetOf(),

    /** Quem cadastrou (auditoria simples). Nulo para livros anteriores à migration V3. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por")
    val criadoPor: Usuario? = null,
) : EntidadeAuditada() {

    @Enumerated(EnumType.STRING)
    var status: StatusRegistro = StatusRegistro.ATIVO
        protected set

    fun atualizar(
        titulo: String,
        autor: String,
        isbn: String?,
        editora: String?,
        anoPublicacao: Int?,
        generos: Set<Genero>,
    ) {
        this.titulo = titulo
        this.autor = autor
        this.isbn = isbn
        this.editora = editora
        this.anoPublicacao = anoPublicacao
        this.generos.clear()
        this.generos.addAll(generos)
    }

    /** Exclusão lógica (RN12). A checagem de empréstimo ativo (RN13) entra junto com Exemplar. */
    fun inativar() {
        status = StatusRegistro.INATIVO
    }
}

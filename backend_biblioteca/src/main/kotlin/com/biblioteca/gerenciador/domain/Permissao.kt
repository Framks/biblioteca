package com.biblioteca.gerenciador.domain

import jakarta.persistence.Entity
import jakarta.persistence.Table

/** Ação que pode ser autorizada, no formato recurso_acao (ex.: book_create). */
@Entity
@Table(name = "permissao")
class Permissao(
    var codigo: String,
    var descricao: String,
) : EntidadeBase()

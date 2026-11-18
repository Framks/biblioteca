package com.biblioteca.gerenciador.domain

import com.biblioteca.gerenciador.enums.FiltroDisponibilidade

/** Critérios da pesquisa do acervo (RF04). Campos nulos não filtram. */
data class FiltroLivro(
    val titulo: String? = null,
    val autor: String? = null,
    val isbn: String? = null,
    val generoId: Long? = null,
    val disponibilidade: FiltroDisponibilidade? = null,
)

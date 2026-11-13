package com.biblioteca.gerenciador.rest.responses

/** Visão pública de um livro (RF05): sem dados internos nem de leitores. */
data class CatalogoLivroResponse(
    val id: Long,
    val titulo: String,
    val autor: String,
    val editora: String?,
    val anoPublicacao: Int?,
    val generos: List<String>,
    val exemplaresDisponiveis: Int,
    val disponivel: Boolean,
)

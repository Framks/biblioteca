package com.biblioteca.gerenciador.enums

/** Filtro da consulta ao acervo (RF04). */
enum class FiltroDisponibilidade {
    /** Livros com ao menos um exemplar disponível. */
    DISPONIVEL,

    /** Livros sem nenhum exemplar disponível. */
    INDISPONIVEL,

    /** Livros com ao menos um exemplar emprestado. */
    EMPRESTADO,
}

package com.biblioteca.gerenciador.rest.responses

import java.time.Instant

data class LivroResponse(
    val id: Long,
    val isbn: String?,
    val titulo: String,
    val autor: String,
    val editora: String?,
    val anoPublicacao: Int?,
    val generos: List<GeneroResponse>,
    /** Exemplares em circulação (exclui os baixados). */
    val totalExemplares: Int,
    val exemplaresDisponiveis: Int,
    val exemplaresEmprestados: Int,
    /** Nome de quem cadastrou o livro. */
    val cadastradoPor: String?,
    val dataCadastro: Instant,
    val dataAtualizacao: Instant,
)

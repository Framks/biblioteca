package com.biblioteca.gerenciador.rest.responses

import java.time.Instant

/** Formato único de erro da API (docs/objetivo.md, seção 10.2). */
data class ErroResponse(
    val status: Int,
    val code: String,
    val message: String,
    val fields: List<CampoErro> = emptyList(),
    val timestamp: Instant,
    val path: String,
)

data class CampoErro(
    val field: String,
    val message: String,
)

package com.biblioteca.gerenciador.mappers

import com.biblioteca.gerenciador.domain.Genero
import com.biblioteca.gerenciador.rest.requests.GeneroRequest
import com.biblioteca.gerenciador.rest.responses.GeneroResponse

fun GeneroRequest.toEntity() = Genero(nome = nome.trim())

fun Genero.toResponse() = GeneroResponse(
    id = id!!,
    nome = nome,
)

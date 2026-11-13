package com.biblioteca.gerenciador.rest.requests

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class LivroRequest(
    @field:NotBlank(message = "é obrigatório")
    @field:Size(max = 255, message = "deve ter no máximo 255 caracteres")
    val titulo: String = "",

    @field:NotBlank(message = "é obrigatório")
    @field:Size(max = 255, message = "deve ter no máximo 255 caracteres")
    val autor: String = "",

    @field:Size(max = 20, message = "deve ter no máximo 20 caracteres")
    val isbn: String? = null,

    @field:Size(max = 255, message = "deve ter no máximo 255 caracteres")
    val editora: String? = null,

    @field:Positive(message = "deve ser positivo")
    val anoPublicacao: Int? = null,

    @field:NotEmpty(message = "informe ao menos um gênero")
    val generoIds: Set<Long> = emptySet(),
)

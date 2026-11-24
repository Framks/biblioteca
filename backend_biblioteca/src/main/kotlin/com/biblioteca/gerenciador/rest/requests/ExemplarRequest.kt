package com.biblioteca.gerenciador.rest.requests

import com.biblioteca.gerenciador.enums.CondicaoExemplar
import com.biblioteca.gerenciador.enums.StatusExemplar
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/** POST /books/{livroId}/copies: adiciona um ou mais exemplares com código gerado. */
data class NovosExemplaresRequest(
    @field:Min(value = 1, message = "deve ser no mínimo 1")
    @field:Max(value = 50, message = "deve ser no máximo 50")
    val quantidade: Int = 1,

    val condicao: CondicaoExemplar = CondicaoExemplar.NOVO,
)

/** PATCH /copies/{id}: só os campos enviados são alterados. */
data class AtualizarExemplarRequest(
    @field:Size(max = 30, message = "deve ter no máximo 30 caracteres")
    @field:Pattern(regexp = ".*\\S.*", message = "não pode ser vazio")
    val codigo: String? = null,

    val condicao: CondicaoExemplar? = null,

    val status: StatusExemplar? = null,
)

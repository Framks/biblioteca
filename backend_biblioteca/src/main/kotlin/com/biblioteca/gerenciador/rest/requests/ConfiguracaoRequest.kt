package com.biblioteca.gerenciador.rest.requests

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Positive

/** PUT /settings/{chave}: todas as configurações atuais são números inteiros positivos. */
data class ConfiguracaoRequest(
    @field:Positive(message = "deve ser maior que zero")
    @field:Max(value = 10_000, message = "deve ser no máximo 10000")
    val valor: Int = 0,
)

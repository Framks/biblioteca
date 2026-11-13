package com.biblioteca.gerenciador.rest.requests

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/*
 * Convenção dos requests: campos obrigatórios têm valor padrão vazio ("" / emptySet()).
 * Assim, um campo ausente no JSON vira erro de validação com o nome do campo (400 VALIDATION_ERROR),
 * em vez de um erro genérico de desserialização.
 */
data class GeneroRequest(
    @field:NotBlank(message = "é obrigatório")
    @field:Size(max = 100, message = "deve ter no máximo 100 caracteres")
    val nome: String = "",
)

package com.biblioteca.gerenciador.rest.requests

import com.biblioteca.gerenciador.enums.TipoDocumento
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@DocumentoValido
data class LeitorRequest(
    @field:NotBlank(message = "é obrigatório")
    @field:Size(max = 255, message = "deve ter no máximo 255 caracteres")
    val nome: String = "",

    val tipoDocumento: TipoDocumento = TipoDocumento.CPF,

    /** Aceita com ou sem pontuação: "529.982.247-25" ou "52998224725". */
    @field:NotBlank(message = "é obrigatório")
    @field:Size(max = 20, message = "deve ter no máximo 20 caracteres")
    val documento: String = "",

    @field:Email(message = "e-mail inválido")
    @field:Size(max = 255, message = "deve ter no máximo 255 caracteres")
    val email: String? = null,

    @field:Size(max = 20, message = "deve ter no máximo 20 caracteres")
    val telefone: String? = null,

    @field:Size(max = 500, message = "deve ter no máximo 500 caracteres")
    val endereco: String? = null,
)

/** POST /readers/{id}/access: habilita (ou redefine) o login do leitor (RF08). */
data class AcessoLeitorRequest(
    @field:NotBlank(message = "é obrigatória")
    @field:Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres")
    val senha: String = "",
)

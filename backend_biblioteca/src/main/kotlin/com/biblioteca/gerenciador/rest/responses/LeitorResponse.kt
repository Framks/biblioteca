package com.biblioteca.gerenciador.rest.responses

import com.biblioteca.gerenciador.enums.TipoDocumento
import java.time.Instant
import java.time.LocalDate

data class LeitorResponse(
    val id: Long,
    val nome: String,
    val tipoDocumento: TipoDocumento?,
    /** Normalizado: só dígitos (CPF) ou letras e dígitos (RG). A formatação fica com o frontend. */
    val documento: String?,
    val email: String?,
    val telefone: String?,
    val endereco: String?,
    /** Pode fazer login no sistema (RF08). */
    val acessoHabilitado: Boolean,
    /** Data final da suspensão por atraso (RN07); nulo se nunca foi suspenso. */
    val suspensoAte: LocalDate?,
    val suspenso: Boolean,
    val dataCadastro: Instant,
    val dataAtualizacao: Instant,
)

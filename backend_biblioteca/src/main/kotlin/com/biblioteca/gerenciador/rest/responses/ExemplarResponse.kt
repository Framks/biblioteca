package com.biblioteca.gerenciador.rest.responses

import com.biblioteca.gerenciador.enums.CondicaoExemplar
import com.biblioteca.gerenciador.enums.StatusExemplar
import java.time.Instant

data class ExemplarResponse(
    val id: Long,
    val codigo: String,
    val livroId: Long,
    val condicao: CondicaoExemplar,
    val status: StatusExemplar,
    val dataCadastro: Instant,
    val dataAtualizacao: Instant,
)

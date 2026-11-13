package com.biblioteca.gerenciador.rest.requests

import com.biblioteca.gerenciador.enums.CondicaoExemplar
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

/** POST /loans (RF09). Ids obrigatórios usam 0 como "não informado" (falha no @Positive). */
data class EmprestimoRequest(
    @field:Positive(message = "é obrigatório")
    val leitorId: Long = 0,

    @field:NotEmpty(message = "informe ao menos um exemplar")
    @field:Size(max = 10, message = "no máximo 10 exemplares por empréstimo")
    val exemplarIds: Set<Long> = emptySet(),

    /** Opcional: se ausente, usa o prazo padrão (RN02). */
    @field:Positive(message = "deve ser maior que zero")
    val prazoDias: Int? = null,

    @field:Size(max = 500, message = "deve ter no máximo 500 caracteres")
    val observacao: String? = null,
)

/** POST /loans/{id}/return (RF10): um ou mais itens do empréstimo. */
data class DevolucaoRequest(
    @field:NotEmpty(message = "informe ao menos um item")
    @field:Valid
    val itens: List<ItemDevolucaoRequest> = emptyList(),
)

data class ItemDevolucaoRequest(
    @field:Positive(message = "é obrigatório")
    val itemId: Long = 0,

    /** Condição do exemplar ao ser devolvido (RN08). */
    @field:NotNull(message = "é obrigatória")
    val condicao: CondicaoExemplar? = null,
)

package com.biblioteca.gerenciador.rest

import com.biblioteca.gerenciador.exceptions.RequisicaoInvalidaException
import org.springframework.data.domain.Pageable

fun Pageable.validarOrdenacao(vararg permitidos: String): Pageable {
    sort.firstOrNull { it.property !in permitidos }?.let {
        throw RequisicaoInvalidaException(
            "INVALID_SORT",
            "Não é possível ordenar por '${it.property}'. Permitidos: ${permitidos.joinToString()}.",
        )
    }
    return this
}

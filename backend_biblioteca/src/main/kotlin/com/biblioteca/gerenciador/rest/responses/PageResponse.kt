package com.biblioteca.gerenciador.rest.responses

import org.springframework.data.domain.Page

/** Formato único de listagem paginada (docs/objetivo.md, seção 10.3). */
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun <E : Any, T> de(pagina: Page<E>, converter: (E) -> T) = PageResponse(
            content = pagina.content.map(converter),
            page = pagina.number,
            size = pagina.size,
            totalElements = pagina.totalElements,
            totalPages = pagina.totalPages,
        )
    }
}

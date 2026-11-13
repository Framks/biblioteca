package com.biblioteca.gerenciador.mappers

/** "  " e "" viram null; o restante é aparado. Usado nos campos opcionais dos requests. */
internal fun String?.semBrancos(): String? = this?.trim()?.ifBlank { null }

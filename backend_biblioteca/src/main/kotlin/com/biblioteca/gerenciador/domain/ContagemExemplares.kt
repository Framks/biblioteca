package com.biblioteca.gerenciador.domain

/** Quantidades calculadas a partir dos exemplares de um livro (não armazenadas, ver seção 8.1). */
data class ContagemExemplares(
    /** Exemplares em circulação (todos exceto os baixados). */
    val total: Int = 0,
    val disponiveis: Int = 0,
    val emprestados: Int = 0,
) {
    companion object {
        val VAZIA = ContagemExemplares()
    }
}

package com.biblioteca.gerenciador.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/** Parâmetro de negócio ajustável sem alterar código (RF19). */
@Entity
@Table(name = "configuracao")
class Configuracao(
    @Id
    val chave: String,
    var valor: String,
    val descricao: String,
) {
    companion object {
        // Chaves criadas pela migration V4.
        const val MAX_ITENS_POR_LEITOR = "emprestimo.max_itens_por_leitor"
        const val PRAZO_PADRAO_DIAS = "emprestimo.prazo_padrao_dias"
        const val PRAZO_MAXIMO_DIAS = "emprestimo.prazo_maximo_dias"
        const val MAX_RENOVACOES = "emprestimo.max_renovacoes"
        const val DIAS_SUSPENSAO_POR_DIA_ATRASO = "suspensao.dias_por_dia_atraso"
    }
}

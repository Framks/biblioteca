package com.biblioteca.gerenciador.rest.responses

import java.time.LocalDate

/** GET /reports/summary (RF13). */
data class ResumoCirculacaoResponse(
    val dataReferencia: LocalDate,
    /** Empréstimos com algum exemplar ainda não devolvido (inclui os atrasados). */
    val emprestimosAtivos: Long,
    val emprestimosAtrasados: Long,
    val emprestimosNoPrazo: Long,
    val exemplaresEmprestados: Long,
    val exemplaresAtrasados: Long,
    val leitoresComAtraso: Long,
    val leitoresSuspensos: Long,
    val emprestimosNoMes: Long,
)

/** GET /reports/loans-per-month: um item por mês, no formato "2026-11". */
data class EmprestimosPorMesResponse(
    val mes: String,
    val quantidade: Long,
)

/** GET /reports/top-books */
data class LivroMaisEmprestadoResponse(
    val posicao: Int,
    val livroId: Long,
    val titulo: String,
    val autor: String,
    val emprestimos: Long,
)

/** GET /reports/overdue-readers */
data class LeitorEmAtrasoResponse(
    val leitorId: Long,
    val nome: String,
    val email: String?,
    val telefone: String?,
    val exemplaresAtrasados: Int,
    val maiorAtrasoDias: Long,
    val vencidoDesde: LocalDate,
    val titulos: List<String>,
)

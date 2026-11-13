package com.biblioteca.gerenciador.facades

import com.biblioteca.gerenciador.mappers.toResponse
import com.biblioteca.gerenciador.rest.responses.EmprestimosPorMesResponse
import com.biblioteca.gerenciador.rest.responses.LeitorEmAtrasoResponse
import com.biblioteca.gerenciador.rest.responses.LivroMaisEmprestadoResponse
import com.biblioteca.gerenciador.rest.responses.ResumoCirculacaoResponse
import com.biblioteca.gerenciador.services.Relogio
import com.biblioteca.gerenciador.services.RelatorioService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth

@Component
@Transactional(readOnly = true)
class RelatorioFacade(
    private val relatorioService: RelatorioService,
    private val relogio: Relogio,
) {

    fun resumo(): ResumoCirculacaoResponse {
        val hoje = relogio.hoje()
        return relatorioService.resumo(hoje, relogio.zona).toResponse(hoje)
    }

    /** Sem período, retorna os últimos 12 meses (incluindo o atual). */
    fun porMes(inicio: YearMonth?, fim: YearMonth?): List<EmprestimosPorMesResponse> {
        val ate = fim ?: YearMonth.from(relogio.hoje())
        val de = inicio ?: ate.minusMonths(11)
        return relatorioService.porMes(de, ate, relogio.zona).map { it.toResponse() }
    }

    fun maisEmprestados(de: LocalDate?, ate: LocalDate?, limite: Int): List<LivroMaisEmprestadoResponse> =
        relatorioService.maisEmprestados(de, ate, limite, relogio.zona).toResponse()

    fun leitoresEmAtraso(): List<LeitorEmAtrasoResponse> =
        relatorioService.leitoresEmAtraso(relogio.hoje()).map { it.toResponse() }
}

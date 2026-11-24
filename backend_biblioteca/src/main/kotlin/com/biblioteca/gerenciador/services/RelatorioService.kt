package com.biblioteca.gerenciador.services

import com.biblioteca.gerenciador.domain.EmprestimoItem
import com.biblioteca.gerenciador.domain.Perfil
import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.enums.StatusEmprestimo
import com.biblioteca.gerenciador.enums.StatusRegistro
import com.biblioteca.gerenciador.exceptions.RequisicaoInvalidaException
import com.biblioteca.gerenciador.repositories.EmprestimoItemRepository
import com.biblioteca.gerenciador.repositories.EmprestimoRepository
import com.biblioteca.gerenciador.repositories.UsuarioRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Relatórios de circulação (RF13). Somente leitura. */
@Service
class RelatorioService(
    private val emprestimoRepository: EmprestimoRepository,
    private val itemRepository: EmprestimoItemRepository,
    private val usuarioRepository: UsuarioRepository,
) {

    fun resumo(hoje: LocalDate, zona: ZoneId): ResumoCirculacao {
        val abertos = emprestimoRepository.countByStatus(StatusEmprestimo.ABERTO)
        val atrasados = itemRepository.contarEmprestimosAtrasados(hoje)
        val mesAtual = YearMonth.from(hoje)
        return ResumoCirculacao(
            emprestimosAtivos = abertos,
            emprestimosAtrasados = atrasados,
            emprestimosNoPrazo = abertos - atrasados,
            exemplaresEmprestados = itemRepository.countByDataDevolucaoRealIsNull(),
            exemplaresAtrasados = itemRepository.countByDataDevolucaoRealIsNullAndDataDevolucaoPrevistaBefore(hoje),
            leitoresComAtraso = itemRepository.itensAtrasados(hoje).map { it.emprestimo.leitor.id }.distinct().size.toLong(),
            leitoresSuspensos = usuarioRepository.countByPerfilNomeAndStatusAndSuspensoAteGreaterThanEqual(
                Perfil.LEITOR, StatusRegistro.ATIVO, hoje,
            ),
            emprestimosNoMes = emprestimoRepository.datasEntre(inicioDoMes(mesAtual, zona), inicioDoMes(mesAtual.plusMonths(1), zona)).size.toLong(),
        )
    }

    /** Quantidade de empréstimos realizados por mês, de [inicio] a [fim] (inclusive). Meses sem empréstimo aparecem com 0. */
    fun porMes(inicio: YearMonth, fim: YearMonth, zona: ZoneId): List<Pair<YearMonth, Long>> {
        if (inicio.isAfter(fim)) throw RequisicaoInvalidaException("INVALID_PERIOD", "O mês inicial deve ser anterior ou igual ao final.")
        if (inicio.plusMonths(MAX_MESES.toLong() - 1).isBefore(fim)) {
            throw RequisicaoInvalidaException("INVALID_PERIOD", "O período pode ter no máximo $MAX_MESES meses.")
        }
        val contagem = emprestimoRepository.datasEntre(inicioDoMes(inicio, zona), inicioDoMes(fim.plusMonths(1), zona))
            .groupingBy { YearMonth.from(it.atZone(zona)) }
            .eachCount()
        return generateSequence(inicio) { it.plusMonths(1) }
            .takeWhile { !it.isAfter(fim) }
            .map { it to (contagem[it] ?: 0).toLong() }
            .toList()
    }

    /** Livros mais emprestados entre [de] e [ate] (datas inclusive); sem datas, considera todo o histórico. */
    fun maisEmprestados(de: LocalDate?, ate: LocalDate?, limite: Int, zona: ZoneId): List<EmprestimoItemRepository.LivroMaisEmprestado> {
        if (de != null && ate != null && de.isAfter(ate)) {
            throw RequisicaoInvalidaException("INVALID_PERIOD", "A data inicial deve ser anterior ou igual à final.")
        }
        val inicio = de?.atStartOfDay(zona)?.toInstant() ?: Instant.EPOCH
        val fim = ate?.plusDays(1)?.atStartOfDay(zona)?.toInstant() ?: FIM_DOS_TEMPOS
        return itemRepository.maisEmprestados(inicio, fim, PageRequest.of(0, limite))
    }

    /** Leitores com itens em atraso, do maior atraso para o menor. */
    fun leitoresEmAtraso(hoje: LocalDate): List<LeitorEmAtraso> =
        itemRepository.itensAtrasados(hoje)
            .groupBy { it.emprestimo.leitor }
            .map { (leitor, itens) -> LeitorEmAtraso(leitor, itens, hoje) }
            .sortedWith(compareByDescending<LeitorEmAtraso> { it.maiorAtrasoDias }.thenBy { it.leitor.nome })

    private fun inicioDoMes(mes: YearMonth, zona: ZoneId): Instant = mes.atDay(1).atStartOfDay(zona).toInstant()

    data class ResumoCirculacao(
        val emprestimosAtivos: Long,
        val emprestimosAtrasados: Long,
        val emprestimosNoPrazo: Long,
        val exemplaresEmprestados: Long,
        val exemplaresAtrasados: Long,
        val leitoresComAtraso: Long,
        val leitoresSuspensos: Long,
        val emprestimosNoMes: Long,
    )

    class LeitorEmAtraso(val leitor: Usuario, val itens: List<EmprestimoItem>, hoje: LocalDate) {
        val maiorAtrasoDias: Long = itens.maxOf { it.diasAtraso(hoje) }
        val vencidoDesde: LocalDate = itens.minOf { it.dataDevolucaoPrevista }
    }

    companion object {
        const val MAX_MESES = 36
        private val FIM_DOS_TEMPOS: Instant = Instant.parse("9999-12-31T00:00:00Z")
    }
}

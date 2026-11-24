package com.biblioteca.gerenciador.domain

import com.biblioteca.gerenciador.enums.CondicaoExemplar
import com.biblioteca.gerenciador.enums.SituacaoEmprestimo
import com.biblioteca.gerenciador.exceptions.NegocioException
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Um exemplar dentro de um empréstimo. Prazo, renovação e devolução são controlados por item (RN09). */
@Entity
@Table(name = "emprestimo_item")
class EmprestimoItem(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emprestimo_id")
    val emprestimo: Emprestimo,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exemplar_id")
    val exemplar: Exemplar,

    dataDevolucaoPrevista: LocalDate,

    @Enumerated(EnumType.STRING)
    val condicaoRetirada: CondicaoExemplar,
) : EntidadeBase() {

    var dataDevolucaoPrevista: LocalDate = dataDevolucaoPrevista
        protected set

    var dataDevolucaoReal: Instant? = null
        protected set

    var renovacoes: Int = 0
        protected set

    @Enumerated(EnumType.STRING)
    var condicaoDevolucao: CondicaoExemplar? = null
        protected set

    /** Funcionário que recebeu a devolução. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recebido_por")
    var recebidoPor: Usuario? = null
        protected set

    val devolvido: Boolean
        get() = dataDevolucaoReal != null

    /** RN06: atrasado = não devolvido e a data prevista já passou (no dia previsto ainda está no prazo). */
    fun estaAtrasado(hoje: LocalDate): Boolean = !devolvido && dataDevolucaoPrevista.isBefore(hoje)

    /** Dias de atraso de um item ainda não devolvido; 0 se estiver no prazo. */
    fun diasAtraso(hoje: LocalDate): Long =
        if (estaAtrasado(hoje)) ChronoUnit.DAYS.between(dataDevolucaoPrevista, hoje) else 0

    fun situacao(hoje: LocalDate): SituacaoEmprestimo = when {
        devolvido -> SituacaoEmprestimo.DEVOLVIDO
        estaAtrasado(hoje) -> SituacaoEmprestimo.ATRASADO
        else -> SituacaoEmprestimo.ATIVO
    }

    /** RN08: registra a condição na devolução e devolve o exemplar à circulação (ou à manutenção). */
    fun devolver(condicao: CondicaoExemplar, recebidoPor: Usuario, hoje: LocalDate, agora: Instant): Long {
        exigirNaoDevolvido()
        val atraso = diasAtraso(hoje)
        dataDevolucaoReal = agora
        condicaoDevolucao = condicao
        this.recebidoPor = recebidoPor
        exemplar.devolver(condicao)
        return atraso
    }

    /**
     * RN10/RN11: renova por mais [prazoDias] a partir de hoje (nunca encurta o prazo atual).
     * Não renova item atrasado nem acima do limite de renovações.
     */
    fun renovar(hoje: LocalDate, prazoDias: Int, maxRenovacoes: Int) {
        exigirNaoDevolvido()
        if (estaAtrasado(hoje)) {
            throw NegocioException("OVERDUE_ITEM_CANNOT_BE_RENEWED", "Item atrasado não pode ser renovado; registre a devolução.")
        }
        if (renovacoes >= maxRenovacoes) {
            throw NegocioException("RENEWAL_LIMIT_REACHED", "Este item já foi renovado $renovacoes vez(es), o máximo permitido.")
        }
        renovacoes++
        dataDevolucaoPrevista = maxOf(dataDevolucaoPrevista, hoje.plusDays(prazoDias.toLong()))
    }

    private fun exigirNaoDevolvido() {
        if (devolvido) throw NegocioException("ITEM_ALREADY_RETURNED", "O exemplar ${exemplar.codigo} já foi devolvido.")
    }
}

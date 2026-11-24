package com.biblioteca.gerenciador.domain

import com.biblioteca.gerenciador.enums.CondicaoExemplar
import com.biblioteca.gerenciador.enums.SituacaoEmprestimo
import com.biblioteca.gerenciador.enums.StatusEmprestimo
import com.biblioteca.gerenciador.exceptions.NaoEncontradoException
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

/** Um atendimento de empréstimo: um leitor leva um ou mais exemplares (RF09). */
@Entity
@Table(name = "emprestimo")
class Emprestimo(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leitor_id")
    val leitor: Usuario,

    /** Funcionário que registrou. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operador_id")
    val operador: Usuario,

    val dataEmprestimo: Instant,

    var observacao: String? = null,
) : EntidadeBase() {

    @Enumerated(EnumType.STRING)
    var status: StatusEmprestimo = StatusEmprestimo.ABERTO
        protected set

    @OneToMany(mappedBy = "emprestimo", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("id")
    val itens: MutableList<EmprestimoItem> = mutableListOf()

    /** Empresta o exemplar (RN03 é conferida pelo próprio exemplar) e o inclui neste empréstimo. */
    fun adicionarItem(exemplar: Exemplar, dataDevolucaoPrevista: LocalDate): EmprestimoItem {
        exemplar.emprestar()
        val item = EmprestimoItem(
            emprestimo = this,
            exemplar = exemplar,
            dataDevolucaoPrevista = dataDevolucaoPrevista,
            condicaoRetirada = exemplar.condicao,
        )
        itens.add(item)
        return item
    }

    /** Devolve um item; quando todos forem devolvidos, o empréstimo é encerrado (RN09). Retorna os dias de atraso. */
    fun devolver(itemId: Long, condicao: CondicaoExemplar, recebidoPor: Usuario, hoje: LocalDate, agora: Instant): Long {
        val item = itens.firstOrNull { it.id == itemId }
            ?: throw NaoEncontradoException("LOAN_ITEM_NOT_FOUND", "O item $itemId não pertence ao empréstimo $id.")
        val diasAtraso = item.devolver(condicao, recebidoPor, hoje, agora)
        if (itens.all { it.devolvido }) status = StatusEmprestimo.ENCERRADO
        return diasAtraso
    }

    fun situacao(hoje: LocalDate): SituacaoEmprestimo = when {
        status == StatusEmprestimo.ENCERRADO -> SituacaoEmprestimo.DEVOLVIDO
        itens.any { it.estaAtrasado(hoje) } -> SituacaoEmprestimo.ATRASADO
        else -> SituacaoEmprestimo.ATIVO
    }
}

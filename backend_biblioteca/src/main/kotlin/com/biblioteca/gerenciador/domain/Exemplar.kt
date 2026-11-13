package com.biblioteca.gerenciador.domain

import com.biblioteca.gerenciador.enums.CondicaoExemplar
import com.biblioteca.gerenciador.enums.StatusExemplar
import com.biblioteca.gerenciador.enums.StatusRegistro
import com.biblioteca.gerenciador.exceptions.NegocioException
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version

/** Cópia física de um livro, com código único e rastreável (RF03). */
@Entity
@Table(name = "exemplar")
class Exemplar(
    var codigo: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "livro_id")
    val livro: Livro,

    condicao: CondicaoExemplar = CondicaoExemplar.NOVO,
) : EntidadeAuditada() {

    @Enumerated(EnumType.STRING)
    var condicao: CondicaoExemplar = condicao
        protected set

    /** Exemplar que já chega danificado começa em manutenção. */
    @Enumerated(EnumType.STRING)
    var status: StatusExemplar =
        if (condicao == CondicaoExemplar.DANIFICADO) StatusExemplar.MANUTENCAO else StatusExemplar.DISPONIVEL
        protected set

    /**
     * Controle de concorrência otimista: se dois atendentes emprestarem o mesmo exemplar ao mesmo tempo,
     * o segundo a gravar recebe erro (409 CONCURRENT_UPDATE) em vez de duplicar o empréstimo.
     */
    @Version
    var versao: Long = 0
        protected set

    /** RN03: só exemplares disponíveis, de livros ativos, podem ser emprestados. */
    fun exigirDisponivelParaEmprestimo() {
        if (status != StatusExemplar.DISPONIVEL || livro.status != StatusRegistro.ATIVO) {
            throw NegocioException("COPY_NOT_AVAILABLE", "O exemplar $codigo não está disponível (status: $status).")
        }
    }

    fun emprestar() {
        exigirDisponivelParaEmprestimo()
        mudarStatus(StatusExemplar.EMPRESTADO)
    }

    /** RN08: na devolução, o exemplar danificado vai para manutenção; os demais voltam a ficar disponíveis. */
    fun devolver(condicaoDevolucao: CondicaoExemplar) {
        condicao = condicaoDevolucao
        mudarStatus(
            if (condicaoDevolucao == CondicaoExemplar.DANIFICADO) StatusExemplar.MANUTENCAO else StatusExemplar.DISPONIVEL,
        )
    }

    /**
     * Alteração manual feita pelo funcionário (PATCH).
     * O status EMPRESTADO só é alterado pelo fluxo de empréstimo/devolução.
     */
    fun atualizar(novoCodigo: String?, novaCondicao: CondicaoExemplar?, novoStatus: StatusExemplar?) {
        if (status == StatusExemplar.EMPRESTADO) {
            throw NegocioException("COPY_ON_LOAN", "O exemplar $codigo está emprestado; registre a devolução antes de alterá-lo.")
        }
        if (novoStatus == StatusExemplar.EMPRESTADO) {
            throw NegocioException("INVALID_STATUS_TRANSITION", "Use o registro de empréstimo para emprestar um exemplar.")
        }
        novoCodigo?.let { codigo = it.trim() }
        novaCondicao?.let { condicao = it }
        if (novoStatus != null && novoStatus != status) mudarStatus(novoStatus)
        validarConsistencia()
    }

    private fun mudarStatus(novo: StatusExemplar) {
        if (!status.podeMudarPara(novo)) {
            throw NegocioException("INVALID_STATUS_TRANSITION", "Não é possível mudar o exemplar $codigo de $status para $novo.")
        }
        status = novo
    }

    /** Exemplar danificado não pode ficar disponível para empréstimo. */
    private fun validarConsistencia() {
        if (condicao == CondicaoExemplar.DANIFICADO && status == StatusExemplar.DISPONIVEL) {
            throw NegocioException(
                "DAMAGED_COPY_UNAVAILABLE",
                "Exemplar danificado não pode ficar disponível; envie para manutenção ou dê baixa.",
            )
        }
    }
}

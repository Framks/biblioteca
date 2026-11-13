package com.biblioteca.gerenciador.rest.responses

import com.biblioteca.gerenciador.enums.CondicaoExemplar
import com.biblioteca.gerenciador.enums.SituacaoEmprestimo
import com.biblioteca.gerenciador.enums.StatusEmprestimo
import java.time.Instant
import java.time.LocalDate

data class EmprestimoResponse(
    val id: Long,
    val leitor: PessoaResumoResponse,
    /** Funcionário que registrou. */
    val operador: PessoaResumoResponse,
    val dataEmprestimo: Instant,
    val status: StatusEmprestimo,
    /** Calculada: ATRASADO se qualquer item estiver atrasado (RN06). */
    val situacao: SituacaoEmprestimo,
    val observacao: String?,
    val itens: List<ItemEmprestimoResponse>,
)

data class ItemEmprestimoResponse(
    val id: Long,
    val exemplarId: Long,
    val codigoExemplar: String,
    val livroId: Long,
    val tituloLivro: String,
    val dataDevolucaoPrevista: LocalDate,
    val dataDevolucaoReal: Instant?,
    val situacao: SituacaoEmprestimo,
    /** Dias de atraso hoje (0 se no prazo ou já devolvido). */
    val diasAtraso: Long,
    val renovacoes: Int,
    val renovacoesRestantes: Int,
    val condicaoRetirada: CondicaoExemplar,
    val condicaoDevolucao: CondicaoExemplar?,
)

data class PessoaResumoResponse(
    val id: Long,
    val nome: String,
)

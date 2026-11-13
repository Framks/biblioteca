package com.biblioteca.gerenciador.services

import com.biblioteca.gerenciador.domain.Configuracao
import com.biblioteca.gerenciador.domain.Emprestimo
import com.biblioteca.gerenciador.domain.EmprestimoItem
import com.biblioteca.gerenciador.domain.Exemplar
import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.enums.CondicaoExemplar
import com.biblioteca.gerenciador.enums.SituacaoEmprestimo
import com.biblioteca.gerenciador.enums.StatusEmprestimo
import com.biblioteca.gerenciador.exceptions.NaoEncontradoException
import com.biblioteca.gerenciador.exceptions.NegocioException
import com.biblioteca.gerenciador.repositories.EmprestimoItemRepository
import com.biblioteca.gerenciador.repositories.EmprestimoRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate

/** Regras de circulação: empréstimo, devolução e renovação (RN01–RN11). Não conhece DTOs. */
@Service
class EmprestimoService(
    private val repository: EmprestimoRepository,
    private val itemRepository: EmprestimoItemRepository,
    private val configuracao: ConfiguracaoService,
) {

    fun buscar(id: Long): Emprestimo =
        repository.findByIdOrNull(id) ?: throw NaoEncontradoException("LOAN_NOT_FOUND", "Empréstimo $id não encontrado.")

    fun buscarItem(itemId: Long): EmprestimoItem =
        itemRepository.findByIdOrNull(itemId)
            ?: throw NaoEncontradoException("LOAN_ITEM_NOT_FOUND", "Item de empréstimo $itemId não encontrado.")

    fun pesquisar(
        leitorId: Long?,
        livroId: Long?,
        situacao: SituacaoEmprestimo?,
        hoje: LocalDate,
        pageable: Pageable,
    ): Page<Emprestimo> {
        val status = when (situacao) {
            SituacaoEmprestimo.DEVOLVIDO -> StatusEmprestimo.ENCERRADO
            SituacaoEmprestimo.ATIVO, SituacaoEmprestimo.ATRASADO -> StatusEmprestimo.ABERTO
            null -> null
        }
        val comAtraso = when (situacao) {
            SituacaoEmprestimo.ATRASADO -> true
            SituacaoEmprestimo.ATIVO -> false
            else -> null
        }
        return repository.pesquisar(leitorId, livroId, status, comAtraso, hoje, pageable)
    }

    fun possuiItemPendente(leitor: Usuario): Boolean = itemRepository.existsByEmprestimoLeitorAndDataDevolucaoRealIsNull(leitor)

    /** RF09: valida as regras e registra o empréstimo de [exemplares] para o [leitor]. */
    fun registrar(
        leitor: Usuario,
        operador: Usuario,
        exemplares: List<Exemplar>,
        prazoDias: Int?,
        observacao: String?,
        hoje: LocalDate,
        agora: Instant,
    ): Emprestimo {
        val prazo = validarPrazo(prazoDias)
        val pendentes = itemRepository.findByEmprestimoLeitorAndDataDevolucaoRealIsNull(leitor)
        validarLeitorApto(leitor, pendentes, hoje)
        validarLimite(pendentes, exemplares)
        validarMesmoLivro(pendentes, exemplares)
        exemplares.forEach { it.exigirDisponivelParaEmprestimo() } // RN03: confere todos antes de alterar qualquer um

        val emprestimo = Emprestimo(leitor = leitor, operador = operador, dataEmprestimo = agora, observacao = observacao)
        val dataPrevista = hoje.plusDays(prazo.toLong())
        exemplares.forEach { emprestimo.adicionarItem(it, dataPrevista) }
        return repository.save(emprestimo)
    }

    /**
     * RF10: devolve os itens informados. Se houver atraso, suspende o leitor (RN07)
     * pelo maior atraso entre os itens devolvidos, multiplicado pelo fator configurado.
     */
    fun devolver(
        emprestimo: Emprestimo,
        devolucoes: List<Pair<Long, CondicaoExemplar>>,
        recebidoPor: Usuario,
        hoje: LocalDate,
        agora: Instant,
    ): Emprestimo {
        val maiorAtraso = devolucoes.maxOf { (itemId, condicao) ->
            emprestimo.devolver(itemId, condicao, recebidoPor, hoje, agora)
        }
        if (maiorAtraso > 0) {
            val fator = configuracao.inteiro(Configuracao.DIAS_SUSPENSAO_POR_DIA_ATRASO)
            emprestimo.leitor.suspenderPor(maiorAtraso * fator, hoje)
        }
        return repository.save(emprestimo)
    }

    /** RF11: renova o prazo de um item (RN10, RN11) se o leitor estiver apto (RN05). */
    fun renovar(item: EmprestimoItem, hoje: LocalDate): EmprestimoItem {
        item.renovar(
            hoje = hoje,
            prazoDias = configuracao.inteiro(Configuracao.PRAZO_PADRAO_DIAS),
            maxRenovacoes = configuracao.inteiro(Configuracao.MAX_RENOVACOES),
        )
        val leitor = item.emprestimo.leitor
        validarLeitorApto(leitor, itemRepository.findByEmprestimoLeitorAndDataDevolucaoRealIsNull(leitor), hoje)
        return itemRepository.save(item)
    }

    fun maxRenovacoes(): Int = configuracao.inteiro(Configuracao.MAX_RENOVACOES)

    /** RN02: prazo padrão, ou o informado, até o máximo configurado. */
    private fun validarPrazo(prazoDias: Int?): Int {
        val prazo = prazoDias ?: configuracao.inteiro(Configuracao.PRAZO_PADRAO_DIAS)
        val maximo = configuracao.inteiro(Configuracao.PRAZO_MAXIMO_DIAS)
        if (prazo > maximo) throw NegocioException("LOAN_TERM_EXCEEDED", "O prazo máximo de empréstimo é de $maximo dias.")
        return prazo
    }

    /** RN05: leitor suspenso ou com item em atraso não pode pegar nem renovar. */
    private fun validarLeitorApto(leitor: Usuario, pendentes: List<EmprestimoItem>, hoje: LocalDate) {
        if (leitor.estaSuspenso(hoje)) {
            throw NegocioException("READER_SUSPENDED", "Leitor suspenso até ${leitor.suspensoAte} por atraso em devolução.")
        }
        if (pendentes.any { it.estaAtrasado(hoje) }) {
            throw NegocioException("READER_HAS_OVERDUE_LOANS", "O leitor possui exemplares em atraso; registre a devolução primeiro.")
        }
    }

    /** RN01: limite de exemplares emprestados ao mesmo tempo. */
    private fun validarLimite(pendentes: List<EmprestimoItem>, novos: List<Exemplar>) {
        val maximo = configuracao.inteiro(Configuracao.MAX_ITENS_POR_LEITOR)
        if (pendentes.size + novos.size > maximo) {
            throw NegocioException(
                "LOAN_LIMIT_EXCEEDED",
                "O leitor pode ter no máximo $maximo exemplares emprestados (já possui ${pendentes.size}).",
            )
        }
    }

    /** RN04: um leitor não pode ter dois exemplares do mesmo livro ao mesmo tempo. */
    private fun validarMesmoLivro(pendentes: List<EmprestimoItem>, novos: List<Exemplar>) {
        val livros = pendentes.map { it.exemplar.livro } + novos.map { it.livro }
        val repetido = livros.groupBy { it.id }.values.firstOrNull { it.size > 1 }?.first() ?: return
        throw NegocioException(
            "SAME_BOOK_TWICE",
            "O leitor não pode ter dois exemplares de '${repetido.titulo}' ao mesmo tempo.",
        )
    }
}

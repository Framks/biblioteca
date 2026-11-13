package com.biblioteca.gerenciador.facades

import com.biblioteca.gerenciador.domain.Emprestimo
import com.biblioteca.gerenciador.enums.AcaoAuditoria
import com.biblioteca.gerenciador.enums.SituacaoEmprestimo
import com.biblioteca.gerenciador.exceptions.ProibidoException
import com.biblioteca.gerenciador.mappers.semBrancos
import com.biblioteca.gerenciador.mappers.toDevolucoes
import com.biblioteca.gerenciador.mappers.toResponse
import com.biblioteca.gerenciador.rest.requests.DevolucaoRequest
import com.biblioteca.gerenciador.rest.requests.EmprestimoRequest
import com.biblioteca.gerenciador.rest.responses.EmprestimoResponse
import com.biblioteca.gerenciador.rest.responses.PageResponse
import com.biblioteca.gerenciador.services.AuditoriaService
import com.biblioteca.gerenciador.services.EmprestimoService
import com.biblioteca.gerenciador.services.ExemplarService
import com.biblioteca.gerenciador.services.Relogio
import com.biblioteca.gerenciador.services.UsuarioLogado
import com.biblioteca.gerenciador.services.UsuarioService
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Facade: ponto de entrada dos controllers.
 * Aqui fica a orquestração de vários services: leitor + exemplares + operador + regras de empréstimo,
 * tudo numa única transação (se uma regra falhar, nada é gravado).
 */
@Component
class EmprestimoFacade(
    private val emprestimoService: EmprestimoService,
    private val usuarioService: UsuarioService,
    private val exemplarService: ExemplarService,
    private val usuarioLogado: UsuarioLogado,
    private val relogio: Relogio,
    private val auditoria: AuditoriaService,
) {

    @Transactional(readOnly = true)
    fun pesquisar(leitorId: Long?, livroId: Long?, situacao: SituacaoEmprestimo?, pageable: Pageable): PageResponse<EmprestimoResponse> {
        val hoje = relogio.hoje()
        val maxRenovacoes = emprestimoService.maxRenovacoes()
        return PageResponse.de(emprestimoService.pesquisar(leitorId, livroId, situacao, hoje, pageable)) {
            it.toResponse(hoje, maxRenovacoes)
        }
    }

    /** RF07: histórico de um leitor. */
    @Transactional(readOnly = true)
    fun historicoDoLeitor(leitorId: Long, situacao: SituacaoEmprestimo?, pageable: Pageable): PageResponse<EmprestimoResponse> {
        usuarioService.buscarLeitor(leitorId) // 404 se não for leitor ativo
        return pesquisar(leitorId, null, situacao, pageable)
    }

    /** Empréstimos do próprio leitor logado. */
    @Transactional(readOnly = true)
    fun meus(situacao: SituacaoEmprestimo?, pageable: Pageable): PageResponse<EmprestimoResponse> =
        pesquisar(usuarioLogado.autenticado().usuarioId, null, situacao, pageable)

    @Transactional(readOnly = true)
    fun buscar(id: Long): EmprestimoResponse = responder(emprestimoService.buscar(id))

    @Transactional
    fun registrar(request: EmprestimoRequest): EmprestimoResponse {
        val leitor = usuarioService.buscarLeitor(request.leitorId)
        val exemplares = exemplarService.buscarTodos(request.exemplarIds)
        val emprestimo = emprestimoService.registrar(
            leitor = leitor,
            operador = usuarioLogado.obter(),
            exemplares = exemplares,
            prazoDias = request.prazoDias,
            observacao = request.observacao.semBrancos(),
            hoje = relogio.hoje(),
            agora = relogio.agora(),
        )
        auditoria.registrar(
            AcaoAuditoria.EMPRESTIMO_REGISTRADO, "Emprestimo", emprestimo.id,
            "leitor ${leitor.id}; exemplares ${exemplares.joinToString { it.codigo }}",
        )
        return responder(emprestimo)
    }

    @Transactional
    fun devolver(id: Long, request: DevolucaoRequest): EmprestimoResponse {
        val emprestimo = emprestimoService.devolver(
            emprestimo = emprestimoService.buscar(id),
            devolucoes = request.toDevolucoes(),
            recebidoPor = usuarioLogado.obter(),
            hoje = relogio.hoje(),
            agora = relogio.agora(),
        )
        auditoria.registrar(
            AcaoAuditoria.DEVOLUCAO_REGISTRADA, "Emprestimo", emprestimo.id,
            request.itens.joinToString { "item ${it.itemId}: ${it.condicao}" },
        )
        return responder(emprestimo)
    }

    @Transactional
    fun renovar(itemId: Long): EmprestimoResponse {
        val item = emprestimoService.buscarItem(itemId)
        val logado = usuarioLogado.autenticado()
        if (!logado.possui("loan_renew") && item.emprestimo.leitor.id != logado.usuarioId) {
            throw ProibidoException("Você só pode renovar os seus próprios empréstimos.")
        }
        emprestimoService.renovar(item, relogio.hoje())
        auditoria.registrar(
            AcaoAuditoria.RENOVACAO_REGISTRADA, "Emprestimo", item.emprestimo.id,
            "item ${item.id}: nova data ${item.dataDevolucaoPrevista}",
        )
        return responder(item.emprestimo)
    }

    private fun responder(emprestimo: Emprestimo) =
        emprestimo.toResponse(relogio.hoje(), emprestimoService.maxRenovacoes())
}

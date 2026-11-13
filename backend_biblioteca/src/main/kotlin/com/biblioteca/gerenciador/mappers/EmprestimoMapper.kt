package com.biblioteca.gerenciador.mappers

import com.biblioteca.gerenciador.domain.Configuracao
import com.biblioteca.gerenciador.domain.Emprestimo
import com.biblioteca.gerenciador.domain.EmprestimoItem
import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.rest.requests.DevolucaoRequest
import com.biblioteca.gerenciador.rest.responses.ConfiguracaoResponse
import com.biblioteca.gerenciador.rest.responses.EmprestimoResponse
import com.biblioteca.gerenciador.rest.responses.ItemEmprestimoResponse
import com.biblioteca.gerenciador.rest.responses.PessoaResumoResponse
import java.time.LocalDate

/** Pares (itemId, condição). O @NotNull da condição já foi validado no controller. */
fun DevolucaoRequest.toDevolucoes() = itens.map { it.itemId to it.condicao!! }

fun Emprestimo.toResponse(hoje: LocalDate, maxRenovacoes: Int) = EmprestimoResponse(
    id = id!!,
    leitor = leitor.toResumo(),
    operador = operador.toResumo(),
    dataEmprestimo = dataEmprestimo,
    status = status,
    situacao = situacao(hoje),
    observacao = observacao,
    itens = itens.map { it.toResponse(hoje, maxRenovacoes) },
)

fun EmprestimoItem.toResponse(hoje: LocalDate, maxRenovacoes: Int) = ItemEmprestimoResponse(
    id = id!!,
    exemplarId = exemplar.id!!,
    codigoExemplar = exemplar.codigo,
    livroId = exemplar.livro.id!!,
    tituloLivro = exemplar.livro.titulo,
    dataDevolucaoPrevista = dataDevolucaoPrevista,
    dataDevolucaoReal = dataDevolucaoReal,
    situacao = situacao(hoje),
    diasAtraso = diasAtraso(hoje),
    renovacoes = renovacoes,
    renovacoesRestantes = if (devolvido) 0 else (maxRenovacoes - renovacoes).coerceAtLeast(0),
    condicaoRetirada = condicaoRetirada,
    condicaoDevolucao = condicaoDevolucao,
)

fun Usuario.toResumo() = PessoaResumoResponse(id = id!!, nome = nome)

fun Configuracao.toResponse() = ConfiguracaoResponse(chave = chave, valor = valor.toInt(), descricao = descricao)

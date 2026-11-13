package com.biblioteca.gerenciador.mappers

import com.biblioteca.gerenciador.domain.Exemplar
import com.biblioteca.gerenciador.rest.requests.AtualizarExemplarRequest
import com.biblioteca.gerenciador.rest.responses.ExemplarResponse

fun AtualizarExemplarRequest.aplicarEm(exemplar: Exemplar) =
    exemplar.atualizar(novoCodigo = codigo, novaCondicao = condicao, novoStatus = status)

fun Exemplar.toResponse() = ExemplarResponse(
    id = id!!,
    codigo = codigo,
    livroId = livro.id!!,
    condicao = condicao,
    status = status,
    dataCadastro = dataCadastro!!,
    dataAtualizacao = dataAtualizacao!!,
)

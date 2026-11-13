package com.biblioteca.gerenciador.mappers

import com.biblioteca.gerenciador.domain.Auditoria
import com.biblioteca.gerenciador.domain.Perfil
import com.biblioteca.gerenciador.domain.Permissao
import com.biblioteca.gerenciador.domain.Sessao
import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.rest.responses.AuditoriaResponse
import com.biblioteca.gerenciador.rest.responses.PerfilResponse
import com.biblioteca.gerenciador.rest.responses.PermissaoResponse
import com.biblioteca.gerenciador.rest.responses.SessaoResponse
import com.biblioteca.gerenciador.rest.responses.UsuarioLogadoResponse
import com.biblioteca.gerenciador.rest.responses.UsuarioResponse
import java.time.Instant
import java.util.UUID

fun Usuario.toUsuarioLogadoResponse() = UsuarioLogadoResponse(
    id = id!!,
    nome = nome,
    email = email,
    perfil = perfil.nome,
    permissoes = perfil.permissoes.map { it.codigo }.sorted(),
)

fun Usuario.toUsuarioResponse(agora: Instant) = UsuarioResponse(
    id = id!!,
    nome = nome,
    email = email,
    telefone = telefone,
    perfil = perfil.nome,
    bloqueadoAte = bloqueadoAte?.takeIf { estaBloqueado(agora) },
    dataCadastro = dataCadastro!!,
    dataAtualizacao = dataAtualizacao!!,
)

fun Sessao.toResponse(sessaoAtual: UUID?) = SessaoResponse(
    id = id,
    ip = ip,
    userAgent = userAgent,
    criadoEm = criadoEm,
    ultimoAcesso = ultimoAcesso,
    expiraEm = expiraEm,
    atual = id == sessaoAtual,
)

fun Perfil.toResponse() = PerfilResponse(
    id = id!!,
    nome = nome,
    descricao = descricao,
    editavel = editavel,
    permissoes = permissoes.map { it.codigo }.sorted(),
)

fun Permissao.toResponse() = PermissaoResponse(codigo = codigo, descricao = descricao)

fun Auditoria.toResponse() = AuditoriaResponse(
    id = id!!,
    dataHora = dataHora,
    acao = acao,
    usuario = usuario?.toResumo(),
    entidade = entidade,
    entidadeId = entidadeId,
    detalhes = detalhes,
    ip = ip,
)

package com.biblioteca.gerenciador.mappers

import com.biblioteca.gerenciador.domain.Documento
import com.biblioteca.gerenciador.domain.Perfil
import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.rest.requests.LeitorRequest
import com.biblioteca.gerenciador.rest.responses.LeitorResponse
import java.time.LocalDate

fun LeitorRequest.toEntity(perfilLeitor: Perfil) = Usuario(
    nome = nome.trim(),
    perfil = perfilLeitor,
    email = emailNormalizado(),
    tipoDocumento = tipoDocumento,
    documento = Documento.normalizar(tipoDocumento, documento),
    telefone = telefone.semBrancos(),
    endereco = endereco.semBrancos(),
)

fun LeitorRequest.aplicarEm(leitor: Usuario) = leitor.atualizarDados(
    nome = nome.trim(),
    email = emailNormalizado(),
    tipoDocumento = tipoDocumento,
    documento = Documento.normalizar(tipoDocumento, documento),
    telefone = telefone.semBrancos(),
    endereco = endereco.semBrancos(),
)

fun Usuario.toLeitorResponse(hoje: LocalDate) = LeitorResponse(
    id = id!!,
    nome = nome,
    tipoDocumento = tipoDocumento,
    documento = documento,
    email = email,
    telefone = telefone,
    endereco = endereco,
    acessoHabilitado = acessoHabilitado,
    suspensoAte = suspensoAte,
    suspenso = estaSuspenso(hoje),
    dataCadastro = dataCadastro!!,
    dataAtualizacao = dataAtualizacao!!,
)

/** E-mail é único sem diferenciar maiúsculas: sempre gravado em minúsculas. */
private fun LeitorRequest.emailNormalizado() = email.semBrancos()?.lowercase()

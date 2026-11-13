package com.biblioteca.gerenciador.mappers

import com.biblioteca.gerenciador.domain.ContagemExemplares
import com.biblioteca.gerenciador.domain.Genero
import com.biblioteca.gerenciador.domain.Livro
import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.rest.requests.LivroRequest
import com.biblioteca.gerenciador.rest.responses.CatalogoLivroResponse
import com.biblioteca.gerenciador.rest.responses.LivroResponse

fun LivroRequest.toEntity(generos: Set<Genero>, criadoPor: Usuario) = Livro(
    titulo = titulo.trim(),
    autor = autor.trim(),
    isbn = isbn.semBrancos(),
    editora = editora.semBrancos(),
    anoPublicacao = anoPublicacao,
    generos = generos.toMutableSet(),
    criadoPor = criadoPor,
)

fun LivroRequest.aplicarEm(livro: Livro, generos: Set<Genero>) = livro.atualizar(
    titulo = titulo.trim(),
    autor = autor.trim(),
    isbn = isbn.semBrancos(),
    editora = editora.semBrancos(),
    anoPublicacao = anoPublicacao,
    generos = generos,
)

fun Livro.toResponse(contagem: ContagemExemplares) = LivroResponse(
    id = id!!,
    isbn = isbn,
    titulo = titulo,
    autor = autor,
    editora = editora,
    anoPublicacao = anoPublicacao,
    generos = generos.sortedBy { it.nome }.map { it.toResponse() },
    totalExemplares = contagem.total,
    exemplaresDisponiveis = contagem.disponiveis,
    exemplaresEmprestados = contagem.emprestados,
    cadastradoPor = criadoPor?.nome,
    dataCadastro = dataCadastro!!,
    dataAtualizacao = dataAtualizacao!!,
)

fun Livro.toCatalogoResponse(contagem: ContagemExemplares) = CatalogoLivroResponse(
    id = id!!,
    titulo = titulo,
    autor = autor,
    editora = editora,
    anoPublicacao = anoPublicacao,
    generos = generos.map { it.nome }.sorted(),
    exemplaresDisponiveis = contagem.disponiveis,
    disponivel = contagem.disponiveis > 0,
)

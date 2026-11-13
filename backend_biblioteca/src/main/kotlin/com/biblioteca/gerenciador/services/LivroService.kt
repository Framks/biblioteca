package com.biblioteca.gerenciador.services

import com.biblioteca.gerenciador.domain.FiltroLivro
import com.biblioteca.gerenciador.domain.Livro
import com.biblioteca.gerenciador.enums.FiltroDisponibilidade
import com.biblioteca.gerenciador.enums.StatusRegistro
import com.biblioteca.gerenciador.exceptions.ConflitoException
import com.biblioteca.gerenciador.exceptions.NaoEncontradoException
import com.biblioteca.gerenciador.repositories.LivroRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/** Regras e operações de uma única entidade: Livro. Não conhece DTOs. */
@Service
class LivroService(private val repository: LivroRepository) {

    fun pesquisar(filtro: FiltroLivro, pageable: Pageable): Page<Livro> {
        val comDisponivel = when (filtro.disponibilidade) {
            FiltroDisponibilidade.DISPONIVEL -> true
            FiltroDisponibilidade.INDISPONIVEL -> false
            else -> null
        }
        val comEmprestado = if (filtro.disponibilidade == FiltroDisponibilidade.EMPRESTADO) true else null
        return repository.pesquisar(
            tituloPadrao = padraoLike(filtro.titulo),
            autorPadrao = padraoLike(filtro.autor),
            isbn = filtro.isbn?.ifBlank { null },
            generoId = filtro.generoId,
            comDisponivel = comDisponivel,
            comEmprestado = comEmprestado,
            status = StatusRegistro.ATIVO,
            pageable = pageable,
        )
    }

    /** Livros inativos são tratados como inexistentes. */
    fun buscarAtivo(id: Long): Livro =
        repository.findByIdAndStatus(id, StatusRegistro.ATIVO)
            ?: throw NaoEncontradoException("BOOK_NOT_FOUND", "Livro $id não encontrado.")

    fun salvar(livro: Livro): Livro {
        validarIsbnUnico(livro)
        return repository.save(livro)
    }

    fun inativar(livro: Livro) {
        livro.inativar()
        repository.save(livro)
    }

    /** "Senhor" -> "%senhor%"; nulo ou vazio -> "%" (qualquer valor). */
    private fun padraoLike(texto: String?): String =
        texto?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }?.let { "%$it%" } ?: "%"

    private fun validarIsbnUnico(livro: Livro) {
        val isbn = livro.isbn ?: return
        val duplicado = livro.id
            ?.let { repository.existsByIsbnAndIdNot(isbn, it) }
            ?: repository.existsByIsbn(isbn)
        if (duplicado) throw ConflitoException("ISBN_ALREADY_EXISTS", "Já existe um livro com o ISBN $isbn.")
    }
}

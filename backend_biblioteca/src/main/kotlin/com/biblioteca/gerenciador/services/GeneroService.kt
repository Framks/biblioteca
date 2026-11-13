package com.biblioteca.gerenciador.services

import com.biblioteca.gerenciador.domain.Genero
import com.biblioteca.gerenciador.exceptions.ConflitoException
import com.biblioteca.gerenciador.exceptions.NaoEncontradoException
import com.biblioteca.gerenciador.repositories.GeneroRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

/** Regras e operações de uma única entidade: Genero. Não conhece DTOs. */
@Service
class GeneroService(private val repository: GeneroRepository) {

    fun listar(): List<Genero> = repository.findAllByOrderByNomeAsc()

    fun buscar(id: Long): Genero =
        repository.findByIdOrNull(id) ?: throw naoEncontrado(id)

    /** Busca todos os ids informados; falha se algum não existir. */
    fun buscarTodos(ids: Collection<Long>): Set<Genero> {
        val encontrados = repository.findAllById(ids).toSet()
        val faltando = ids.toSet() - encontrados.mapNotNull { it.id }.toSet()
        if (faltando.isNotEmpty()) throw naoEncontrado(faltando.first())
        return encontrados
    }

    fun salvar(genero: Genero): Genero {
        val duplicado = genero.id
            ?.let { repository.existsByNomeIgnoreCaseAndIdNot(genero.nome, it) }
            ?: repository.existsByNomeIgnoreCase(genero.nome)
        if (duplicado) throw ConflitoException("GENRE_ALREADY_EXISTS", "Já existe um gênero com o nome '${genero.nome}'.")
        return repository.save(genero)
    }

    private fun naoEncontrado(id: Long) = NaoEncontradoException("GENRE_NOT_FOUND", "Gênero $id não encontrado.")
}

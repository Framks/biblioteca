package com.biblioteca.gerenciador.services

import com.biblioteca.gerenciador.domain.ContagemExemplares
import com.biblioteca.gerenciador.domain.Exemplar
import com.biblioteca.gerenciador.domain.Livro
import com.biblioteca.gerenciador.enums.CondicaoExemplar
import com.biblioteca.gerenciador.enums.StatusExemplar
import com.biblioteca.gerenciador.exceptions.ConflitoException
import com.biblioteca.gerenciador.exceptions.NaoEncontradoException
import com.biblioteca.gerenciador.repositories.ExemplarRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

/** Regras e operações de uma única entidade: Exemplar. Não conhece DTOs. */
@Service
class ExemplarService(private val repository: ExemplarRepository) {

    fun listarPorLivro(livro: Livro): List<Exemplar> = repository.findAllByLivroOrderByCodigoAsc(livro)

    fun buscar(id: Long): Exemplar =
        repository.findByIdOrNull(id) ?: throw NaoEncontradoException("COPY_NOT_FOUND", "Exemplar $id não encontrado.")

    /** Busca todos os ids, na ordem informada; falha se algum não existir. */
    fun buscarTodos(ids: Collection<Long>): List<Exemplar> {
        val porId = repository.findAllById(ids).associateBy { it.id }
        return ids.map { id -> porId[id] ?: throw NaoEncontradoException("COPY_NOT_FOUND", "Exemplar $id não encontrado.") }
    }

    /** Cria [quantidade] exemplares com códigos sequenciais (RN15): LIV000123-EX01, LIV000123-EX02... */
    fun criar(livro: Livro, quantidade: Int, condicao: CondicaoExemplar): List<Exemplar> {
        val proximo = repository.countByLivro(livro).toInt() + 1
        val novos = (proximo until proximo + quantidade).map { sequencia ->
            Exemplar(codigo = gerarCodigo(livro, sequencia), livro = livro, condicao = condicao)
        }
        novos.forEach(::validarCodigoUnico)
        return repository.saveAll(novos)
    }

    fun salvar(exemplar: Exemplar): Exemplar {
        validarCodigoUnico(exemplar)
        return repository.save(exemplar)
    }

    fun possuiEmprestado(livro: Livro): Boolean = repository.existsByLivroAndStatus(livro, StatusExemplar.EMPRESTADO)

    /** Contagem de exemplares por livro, numa única consulta. Livros sem exemplares ficam com contagem zerada. */
    fun contar(livroIds: Collection<Long>): Map<Long, ContagemExemplares> {
        if (livroIds.isEmpty()) return emptyMap()
        return repository.contarPorStatus(livroIds)
            .groupBy { it.livroId }
            .mapValues { (_, linhas) ->
                val porStatus = linhas.associate { it.status to it.quantidade.toInt() }
                ContagemExemplares(
                    total = porStatus.filterKeys { it != StatusExemplar.BAIXADO }.values.sum(),
                    disponiveis = porStatus[StatusExemplar.DISPONIVEL] ?: 0,
                    emprestados = porStatus[StatusExemplar.EMPRESTADO] ?: 0,
                )
            }
    }

    private fun gerarCodigo(livro: Livro, sequencia: Int) = "LIV%06d-EX%02d".format(livro.id, sequencia)

    private fun validarCodigoUnico(exemplar: Exemplar) {
        val duplicado = exemplar.id
            ?.let { repository.existsByCodigoAndIdNot(exemplar.codigo, it) }
            ?: repository.existsByCodigo(exemplar.codigo)
        if (duplicado) {
            throw ConflitoException("COPY_CODE_ALREADY_EXISTS", "Já existe um exemplar com o código ${exemplar.codigo}.")
        }
    }
}

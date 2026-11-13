package com.biblioteca.gerenciador.repositories

import com.biblioteca.gerenciador.domain.Exemplar
import com.biblioteca.gerenciador.domain.Livro
import com.biblioteca.gerenciador.enums.StatusExemplar
import jakarta.persistence.QueryHint
import org.hibernate.jpa.HibernateHints
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints

interface ExemplarRepository : JpaRepository<Exemplar, Long> {

    fun findAllByLivroOrderByCodigoAsc(livro: Livro): List<Exemplar>

    fun countByLivro(livro: Livro): Long

    fun existsByLivroAndStatus(livro: Livro, status: StatusExemplar): Boolean

    // Consultas de unicidade não podem disparar o auto-flush: a entidade editada ainda não foi validada
    // e gravá-la antes faria a constraint UNIQUE estourar (500) em vez do 409 esperado.
    @QueryHints(QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
    fun existsByCodigo(codigo: String): Boolean

    @QueryHints(QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
    fun existsByCodigoAndIdNot(codigo: String, id: Long): Boolean

    /** Uma linha por (livro, status) com a quantidade. Evita uma consulta por livro na listagem. */
    @Query(
        """
        select e.livro.id as livroId, e.status as status, count(e) as quantidade
        from Exemplar e
        where e.livro.id in :livroIds
        group by e.livro.id, e.status
        """,
    )
    fun contarPorStatus(livroIds: Collection<Long>): List<QuantidadePorStatus>

    interface QuantidadePorStatus {
        val livroId: Long
        val status: StatusExemplar
        val quantidade: Long
    }
}

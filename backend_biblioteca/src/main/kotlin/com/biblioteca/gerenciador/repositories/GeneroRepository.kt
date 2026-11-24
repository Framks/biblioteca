package com.biblioteca.gerenciador.repositories

import com.biblioteca.gerenciador.domain.Genero
import jakarta.persistence.QueryHint
import org.hibernate.jpa.HibernateHints
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.QueryHints

interface GeneroRepository : JpaRepository<Genero, Long> {

    fun findAllByOrderByNomeAsc(): List<Genero>

    // Consultas de unicidade não podem disparar o auto-flush: a entidade editada ainda não foi validada
    // e gravá-la antes faria a constraint UNIQUE estourar (500) em vez do 409 esperado.
    @QueryHints(QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
    fun existsByNomeIgnoreCase(nome: String): Boolean

    @QueryHints(QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
    fun existsByNomeIgnoreCaseAndIdNot(nome: String, id: Long): Boolean
}

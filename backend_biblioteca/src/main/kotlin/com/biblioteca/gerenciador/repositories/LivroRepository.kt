package com.biblioteca.gerenciador.repositories

import com.biblioteca.gerenciador.domain.Livro
import com.biblioteca.gerenciador.enums.StatusRegistro
import jakarta.persistence.QueryHint
import org.hibernate.jpa.HibernateHints
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints

interface LivroRepository : JpaRepository<Livro, Long> {

    fun findByIdAndStatus(id: Long, status: StatusRegistro): Livro?

    // Consultas de unicidade não podem disparar o auto-flush: a entidade editada ainda não foi validada
    // e gravá-la antes faria a constraint UNIQUE estourar (500) em vez do 409 esperado.
    @QueryHints(QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
    fun existsByIsbn(isbn: String): Boolean

    @QueryHints(QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
    fun existsByIsbnAndIdNot(isbn: String, id: Long): Boolean

    /**
     * Pesquisa do acervo (RF04). Filtros nulos são ignorados.
     * [tituloPadrao]/[autorPadrao]: padrão do LIKE já em minúsculas ("%" = qualquer). Nunca nulos: no PostgreSQL,
     * `lower(:param)` com parâmetro nulo não tem tipo definido e a consulta falha (no H2 passa, cuidado).
     * [comDisponivel]: true = tem exemplar disponível; false = não tem nenhum disponível.
     * [comEmprestado]: true = tem ao menos um exemplar emprestado.
     */
    @Query(
        """
        select distinct l from Livro l left join l.generos g
        where l.status = :status
          and lower(l.titulo) like :tituloPadrao
          and lower(l.autor) like :autorPadrao
          and (:isbn is null or l.isbn = :isbn)
          and (:generoId is null or g.id = :generoId)
          and (
               :comDisponivel is null
            or (:comDisponivel = true and exists (
                  select 1 from Exemplar e where e.livro = l
                  and e.status = com.biblioteca.gerenciador.enums.StatusExemplar.DISPONIVEL))
            or (:comDisponivel = false and not exists (
                  select 1 from Exemplar e where e.livro = l
                  and e.status = com.biblioteca.gerenciador.enums.StatusExemplar.DISPONIVEL))
          )
          and (
               :comEmprestado is null
            or exists (
                  select 1 from Exemplar e where e.livro = l
                  and e.status = com.biblioteca.gerenciador.enums.StatusExemplar.EMPRESTADO)
          )
        """,
    )
    fun pesquisar(
        tituloPadrao: String,
        autorPadrao: String,
        isbn: String?,
        generoId: Long?,
        comDisponivel: Boolean?,
        comEmprestado: Boolean?,
        status: StatusRegistro,
        pageable: Pageable,
    ): Page<Livro>
}

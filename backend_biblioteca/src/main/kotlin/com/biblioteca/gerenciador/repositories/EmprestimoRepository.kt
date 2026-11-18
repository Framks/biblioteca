package com.biblioteca.gerenciador.repositories

import com.biblioteca.gerenciador.domain.Emprestimo
import com.biblioteca.gerenciador.domain.EmprestimoItem
import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.enums.StatusEmprestimo
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.time.LocalDate

interface EmprestimoRepository : JpaRepository<Emprestimo, Long> {

    /**
     * Consulta de empréstimos (RF07, RF12). Filtros nulos são ignorados.
     * [comAtraso]: true = tem item atrasado em [hoje]; false = nenhum item atrasado.
     */
    @Query(
        """
        select e from Emprestimo e
        where (:leitorId is null or e.leitor.id = :leitorId)
          and (:status is null or e.status = :status)
          and (:livroId is null or exists (
                select 1 from EmprestimoItem i where i.emprestimo = e and i.exemplar.livro.id = :livroId))
          and (
               :comAtraso is null
            or (:comAtraso = true and exists (
                  select 1 from EmprestimoItem i where i.emprestimo = e
                  and i.dataDevolucaoReal is null and i.dataDevolucaoPrevista < :hoje))
            or (:comAtraso = false and not exists (
                  select 1 from EmprestimoItem i where i.emprestimo = e
                  and i.dataDevolucaoReal is null and i.dataDevolucaoPrevista < :hoje))
          )
        """,
    )
    fun pesquisar(
        leitorId: Long?,
        livroId: Long?,
        status: StatusEmprestimo?,
        comAtraso: Boolean?,
        hoje: LocalDate,
        pageable: Pageable,
    ): Page<Emprestimo>

    // ---------- relatórios (RF13) ----------

    fun countByStatus(status: StatusEmprestimo): Long

    /** Datas dos empréstimos no período [de, ate). O agrupamento por mês é feito no fuso da biblioteca. */
    @Query("select e.dataEmprestimo from Emprestimo e where e.dataEmprestimo >= :de and e.dataEmprestimo < :ate")
    fun datasEntre(de: Instant, ate: Instant): List<Instant>
}

interface EmprestimoItemRepository : JpaRepository<EmprestimoItem, Long> {

    /** Itens ainda não devolvidos de um leitor. */
    fun findByEmprestimoLeitorAndDataDevolucaoRealIsNull(leitor: Usuario): List<EmprestimoItem>

    fun existsByEmprestimoLeitorAndDataDevolucaoRealIsNull(leitor: Usuario): Boolean

    // ---------- relatórios (RF13) ----------

    fun countByDataDevolucaoRealIsNull(): Long

    fun countByDataDevolucaoRealIsNullAndDataDevolucaoPrevistaBefore(hoje: LocalDate): Long

    @Query(
        """
        select count(distinct i.emprestimo) from EmprestimoItem i
        where i.dataDevolucaoReal is null and i.dataDevolucaoPrevista < :hoje
        """,
    )
    fun contarEmprestimosAtrasados(hoje: LocalDate): Long

    /** Itens atrasados com leitor, exemplar e livro já carregados (evita uma consulta por item). */
    @Query(
        """
        select i from EmprestimoItem i
          join fetch i.emprestimo e join fetch e.leitor
          join fetch i.exemplar x join fetch x.livro
        where i.dataDevolucaoReal is null and i.dataDevolucaoPrevista < :hoje
        """,
    )
    fun itensAtrasados(hoje: LocalDate): List<EmprestimoItem>

    /** Livros mais emprestados no período [de, ate). */
    @Query(
        """
        select l.id as livroId, l.titulo as titulo, l.autor as autor, count(i) as quantidade
        from EmprestimoItem i join i.exemplar x join x.livro l
        where i.emprestimo.dataEmprestimo >= :de and i.emprestimo.dataEmprestimo < :ate
        group by l.id, l.titulo, l.autor
        order by count(i) desc, l.titulo asc
        """,
    )
    fun maisEmprestados(de: Instant, ate: Instant, pageable: Pageable): List<LivroMaisEmprestado>

    interface LivroMaisEmprestado {
        val livroId: Long
        val titulo: String
        val autor: String
        val quantidade: Long
    }
}

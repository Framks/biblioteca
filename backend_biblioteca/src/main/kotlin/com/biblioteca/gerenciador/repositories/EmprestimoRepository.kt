package com.biblioteca.gerenciador.repositories

import com.biblioteca.gerenciador.domain.Emprestimo
import com.biblioteca.gerenciador.domain.EmprestimoItem
import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.enums.StatusEmprestimo
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
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
}

interface EmprestimoItemRepository : JpaRepository<EmprestimoItem, Long> {

    /** Itens ainda não devolvidos de um leitor. */
    fun findByEmprestimoLeitorAndDataDevolucaoRealIsNull(leitor: Usuario): List<EmprestimoItem>

    fun existsByEmprestimoLeitorAndDataDevolucaoRealIsNull(leitor: Usuario): Boolean
}

package com.biblioteca.gerenciador.rest

import com.biblioteca.gerenciador.enums.SituacaoEmprestimo
import com.biblioteca.gerenciador.facades.EmprestimoFacade
import com.biblioteca.gerenciador.rest.requests.DevolucaoRequest
import com.biblioteca.gerenciador.rest.requests.EmprestimoRequest
import com.biblioteca.gerenciador.rest.responses.EmprestimoResponse
import com.biblioteca.gerenciador.rest.responses.PageResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/v1")
class EmprestimoController(private val facade: EmprestimoFacade) {

    /** GET /loans?situacao=ATRASADO&leitorId=&livroId=&page=0&size=20 (RF12). Mais recentes primeiro. */
    @GetMapping("/loans")
    fun pesquisar(
        @RequestParam(required = false) situacao: SituacaoEmprestimo?,
        @RequestParam(required = false) leitorId: Long?,
        @RequestParam(required = false) livroId: Long?,
        @PageableDefault(size = 20, sort = ["dataEmprestimo"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): PageResponse<EmprestimoResponse> =
        facade.pesquisar(leitorId, livroId, situacao, pageable.validarOrdenacao(*ORDENACAO_EMPRESTIMO))

    /** Histórico do leitor (RF07). */
    @GetMapping("/readers/{leitorId}/loans")
    fun historicoDoLeitor(
        @PathVariable leitorId: Long,
        @RequestParam(required = false) situacao: SituacaoEmprestimo?,
        @PageableDefault(size = 20, sort = ["dataEmprestimo"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): PageResponse<EmprestimoResponse> =
        facade.historicoDoLeitor(leitorId, situacao, pageable.validarOrdenacao(*ORDENACAO_EMPRESTIMO))

    @GetMapping("/loans/{id}")
    fun buscar(@PathVariable id: Long): EmprestimoResponse = facade.buscar(id)

    @PostMapping("/loans")
    fun registrar(@Valid @RequestBody request: EmprestimoRequest): ResponseEntity<EmprestimoResponse> {
        val criado = facade.registrar(request)
        return ResponseEntity.created(URI.create("/api/v1/loans/${criado.id}")).body(criado)
    }

    @PostMapping("/loans/{id}/return")
    fun devolver(@PathVariable id: Long, @Valid @RequestBody request: DevolucaoRequest): EmprestimoResponse =
        facade.devolver(id, request)

    @PostMapping("/loans/items/{itemId}/renew")
    fun renovar(@PathVariable itemId: Long): EmprestimoResponse = facade.renovar(itemId)

    private companion object {
        val ORDENACAO_EMPRESTIMO = arrayOf("dataEmprestimo")
    }
}

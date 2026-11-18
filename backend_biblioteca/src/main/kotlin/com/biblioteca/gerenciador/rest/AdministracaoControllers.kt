package com.biblioteca.gerenciador.rest

import com.biblioteca.gerenciador.enums.AcaoAuditoria
import com.biblioteca.gerenciador.facades.AuditoriaFacade
import com.biblioteca.gerenciador.facades.PerfilFacade
import com.biblioteca.gerenciador.facades.UsuarioFacade
import com.biblioteca.gerenciador.rest.requests.AtualizarUsuarioRequest
import com.biblioteca.gerenciador.rest.requests.CriarUsuarioRequest
import com.biblioteca.gerenciador.rest.requests.PermissoesPerfilRequest
import com.biblioteca.gerenciador.rest.requests.RedefinirSenhaRequest
import com.biblioteca.gerenciador.rest.responses.AuditoriaResponse
import com.biblioteca.gerenciador.rest.responses.PageResponse
import com.biblioteca.gerenciador.rest.responses.PerfilResponse
import com.biblioteca.gerenciador.rest.responses.PermissaoResponse
import com.biblioteca.gerenciador.rest.responses.SessaoResponse
import com.biblioteca.gerenciador.rest.responses.UsuarioResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.LocalDate

/** Funcionários e administradores (RF17). */
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasAuthority('user_manage')")
class UsuarioController(private val facade: UsuarioFacade) {

    @GetMapping
    fun pesquisar(
        @RequestParam(required = false) busca: String?,
        @PageableDefault(size = 20, sort = ["nome"]) pageable: Pageable,
    ): PageResponse<UsuarioResponse> = facade.pesquisar(busca, pageable.validarOrdenacao("nome", "dataCadastro"))

    @GetMapping("/{id}")
    fun buscar(@PathVariable id: Long): UsuarioResponse = facade.buscar(id)

    @PostMapping
    fun criar(@Valid @RequestBody request: CriarUsuarioRequest): ResponseEntity<UsuarioResponse> {
        val criado = facade.criar(request)
        return ResponseEntity.created(URI.create("/api/v1/users/${criado.id}")).body(criado)
    }

    @PutMapping("/{id}")
    fun atualizar(@PathVariable id: Long, @Valid @RequestBody request: AtualizarUsuarioRequest): UsuarioResponse =
        facade.atualizar(id, request)

    @PutMapping("/{id}/password")
    fun redefinirSenha(@PathVariable id: Long, @Valid @RequestBody request: RedefinirSenhaRequest): ResponseEntity<Unit> {
        facade.redefinirSenha(id, request)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}")
    fun inativar(@PathVariable id: Long): ResponseEntity<Unit> {
        facade.inativar(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}/sessions")
    @PreAuthorize("hasAuthority('session_manage')")
    fun sessoes(@PathVariable id: Long): List<SessaoResponse> = facade.sessoes(id)
}

/** Perfis e permissões (RF18). */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAuthority('role_manage')")
class PerfilController(private val facade: PerfilFacade) {

    @GetMapping("/roles")
    fun listar(): List<PerfilResponse> = facade.listar()

    @GetMapping("/permissions")
    fun listarPermissoes(): List<PermissaoResponse> = facade.listarPermissoes()

    @PutMapping("/roles/{id}/permissions")
    fun atualizarPermissoes(@PathVariable id: Long, @Valid @RequestBody request: PermissoesPerfilRequest): PerfilResponse =
        facade.atualizarPermissoes(id, request)
}

/** Auditoria (RF21). */
@RestController
@RequestMapping("/api/v1/audit")
@PreAuthorize("hasAuthority('audit_read')")
class AuditoriaController(private val facade: AuditoriaFacade) {

    /** GET /audit?acao=LOGIN_FALHA&usuarioId=&de=2026-11-01&ate=2026-11-30 (mais recentes primeiro). */
    @GetMapping
    fun pesquisar(
        @RequestParam(required = false) acao: AcaoAuditoria?,
        @RequestParam(required = false) usuarioId: Long?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) de: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) ate: LocalDate?,
        @PageableDefault(size = 50, sort = ["dataHora", "id"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): PageResponse<AuditoriaResponse> = facade.pesquisar(acao, usuarioId, de, ate, pageable.validarOrdenacao("dataHora", "id"))
}

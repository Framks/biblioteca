package com.biblioteca.gerenciador.rest

import com.biblioteca.gerenciador.facades.GeneroFacade
import com.biblioteca.gerenciador.rest.requests.GeneroRequest
import com.biblioteca.gerenciador.rest.responses.GeneroResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/v1/genres")
class GeneroController(private val facade: GeneroFacade) {

    @GetMapping
    fun listar(): List<GeneroResponse> = facade.listar()

    @PostMapping
    fun criar(@Valid @RequestBody request: GeneroRequest): ResponseEntity<GeneroResponse> {
        val criado = facade.criar(request)
        return ResponseEntity.created(URI.create("/api/v1/genres/${criado.id}")).body(criado)
    }

    @PutMapping("/{id}")
    fun atualizar(@PathVariable id: Long, @Valid @RequestBody request: GeneroRequest): GeneroResponse =
        facade.atualizar(id, request)
}

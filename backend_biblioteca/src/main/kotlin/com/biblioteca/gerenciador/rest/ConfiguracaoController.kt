package com.biblioteca.gerenciador.rest

import com.biblioteca.gerenciador.facades.ConfiguracaoFacade
import com.biblioteca.gerenciador.rest.requests.ConfiguracaoRequest
import com.biblioteca.gerenciador.rest.responses.ConfiguracaoResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Parâmetros de negócio (RF19). Na fase de segurança, exigirá a permissão config_manage. */
@RestController
@RequestMapping("/api/v1/settings")
class ConfiguracaoController(private val facade: ConfiguracaoFacade) {

    @GetMapping
    fun listar(): List<ConfiguracaoResponse> = facade.listar()

    @PutMapping("/{chave}")
    fun atualizar(@PathVariable chave: String, @Valid @RequestBody request: ConfiguracaoRequest): ConfiguracaoResponse =
        facade.atualizar(chave, request)
}

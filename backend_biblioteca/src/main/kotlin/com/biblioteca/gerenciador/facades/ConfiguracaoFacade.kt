package com.biblioteca.gerenciador.facades

import com.biblioteca.gerenciador.mappers.toResponse
import com.biblioteca.gerenciador.rest.requests.ConfiguracaoRequest
import com.biblioteca.gerenciador.rest.responses.ConfiguracaoResponse
import com.biblioteca.gerenciador.services.ConfiguracaoService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ConfiguracaoFacade(private val configuracaoService: ConfiguracaoService) {

    @Transactional(readOnly = true)
    fun listar(): List<ConfiguracaoResponse> = configuracaoService.listar().map { it.toResponse() }

    @Transactional
    fun atualizar(chave: String, request: ConfiguracaoRequest): ConfiguracaoResponse =
        configuracaoService.atualizar(chave, request.valor).toResponse()
}

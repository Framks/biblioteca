package com.biblioteca.gerenciador.facades

import com.biblioteca.gerenciador.enums.AcaoAuditoria
import com.biblioteca.gerenciador.mappers.toResponse
import com.biblioteca.gerenciador.rest.requests.ConfiguracaoRequest
import com.biblioteca.gerenciador.rest.responses.ConfiguracaoResponse
import com.biblioteca.gerenciador.services.AuditoriaService
import com.biblioteca.gerenciador.services.ConfiguracaoService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ConfiguracaoFacade(
    private val configuracaoService: ConfiguracaoService,
    private val auditoria: AuditoriaService,
) {

    @Transactional(readOnly = true)
    fun listar(): List<ConfiguracaoResponse> = configuracaoService.listar().map { it.toResponse() }

    @Transactional
    fun atualizar(chave: String, request: ConfiguracaoRequest): ConfiguracaoResponse {
        val anterior = configuracaoService.buscar(chave).valor
        val configuracao = configuracaoService.atualizar(chave, request.valor)
        auditoria.registrar(AcaoAuditoria.CONFIGURACAO_ALTERADA, "Configuracao", null, "$chave: $anterior -> ${request.valor}")
        return configuracao.toResponse()
    }
}

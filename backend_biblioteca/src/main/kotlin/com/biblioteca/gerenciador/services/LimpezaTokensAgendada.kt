package com.biblioteca.gerenciador.services

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/** 6.4: remove da blocklist (e os refresh tokens) o que já expirou: depois disso o JWT já seria recusado de qualquer forma. */
@Component
class LimpezaTokensAgendada(
    private val sessaoService: SessaoService,
    private val relogio: Relogio,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(initialDelayString = "PT1H", fixedDelayString = "PT1H")
    fun limpar() {
        val removidos = sessaoService.limparExpirados(relogio.agora())
        if (removidos > 0) log.info("Limpeza de tokens expirados: {} registro(s) removido(s).", removidos)
    }
}

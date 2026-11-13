package com.biblioteca.gerenciador.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Nos testes, "agora" é sempre [AGORA]. Permite testar atraso, suspensão e renovação de forma determinística. */
@TestConfiguration
class ClockFixoConfig {

    @Bean
    @Primary
    fun clockFixo(): Clock = Clock.fixed(AGORA, ZoneOffset.UTC)

    companion object {
        val AGORA: Instant = Instant.parse("2026-11-13T12:00:00Z")

        /** "Hoje" da biblioteca (America/Fortaleza): 13/11/2026, 09h locais. */
        val HOJE: LocalDate = LocalDate.of(2026, 11, 13)
    }
}

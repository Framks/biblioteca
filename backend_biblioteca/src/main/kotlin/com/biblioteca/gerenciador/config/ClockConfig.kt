package com.biblioteca.gerenciador.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/**
 * Toda regra que depende de "agora" (atraso, suspensão, renovação) deve receber este Clock
 * em vez de chamar LocalDate.now(). Assim os testes podem fixar a data (ver ClockFixoConfig).
 */
@Configuration
class ClockConfig {

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}

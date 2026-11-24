package com.biblioteca.gerenciador.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * "Agora" e "hoje" do negócio. Instantes são em UTC (RNF09), mas o "hoje" que define prazos e atrasos
 * é o da biblioteca (America/Fortaleza): às 22h de Quixadá ainda é o mesmo dia, embora em UTC já seja o seguinte.
 * Nos testes, o Clock é fixo (ClockFixoConfig).
 */
@Component
class Relogio(
    private val clock: Clock,
    @Value("\${app.fuso-horario}") fusoHorario: String,
) {
    val zona: ZoneId = ZoneId.of(fusoHorario)

    fun agora(): Instant = Instant.now(clock)

    fun hoje(): LocalDate = LocalDate.now(clock.withZone(zona))
}

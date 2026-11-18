package com.biblioteca.gerenciador.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Libera o frontend (outra porta/origem) para chamar a API, aplicado pelo Spring Security (.cors {}).
 * allowCredentials = true é necessário para o navegador enviar os cookies refresh_token e device_id:
 * no frontend, use fetch(..., { credentials: "include" }).
 */
@Configuration
class WebConfig(
    @Value("\${app.cors.origens-permitidas}") private val origensPermitidas: List<String>,
) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuracao = CorsConfiguration().apply {
            allowedOrigins = origensPermitidas
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            exposedHeaders = listOf("Location", "Content-Disposition")
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/api/**", configuracao) }
    }
}

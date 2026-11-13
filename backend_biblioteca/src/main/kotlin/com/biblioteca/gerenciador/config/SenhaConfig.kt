package com.biblioteca.gerenciador.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * Hash de senhas com BCrypt (RNF03).
 * Usa só o módulo spring-security-crypto: os endpoints continuam abertos até a fase de segurança.
 */
@Configuration
class SenhaConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}

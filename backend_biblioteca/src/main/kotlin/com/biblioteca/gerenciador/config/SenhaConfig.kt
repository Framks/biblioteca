package com.biblioteca.gerenciador.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/** Hash de senhas com BCrypt (RNF03): só o hash é gravado, nunca a senha. */
@Configuration
class SenhaConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}

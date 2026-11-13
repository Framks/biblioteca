package com.biblioteca.gerenciador

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.scheduling.annotation.EnableScheduling

// UserDetailsServiceAutoConfiguration criaria um usuário em memória com senha gerada; a autenticação aqui é por JWT.
@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
@EnableScheduling
class GerenciadorApplication

fun main(args: Array<String>) {
    runApplication<GerenciadorApplication>(*args)
}

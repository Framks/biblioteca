package com.biblioteca.gerenciador.config

import com.biblioteca.gerenciador.repositories.UsuarioRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.support.TransactionTemplate

/**
 * Define a senha do administrador inicial (criado sem senha pela migration V3) na primeira execução.
 * A senha vem de app.seguranca.admin-senha-inicial (variável ADMIN_SENHA_INICIAL em produção) e
 * nunca sobrescreve uma senha já definida. Depois do primeiro login, troque-a em PUT /auth/password.
 */
@Configuration
class AdminInicialConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun definirSenhaAdminInicial(
        usuarioRepository: UsuarioRepository,
        passwordEncoder: PasswordEncoder,
        transacao: TransactionTemplate,
        @Value("\${app.seguranca.admin-senha-inicial:}") senhaInicial: String,
    ) = ApplicationRunner {
        transacao.executeWithoutResult {
            val admin = usuarioRepository.findByEmail(EMAIL_ADMIN_INICIAL) ?: return@executeWithoutResult
            when {
                admin.acessoHabilitado -> Unit
                senhaInicial.isBlank() -> log.warn("Administrador inicial sem senha: defina app.seguranca.admin-senha-inicial.")
                else -> {
                    admin.definirSenha(passwordEncoder.encode(senhaInicial)!!)
                    usuarioRepository.save(admin)
                    log.info("Senha do administrador inicial ({}) definida.", EMAIL_ADMIN_INICIAL)
                }
            }
        }
    }

    companion object {
        const val EMAIL_ADMIN_INICIAL = "admin@biblioteca.local"
    }
}

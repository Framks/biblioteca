package com.biblioteca.gerenciador.services

import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.repositories.UsuarioRepository
import org.springframework.stereotype.Component

/** Quem está executando a operação atual (operador do empréstimo, quem cadastrou o livro etc.). */
interface UsuarioLogado {
    fun obter(): Usuario
}

/**
 * Implementação PROVISÓRIA, até a fase de segurança: sempre o administrador inicial (migration V3).
 * Na fase de segurança, será substituída por uma que lê o usuário do JWT, sem mudar quem usa a interface.
 */
@Component
class UsuarioLogadoProvisorio(private val repository: UsuarioRepository) : UsuarioLogado {

    override fun obter(): Usuario =
        repository.findByEmail(EMAIL_ADMIN_INICIAL)
            ?: error("Administrador inicial ($EMAIL_ADMIN_INICIAL) não encontrado. A migration V3 foi aplicada?")

    companion object {
        const val EMAIL_ADMIN_INICIAL = "admin@biblioteca.local"
    }
}

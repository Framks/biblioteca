package com.biblioteca.gerenciador.services

import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.exceptions.NaoAutenticadoException
import com.biblioteca.gerenciador.repositories.UsuarioRepository
import com.biblioteca.gerenciador.security.UsuarioAutenticado
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/** Quem está executando a operação atual (operador do empréstimo, quem cadastrou o livro etc.). */
interface UsuarioLogado {
    /** Dados da autenticação (id, sessão, permissões), sem consultar o banco. */
    fun autenticado(): UsuarioAutenticado

    /** A entidade do usuário logado. */
    fun obter(): Usuario

    fun possui(permissao: String): Boolean = autenticado().possui(permissao)
}

/** Lê o usuário do SecurityContext, preenchido pelo JwtAutenticacaoFiltro a partir do JWT. */
@Component
class UsuarioLogadoSeguranca(private val repository: UsuarioRepository) : UsuarioLogado {

    override fun autenticado(): UsuarioAutenticado =
        SecurityContextHolder.getContext().authentication?.principal as? UsuarioAutenticado
            ?: throw NaoAutenticadoException("UNAUTHENTICATED", "Faça login para acessar este recurso.")

    override fun obter(): Usuario =
        repository.findByIdOrNull(autenticado().usuarioId)
            ?: throw NaoAutenticadoException("UNAUTHENTICATED", "Usuário não encontrado.")
}

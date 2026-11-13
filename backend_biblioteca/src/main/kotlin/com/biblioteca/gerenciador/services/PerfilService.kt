package com.biblioteca.gerenciador.services

import com.biblioteca.gerenciador.domain.Perfil
import com.biblioteca.gerenciador.repositories.PerfilRepository
import org.springframework.stereotype.Service

@Service
class PerfilService(private val repository: PerfilRepository) {

    /** Os perfis padrão vêm da migration V3; se faltar, é erro de instalação, não do usuário. */
    fun buscarPorNome(nome: String): Perfil =
        repository.findByNome(nome) ?: error("Perfil $nome não encontrado. A migration V3 foi aplicada?")
}

package com.biblioteca.gerenciador.repositories

import com.biblioteca.gerenciador.domain.Perfil
import org.springframework.data.jpa.repository.JpaRepository

interface PerfilRepository : JpaRepository<Perfil, Long> {

    fun findByNome(nome: String): Perfil?
}

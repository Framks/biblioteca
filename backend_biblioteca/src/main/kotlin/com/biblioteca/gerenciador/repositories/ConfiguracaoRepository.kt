package com.biblioteca.gerenciador.repositories

import com.biblioteca.gerenciador.domain.Configuracao
import org.springframework.data.jpa.repository.JpaRepository

interface ConfiguracaoRepository : JpaRepository<Configuracao, String> {

    fun findAllByOrderByChaveAsc(): List<Configuracao>
}

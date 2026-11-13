package com.biblioteca.gerenciador.domain

import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "genero")
class Genero(
    var nome: String,
) : EntidadeBase() {

    fun renomear(novoNome: String) {
        nome = novoNome.trim()
    }
}

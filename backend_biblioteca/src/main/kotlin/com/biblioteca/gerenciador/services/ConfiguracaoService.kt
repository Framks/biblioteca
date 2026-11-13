package com.biblioteca.gerenciador.services

import com.biblioteca.gerenciador.domain.Configuracao
import com.biblioteca.gerenciador.exceptions.NaoEncontradoException
import com.biblioteca.gerenciador.repositories.ConfiguracaoRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

/** Parâmetros de negócio da tabela CONFIGURACAO (RF19, seção 8.2). */
@Service
class ConfiguracaoService(private val repository: ConfiguracaoRepository) {

    fun listar(): List<Configuracao> = repository.findAllByOrderByChaveAsc()

    fun buscar(chave: String): Configuracao =
        repository.findByIdOrNull(chave)
            ?: throw NaoEncontradoException("SETTING_NOT_FOUND", "Configuração '$chave' não encontrada.")

    /** Lê uma configuração numérica. As chaves usadas pelo código vêm da migration V4. */
    fun inteiro(chave: String): Int =
        repository.findByIdOrNull(chave)?.valor?.toInt()
            ?: error("Configuração '$chave' não encontrada. A migration V4 foi aplicada?")

    fun atualizar(chave: String, valor: Int): Configuracao {
        val configuracao = buscar(chave)
        configuracao.valor = valor.toString()
        return repository.save(configuracao)
    }
}

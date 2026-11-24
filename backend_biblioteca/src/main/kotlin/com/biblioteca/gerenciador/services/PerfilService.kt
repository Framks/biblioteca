package com.biblioteca.gerenciador.services

import com.biblioteca.gerenciador.domain.Perfil
import com.biblioteca.gerenciador.domain.Permissao
import com.biblioteca.gerenciador.exceptions.NaoEncontradoException
import com.biblioteca.gerenciador.exceptions.NegocioException
import com.biblioteca.gerenciador.exceptions.RequisicaoInvalidaException
import com.biblioteca.gerenciador.repositories.PerfilRepository
import com.biblioteca.gerenciador.repositories.PermissaoRepository
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

/** Perfis e permissões (RF18, seção 7). */
@Service
class PerfilService(
    private val repository: PerfilRepository,
    private val permissaoRepository: PermissaoRepository,
) {

    /** Os perfis padrão vêm da migration V3; se faltar, é erro de instalação, não do usuário. */
    fun buscarPorNome(nome: String): Perfil =
        repository.findByNome(nome) ?: error("Perfil $nome não encontrado. A migration V3 foi aplicada?")

    fun buscar(id: Long): Perfil =
        repository.findByIdOrNull(id) ?: throw NaoEncontradoException("ROLE_NOT_FOUND", "Perfil $id não encontrado.")

    fun listar(): List<Perfil> = repository.findAll(Sort.by("id"))

    fun listarPermissoes(): List<Permissao> = permissaoRepository.findAllByOrderByCodigoAsc()

    fun atualizarPermissoes(perfil: Perfil, codigos: Set<String>): Perfil {
        if (!perfil.editavel) throw NegocioException("ROLE_NOT_EDITABLE", "As permissões do perfil ${perfil.nome} não podem ser alteradas.")
        val encontradas = if (codigos.isEmpty()) emptyList() else permissaoRepository.findByCodigoIn(codigos)
        val desconhecidas = codigos - encontradas.map { it.codigo }.toSet()
        if (desconhecidas.isNotEmpty()) {
            throw RequisicaoInvalidaException("UNKNOWN_PERMISSION", "Permissões inexistentes: ${desconhecidas.sorted().joinToString()}.")
        }
        perfil.substituirPermissoes(encontradas)
        return repository.save(perfil)
    }
}

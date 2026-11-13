package com.biblioteca.gerenciador.services

import com.biblioteca.gerenciador.domain.Perfil
import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.enums.StatusRegistro
import com.biblioteca.gerenciador.exceptions.ConflitoException
import com.biblioteca.gerenciador.exceptions.NaoEncontradoException
import com.biblioteca.gerenciador.repositories.UsuarioRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

/** Regras e operações de uma única entidade: Usuario (leitores, funcionários e administradores). */
@Service
class UsuarioService(
    private val repository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    fun pesquisarLeitores(busca: String?, pageable: Pageable): Page<Usuario> {
        val termo = busca?.trim()?.lowercase().orEmpty()
        val digitos = termo.filter(Char::isLetterOrDigit).uppercase()
        return repository.pesquisar(
            perfil = Perfil.LEITOR,
            padraoTexto = if (termo.isEmpty()) "%" else "%$termo%",
            // Sem letras/dígitos no termo, o documento não deve casar com tudo: "" só casa com documento vazio.
            padraoDocumento = if (termo.isEmpty()) "%" else if (digitos.isEmpty()) "" else "%$digitos%",
            status = StatusRegistro.ATIVO,
            pageable = pageable,
        )
    }

    /** Leitores inativos e usuários de outros perfis são tratados como inexistentes. */
    fun buscarLeitor(id: Long): Usuario =
        repository.findByIdAndPerfilNomeAndStatus(id, Perfil.LEITOR, StatusRegistro.ATIVO)
            ?: throw NaoEncontradoException("READER_NOT_FOUND", "Leitor $id não encontrado.")

    fun salvar(usuario: Usuario): Usuario {
        validarEmailUnico(usuario)
        validarDocumentoUnico(usuario)
        return repository.save(usuario)
    }

    /** RF08: guarda só o hash BCrypt da senha (RNF03). */
    fun definirSenha(usuario: Usuario, senha: String): Usuario {
        usuario.definirSenha(passwordEncoder.encode(senha)!!)
        return repository.save(usuario)
    }

    fun inativar(usuario: Usuario) {
        usuario.inativar()
        repository.save(usuario)
    }

    private fun validarEmailUnico(usuario: Usuario) {
        val email = usuario.email ?: return
        val duplicado = usuario.id
            ?.let { repository.existsByEmailAndIdNot(email, it) }
            ?: repository.existsByEmail(email)
        if (duplicado) throw ConflitoException("EMAIL_ALREADY_EXISTS", "Já existe um cadastro com o e-mail $email.")
    }

    private fun validarDocumentoUnico(usuario: Usuario) {
        val tipo = usuario.tipoDocumento ?: return
        val documento = usuario.documento ?: return
        val duplicado = usuario.id
            ?.let { repository.existsByTipoDocumentoAndDocumentoAndIdNot(tipo, documento, it) }
            ?: repository.existsByTipoDocumentoAndDocumento(tipo, documento)
        if (duplicado) throw ConflitoException("DOCUMENT_ALREADY_EXISTS", "Já existe um cadastro com este $tipo.")
    }
}

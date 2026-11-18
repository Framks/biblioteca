package com.biblioteca.gerenciador.rest.requests

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/** POST /auth/login (RF15). */
data class LoginRequest(
    @field:NotBlank(message = "é obrigatório")
    val email: String = "",

    @field:NotBlank(message = "é obrigatória")
    val senha: String = "",
)

/** PUT /auth/password: troca a própria senha. */
data class AlterarSenhaRequest(
    @field:NotBlank(message = "é obrigatória")
    val senhaAtual: String = "",

    @field:Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres")
    val novaSenha: String = "",
)

/** POST /users (RF17): funcionário ou administrador. */
data class CriarUsuarioRequest(
    @field:NotBlank(message = "é obrigatório")
    @field:Size(max = 255, message = "deve ter no máximo 255 caracteres")
    val nome: String = "",

    @field:NotBlank(message = "é obrigatório")
    @field:Email(message = "e-mail inválido")
    @field:Size(max = 255, message = "deve ter no máximo 255 caracteres")
    val email: String = "",

    @field:Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres")
    val senha: String = "",

    @field:Pattern(regexp = "FUNCIONARIO|ADMIN", message = "deve ser FUNCIONARIO ou ADMIN")
    val perfil: String = "",

    @field:Size(max = 20, message = "deve ter no máximo 20 caracteres")
    val telefone: String? = null,
)

/** PUT /users/{id} */
data class AtualizarUsuarioRequest(
    @field:NotBlank(message = "é obrigatório")
    @field:Size(max = 255, message = "deve ter no máximo 255 caracteres")
    val nome: String = "",

    @field:NotBlank(message = "é obrigatório")
    @field:Email(message = "e-mail inválido")
    @field:Size(max = 255, message = "deve ter no máximo 255 caracteres")
    val email: String = "",

    @field:Pattern(regexp = "FUNCIONARIO|ADMIN", message = "deve ser FUNCIONARIO ou ADMIN")
    val perfil: String = "",

    @field:Size(max = 20, message = "deve ter no máximo 20 caracteres")
    val telefone: String? = null,
)

/** PUT /users/{id}/password: o administrador redefine a senha de alguém. */
data class RedefinirSenhaRequest(
    @field:Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres")
    val senha: String = "",
)

/** PUT /roles/{id}/permissions (RF18): lista completa de códigos (substitui a atual). */
data class PermissoesPerfilRequest(
    val permissoes: Set<String> = emptySet(),
)

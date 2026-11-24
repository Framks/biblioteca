package com.biblioteca.gerenciador.security

import java.time.Instant
import java.util.UUID

/** Quem fez a requisição atual. Montado pelo JwtAutenticacaoFiltro e guardado no SecurityContext. */
data class UsuarioAutenticado(
    val usuarioId: Long,
    val sessaoId: UUID,
    val jti: String,
    val tokenExpiraEm: Instant,
    val perfil: String,
    val permissoes: Set<String>,
) {
    fun possui(permissao: String) = permissao in permissoes
}

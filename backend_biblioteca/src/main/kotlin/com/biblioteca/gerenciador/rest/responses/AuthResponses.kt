package com.biblioteca.gerenciador.rest.responses

import java.time.Instant
import java.util.UUID

/** POST /auth/login e /auth/refresh. O refresh token não aparece aqui: vai no cookie HttpOnly. */
data class LoginResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    /** Quando o access token expira; antes disso, chame POST /auth/refresh. */
    val expiraEm: Instant,
    /** Segundos até expirar (mesma informação de [expiraEm]). */
    val expiresIn: Long,
    val usuario: UsuarioLogadoResponse,
)

/** GET /auth/me: dados do usuário logado e o que ele pode fazer (o frontend usa para montar menus). */
data class UsuarioLogadoResponse(
    val id: Long,
    val nome: String,
    val email: String?,
    val perfil: String,
    val permissoes: List<String>,
)

data class SessaoResponse(
    val id: UUID,
    val ip: String,
    val userAgent: String,
    val criadoEm: Instant,
    val ultimoAcesso: Instant,
    val expiraEm: Instant,
    /** É a sessão desta requisição. */
    val atual: Boolean,
)

/** Funcionário ou administrador (GET /users). */
data class UsuarioResponse(
    val id: Long,
    val nome: String,
    val email: String?,
    val telefone: String?,
    val perfil: String,
    /** Preenchido se o login estiver bloqueado por tentativas erradas (seção 6.1). */
    val bloqueadoAte: Instant?,
    val dataCadastro: Instant,
    val dataAtualizacao: Instant,
)

data class PerfilResponse(
    val id: Long,
    val nome: String,
    val descricao: String?,
    val editavel: Boolean,
    val permissoes: List<String>,
)

data class PermissaoResponse(
    val codigo: String,
    val descricao: String,
)

data class AuditoriaResponse(
    val id: Long,
    val dataHora: Instant,
    val acao: String,
    val usuario: PessoaResumoResponse?,
    val entidade: String?,
    val entidadeId: Long?,
    val detalhes: String?,
    val ip: String?,
)

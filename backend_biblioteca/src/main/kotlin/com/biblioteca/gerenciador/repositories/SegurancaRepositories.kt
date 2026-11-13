package com.biblioteca.gerenciador.repositories

import com.biblioteca.gerenciador.domain.Auditoria
import com.biblioteca.gerenciador.domain.Permissao
import com.biblioteca.gerenciador.domain.RefreshToken
import com.biblioteca.gerenciador.domain.Sessao
import com.biblioteca.gerenciador.domain.TokenRevogado
import com.biblioteca.gerenciador.domain.Usuario
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface SessaoRepository : JpaRepository<Sessao, UUID> {

    fun findByUsuarioAndRevogadaEmIsNullAndExpiraEmAfterOrderByCriadoEmDesc(usuario: Usuario, agora: Instant): List<Sessao>
}

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {

    fun findByTokenHash(tokenHash: String): RefreshToken?

    @Modifying
    @Query("delete from RefreshToken r where r.expiraEm < :agora")
    fun apagarExpirados(agora: Instant): Int
}

interface TokenRevogadoRepository : JpaRepository<TokenRevogado, String> {

    @Modifying
    @Query("delete from TokenRevogado t where t.expiraEm < :agora")
    fun apagarExpirados(agora: Instant): Int
}

interface PermissaoRepository : JpaRepository<Permissao, Long> {

    fun findAllByOrderByCodigoAsc(): List<Permissao>

    fun findByCodigoIn(codigos: Collection<String>): List<Permissao>
}

interface AuditoriaRepository : JpaRepository<Auditoria, Long> {

    /** Filtros nulos são ignorados; [de] e [ate] nunca são nulos (ver LivroRepository sobre parâmetros nulos no PostgreSQL). */
    @Query(
        """
        select a from Auditoria a
        where (:acao is null or a.acao = :acao)
          and (:usuarioId is null or a.usuario.id = :usuarioId)
          and a.dataHora >= :de and a.dataHora < :ate
        """,
    )
    fun pesquisar(acao: String?, usuarioId: Long?, de: Instant, ate: Instant, pageable: Pageable): Page<Auditoria>
}

package com.biblioteca.gerenciador.security

import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.services.ConfiguracaoService
import com.biblioteca.gerenciador.services.Relogio
import com.nimbusds.jose.jwk.source.ImmutableSecret
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.crypto.spec.SecretKeySpec

/** Access token JWT assinado com HS256 (seção 6.2). */
@Component
class TokenService(
    @Value("\${app.seguranca.jwt-segredo}") segredo: String,
    private val configuracao: ConfiguracaoService,
    private val relogio: Relogio,
    clock: Clock,
) {
    init {
        require(segredo.length >= 32) { "app.seguranca.jwt-segredo precisa ter pelo menos 32 caracteres (256 bits)." }
    }

    private val chave = SecretKeySpec(segredo.toByteArray(), "HmacSHA256")
    private val encoder = NimbusJwtEncoder(ImmutableSecret(chave))
    private val decoder = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build().apply {
        // Valida exp/nbf com o mesmo Clock da aplicação (fixo nos testes), sem tolerância.
        setJwtValidator(JwtTimestampValidator(Duration.ZERO).apply { setClock(clock) })
    }

    /** Claims: sub (usuário), sid (sessão), jti (id do token), perfil, iat, exp. */
    fun emitir(usuario: Usuario, sessaoId: UUID): AccessToken {
        val agora = relogio.agora()
        val expiraEm = agora.plus(Duration.ofMinutes(configuracao.inteiro(ACCESS_MINUTOS).toLong()))
        val jti = UUID.randomUUID().toString()
        val claims = JwtClaimsSet.builder()
            .issuer(EMISSOR)
            .subject(usuario.id.toString())
            .issuedAt(agora)
            .expiresAt(expiraEm)
            .id(jti)
            .claim(CLAIM_SESSAO, sessaoId.toString())
            .claim(CLAIM_PERFIL, usuario.perfil.nome)
            .build()
        val token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        return AccessToken(token.tokenValue, jti, expiraEm)
    }

    /** Confere assinatura e validade. Lança [JwtException] se o token for inválido ou estiver expirado. */
    fun validar(token: String): Jwt = decoder.decode(token)

    data class AccessToken(val valor: String, val jti: String, val expiraEm: Instant)

    companion object {
        const val CLAIM_SESSAO = "sid"
        const val CLAIM_PERFIL = "perfil"
        private const val EMISSOR = "biblioteca"
        private const val ACCESS_MINUTOS = "jwt.access_minutos"
    }
}

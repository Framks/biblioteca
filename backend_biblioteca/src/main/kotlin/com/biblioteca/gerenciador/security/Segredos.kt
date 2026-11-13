package com.biblioteca.gerenciador.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** Geração e hash de valores secretos (refresh token, device_id). */
object Segredos {

    private val aleatorio = SecureRandom()

    /** 256 bits aleatórios em Base64 URL-safe (cabe em cookie sem escape). */
    fun gerar(): String {
        val bytes = ByteArray(32).also(aleatorio::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /**
     * SHA-256 em hexadecimal. Para valores aleatórios de 256 bits um hash rápido basta
     * (BCrypt é para senhas, que têm pouca entropia).
     */
    fun sha256(valor: String): String =
        MessageDigest.getInstance("SHA-256").digest(valor.toByteArray()).joinToString("") { "%02x".format(it) }
}

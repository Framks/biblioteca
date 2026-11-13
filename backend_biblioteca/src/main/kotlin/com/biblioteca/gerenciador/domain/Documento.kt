package com.biblioteca.gerenciador.domain

import com.biblioteca.gerenciador.enums.TipoDocumento

/** Normalização e validação de documentos de identificação (RN14). */
object Documento {

    /** CPF: só dígitos ("529.982.247-25" -> "52998224725"). RG: letras maiúsculas e dígitos. */
    fun normalizar(tipo: TipoDocumento, valor: String): String = when (tipo) {
        TipoDocumento.CPF -> valor.filter(Char::isDigit)
        TipoDocumento.RG -> valor.uppercase().filter(Char::isLetterOrDigit)
    }

    /** Recebe o valor já normalizado. */
    fun valido(tipo: TipoDocumento, valor: String): Boolean = when (tipo) {
        TipoDocumento.CPF -> cpfValido(valor)
        TipoDocumento.RG -> valor.length in 5..14
    }

    /** Confere os dois dígitos verificadores. Rejeita sequências repetidas (111.111.111-11). */
    fun cpfValido(cpf: String): Boolean {
        if (cpf.length != 11 || !cpf.all(Char::isDigit) || cpf.all { it == cpf[0] }) return false
        val d = cpf.map(Char::digitToInt)
        val dv1 = (0..8).sumOf { d[it] * (10 - it) } * 10 % 11 % 10
        val dv2 = (0..9).sumOf { d[it] * (11 - it) } * 10 % 11 % 10
        return d[9] == dv1 && d[10] == dv2
    }
}

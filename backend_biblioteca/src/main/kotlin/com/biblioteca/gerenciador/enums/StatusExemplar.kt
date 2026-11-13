package com.biblioteca.gerenciador.enums

/**
 * Ciclo de vida do exemplar (padrão State com enum): cada status sabe para quais pode ir.
 *
 *   DISPONIVEL ──► EMPRESTADO ──► DISPONIVEL / MANUTENCAO / BAIXADO
 *       │  ▲
 *       ▼  │
 *   MANUTENCAO ──► BAIXADO   (BAIXADO é final: não volta a circular, RN16)
 */
enum class StatusExemplar {
    DISPONIVEL,
    EMPRESTADO,
    MANUTENCAO,
    BAIXADO;

    fun podeMudarPara(novo: StatusExemplar): Boolean = novo in proximos()

    private fun proximos(): Set<StatusExemplar> = when (this) {
        DISPONIVEL -> setOf(EMPRESTADO, MANUTENCAO, BAIXADO)
        EMPRESTADO -> setOf(DISPONIVEL, MANUTENCAO, BAIXADO)
        MANUTENCAO -> setOf(DISPONIVEL, BAIXADO)
        BAIXADO -> emptySet()
    }
}

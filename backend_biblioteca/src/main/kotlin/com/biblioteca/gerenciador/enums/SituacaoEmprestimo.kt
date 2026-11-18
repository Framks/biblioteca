package com.biblioteca.gerenciador.enums

/**
 * Situação calculada na consulta, a partir das datas (RN06). Vale para o item e para o empréstimo:
 * um empréstimo está ATRASADO se qualquer item dele estiver atrasado.
 */
enum class SituacaoEmprestimo {
    /** Não devolvido e dentro do prazo. */
    ATIVO,

    /** Não devolvido e a data prevista já passou. */
    ATRASADO,

    /** Devolvido. */
    DEVOLVIDO,
}

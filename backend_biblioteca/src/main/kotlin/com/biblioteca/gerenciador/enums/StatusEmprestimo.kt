package com.biblioteca.gerenciador.enums

/** Status gravado do empréstimo. "Atrasado" não é gravado: é calculado (ver SituacaoEmprestimo, RN06). */
enum class StatusEmprestimo {
    /** Ainda há item não devolvido. */
    ABERTO,

    /** Todos os itens foram devolvidos (RN09). */
    ENCERRADO,
}

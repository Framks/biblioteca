package com.biblioteca.gerenciador.enums

/** Por que uma sessão foi encerrada (gravado em sessao.motivo_revogacao e na auditoria). */
enum class MotivoRevogacao {
    LOGOUT,
    ENCERRADA_PELO_USUARIO,
    ENCERRADA_PELO_ADMIN,
    SENHA_ALTERADA,
    USUARIO_INATIVADO,

    /** 6.3: refresh token já usado foi apresentado de novo (provável cópia). */
    REUSO_REFRESH_TOKEN,

    /** 6.5: token usado sem o cookie de dispositivo da sessão (ex.: copiado para outra máquina). */
    DISPOSITIVO_DIFERENTE,

    /** 6.5: o navegador (User-Agent) mudou no meio da sessão. */
    NAVEGADOR_DIFERENTE,
}

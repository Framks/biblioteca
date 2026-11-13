package com.biblioteca.gerenciador.services

import com.biblioteca.gerenciador.domain.Auditoria
import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.enums.AcaoAuditoria
import com.biblioteca.gerenciador.repositories.AuditoriaRepository
import com.biblioteca.gerenciador.repositories.UsuarioRepository
import com.biblioteca.gerenciador.security.UsuarioAutenticado
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Instant

/**
 * Trilha de auditoria (RF21). O registro entra na mesma transação da operação:
 * se a operação falhar e for desfeita, o registro também é (só fica o que de fato aconteceu).
 */
@Service
class AuditoriaService(
    private val repository: AuditoriaRepository,
    private val usuarioRepository: UsuarioRepository,
    private val relogio: Relogio,
) {

    /** [usuario] padrão: quem está logado na requisição atual (nulo se anônimo). */
    fun registrar(
        acao: AcaoAuditoria,
        entidade: String? = null,
        entidadeId: Long? = null,
        detalhes: String? = null,
        usuario: Usuario? = usuarioDaRequisicao(),
    ) {
        repository.save(
            Auditoria(
                usuario = usuario,
                acao = acao.name,
                entidade = entidade,
                entidadeId = entidadeId,
                detalhes = detalhes?.take(1000),
                ip = ipDaRequisicao(),
                dataHora = relogio.agora(),
            ),
        )
    }

    fun pesquisar(acao: AcaoAuditoria?, usuarioId: Long?, de: Instant, ate: Instant, pageable: Pageable): Page<Auditoria> =
        repository.pesquisar(acao?.name, usuarioId, de, ate, pageable)

    private fun usuarioDaRequisicao(): Usuario? {
        val principal = SecurityContextHolder.getContext().authentication?.principal as? UsuarioAutenticado ?: return null
        return usuarioRepository.getReferenceById(principal.usuarioId)
    }

    private fun ipDaRequisicao(): String? =
        (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request?.remoteAddr
}

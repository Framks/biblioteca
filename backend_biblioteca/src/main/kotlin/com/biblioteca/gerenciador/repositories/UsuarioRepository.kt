package com.biblioteca.gerenciador.repositories

import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.enums.StatusRegistro
import com.biblioteca.gerenciador.enums.TipoDocumento
import jakarta.persistence.QueryHint
import org.hibernate.jpa.HibernateHints
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints

interface UsuarioRepository : JpaRepository<Usuario, Long> {

    fun findByEmail(email: String): Usuario?

    fun findByIdAndPerfilNomeAndStatus(id: Long, perfil: String, status: StatusRegistro): Usuario?

    // Consultas de unicidade não podem disparar o auto-flush (ver comentário em LivroRepository).
    @QueryHints(QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
    fun existsByEmail(email: String): Boolean

    @QueryHints(QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
    fun existsByEmailAndIdNot(email: String, id: Long): Boolean

    @QueryHints(QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
    fun existsByTipoDocumentoAndDocumento(tipo: TipoDocumento, documento: String): Boolean

    @QueryHints(QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
    fun existsByTipoDocumentoAndDocumentoAndIdNot(tipo: TipoDocumento, documento: String, id: Long): Boolean

    /**
     * Pesquisa por um único termo, que pode ser parte do nome, do e-mail ou do documento.
     * [padraoTexto] e [padraoDocumento] nunca são nulos (ver LivroRepository); "%" = qualquer.
     */
    @Query(
        """
        select u from Usuario u
        where u.perfil.nome = :perfil
          and u.status = :status
          and (lower(u.nome) like :padraoTexto
            or lower(u.email) like :padraoTexto
            or u.documento like :padraoDocumento)
        """,
    )
    fun pesquisar(
        perfil: String,
        padraoTexto: String,
        padraoDocumento: String,
        status: StatusRegistro,
        pageable: Pageable,
    ): Page<Usuario>
}

package com.biblioteca.gerenciador.domain

import com.biblioteca.gerenciador.enums.StatusRegistro
import com.biblioteca.gerenciador.enums.TipoDocumento
import com.biblioteca.gerenciador.exceptions.NegocioException
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * Qualquer pessoa cadastrada: leitor, funcionário ou administrador (diferenciados pelo perfil).
 * Leitor comum não tem senha; o login dele é opcional (RF08).
 */
@Entity
@Table(name = "usuario")
class Usuario(
    var nome: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perfil_id")
    var perfil: Perfil,

    var email: String? = null,

    @Enumerated(EnumType.STRING)
    var tipoDocumento: TipoDocumento? = null,

    var documento: String? = null,
    var telefone: String? = null,
    var endereco: String? = null,
) : EntidadeAuditada() {

    var senhaHash: String? = null
        protected set

    @Enumerated(EnumType.STRING)
    var status: StatusRegistro = StatusRegistro.ATIVO
        protected set

    /** RN07: preenchido quando o leitor devolve com atraso. */
    var suspensoAte: LocalDate? = null
        protected set

    /** 6.1: tentativas de login erradas seguidas. */
    var tentativasLoginFalhas: Int = 0
        protected set

    /** 6.1: login bloqueado até este instante. */
    var bloqueadoAte: Instant? = null
        protected set

    val acessoHabilitado: Boolean
        get() = senhaHash != null

    val ativo: Boolean
        get() = status == StatusRegistro.ATIVO

    fun estaBloqueado(agora: Instant): Boolean = bloqueadoAte?.isAfter(agora) ?: false

    /** 6.1: conta a falha; ao atingir [maxTentativas], bloqueia por [minutosBloqueio]. Retorna true se bloqueou agora. */
    fun registrarFalhaLogin(maxTentativas: Int, minutosBloqueio: Int, agora: Instant): Boolean {
        tentativasLoginFalhas++
        if (tentativasLoginFalhas < maxTentativas) return false
        bloqueadoAte = agora.plus(Duration.ofMinutes(minutosBloqueio.toLong()))
        tentativasLoginFalhas = 0
        return true
    }

    fun registrarLoginComSucesso() {
        tentativasLoginFalhas = 0
        bloqueadoAte = null
    }

    fun alterarPerfil(novoPerfil: Perfil) {
        perfil = novoPerfil
    }

    fun atualizarDados(
        nome: String,
        email: String?,
        tipoDocumento: TipoDocumento?,
        documento: String?,
        telefone: String?,
        endereco: String?,
    ) {
        if (email == null && acessoHabilitado) {
            throw NegocioException("EMAIL_REQUIRED_FOR_ACCESS", "Usuário com acesso ao sistema precisa ter e-mail.")
        }
        this.nome = nome
        this.email = email
        this.tipoDocumento = tipoDocumento
        this.documento = documento
        this.telefone = telefone
        this.endereco = endereco
    }

    /** RF08: o login usa o e-mail, então ele é obrigatório para habilitar o acesso. */
    fun definirSenha(senhaHash: String) {
        if (email == null) {
            throw NegocioException("EMAIL_REQUIRED_FOR_ACCESS", "Cadastre um e-mail antes de habilitar o acesso.")
        }
        this.senhaHash = senhaHash
    }

    /** [suspensoAte] é o último dia da suspensão (inclusive). */
    fun estaSuspenso(hoje: LocalDate): Boolean = suspensoAte?.let { !hoje.isAfter(it) } ?: false

    /**
     * RN07: suspende por [dias] dias a contar de hoje (hoje conta como o 1º dia).
     * Nunca encurta uma suspensão que já termina depois.
     */
    fun suspenderPor(dias: Long, hoje: LocalDate) {
        if (dias <= 0) return
        val ultimoDia = hoje.plusDays(dias - 1)
        if (suspensoAte == null || ultimoDia.isAfter(suspensoAte)) suspensoAte = ultimoDia
    }

    /** Exclusão lógica (RN12). A RN13 (sem empréstimo pendente) é conferida no LeitorFacade. */
    fun inativar() {
        status = StatusRegistro.INATIVO
    }
}

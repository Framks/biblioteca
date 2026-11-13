package com.biblioteca.gerenciador.config

import com.biblioteca.gerenciador.config.ClockFixoConfig.Companion.AGORA
import com.biblioteca.gerenciador.config.ClockFixoConfig.Companion.HOJE
import com.biblioteca.gerenciador.domain.Emprestimo
import com.biblioteca.gerenciador.domain.Exemplar
import com.biblioteca.gerenciador.domain.Genero
import com.biblioteca.gerenciador.domain.Livro
import com.biblioteca.gerenciador.domain.Perfil
import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.enums.CondicaoExemplar
import com.biblioteca.gerenciador.enums.StatusExemplar
import com.biblioteca.gerenciador.enums.TipoDocumento
import com.biblioteca.gerenciador.repositories.EmprestimoRepository
import com.biblioteca.gerenciador.repositories.ExemplarRepository
import com.biblioteca.gerenciador.repositories.GeneroRepository
import com.biblioteca.gerenciador.repositories.LivroRepository
import com.biblioteca.gerenciador.repositories.PerfilRepository
import com.biblioteca.gerenciador.repositories.UsuarioRepository
import org.springframework.boot.test.context.TestComponent
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.time.LocalDate

/** Fábrica de dados para preparar o cenário ("dado que...") direto no banco. */
@TestComponent
class DadosTeste(
    private val generoRepository: GeneroRepository,
    private val livroRepository: LivroRepository,
    private val exemplarRepository: ExemplarRepository,
    private val usuarioRepository: UsuarioRepository,
    private val perfilRepository: PerfilRepository,
    private val emprestimoRepository: EmprestimoRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    fun genero(nome: String = "Romance"): Genero = generoRepository.save(Genero(nome))

    fun livro(
        titulo: String = "Dom Casmurro",
        autor: String = "Machado de Assis",
        isbn: String? = null,
        generos: Set<Genero> = setOf(genero("Genero ${System.nanoTime()}")),
    ): Livro = livroRepository.save(
        Livro(titulo = titulo, autor = autor, isbn = isbn, generos = generos.toMutableSet()),
    )

    /** Cria um exemplar e o leva até o [status] desejado pelos métodos de domínio (respeitando as transições). */
    fun exemplar(
        livro: Livro,
        status: StatusExemplar = StatusExemplar.DISPONIVEL,
        condicao: CondicaoExemplar = CondicaoExemplar.CONSERVADO,
        codigo: String = "TESTE-${System.nanoTime()}",
    ): Exemplar {
        val exemplar = Exemplar(codigo = codigo, livro = livro, condicao = condicao)
        when (status) {
            StatusExemplar.DISPONIVEL -> Unit
            StatusExemplar.EMPRESTADO -> exemplar.emprestar()
            else -> exemplar.atualizar(null, null, status)
        }
        return exemplarRepository.save(exemplar)
    }

    fun leitor(
        nome: String = "Maria da Silva",
        cpf: String = CPFS_VALIDOS.first(),
        email: String? = null,
    ): Usuario = usuarioRepository.save(
        Usuario(
            nome = nome,
            perfil = perfilRepository.findByNome(Perfil.LEITOR)!!,
            email = email,
            tipoDocumento = TipoDocumento.CPF,
            documento = cpf,
        ),
    )

    /**
     * Empréstimo já registrado. Use [dataPrevista] no passado para simular atraso
     * (ex.: HOJE.minusDays(3) = 3 dias atrasado).
     */
    fun emprestimo(
        leitor: Usuario,
        vararg exemplares: Exemplar,
        dataPrevista: LocalDate = HOJE.plusDays(14),
        dataEmprestimo: Instant = AGORA,
    ): Emprestimo {
        val emprestimo = Emprestimo(leitor = leitor, operador = admin(), dataEmprestimo = dataEmprestimo)
        exemplares.forEach { emprestimo.adicionarItem(it, dataPrevista) }
        return emprestimoRepository.save(emprestimo)
    }

    /** Usuário com login habilitado (leitor, funcionário ou administrador). */
    fun usuarioComAcesso(
        perfil: String,
        email: String,
        senha: String = "senha-correta-123",
        nome: String = "Usuário $perfil",
        cpf: String? = null,
    ): Usuario {
        val usuario = Usuario(
            nome = nome,
            perfil = perfilRepository.findByNome(perfil)!!,
            email = email,
            tipoDocumento = cpf?.let { TipoDocumento.CPF },
            documento = cpf,
        )
        usuario.definirSenha(passwordEncoder.encode(senha)!!)
        return usuarioRepository.save(usuario)
    }

    /** Suspende o leitor por [dias] dias a partir de hoje. */
    fun suspender(leitor: Usuario, dias: Long): Usuario {
        leitor.suspenderPor(dias, HOJE)
        return usuarioRepository.save(leitor)
    }

    fun admin(): Usuario = usuarioRepository.findByEmail("admin@biblioteca.local")!!

    companion object {
        /** CPFs com dígitos verificadores corretos, para os testes. */
        val CPFS_VALIDOS = listOf("52998224725", "11144477735", "12345678909", "93541134780", "71460238001")
    }
}

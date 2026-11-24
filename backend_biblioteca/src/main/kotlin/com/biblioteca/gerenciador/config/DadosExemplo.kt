package com.biblioteca.gerenciador.config

import com.biblioteca.gerenciador.domain.Documento
import com.biblioteca.gerenciador.domain.Exemplar
import com.biblioteca.gerenciador.domain.Genero
import com.biblioteca.gerenciador.domain.Livro
import com.biblioteca.gerenciador.domain.Perfil
import com.biblioteca.gerenciador.domain.Usuario
import com.biblioteca.gerenciador.enums.CondicaoExemplar
import com.biblioteca.gerenciador.enums.StatusExemplar
import com.biblioteca.gerenciador.enums.TipoDocumento
import com.biblioteca.gerenciador.services.EmprestimoService
import com.biblioteca.gerenciador.services.ExemplarService
import com.biblioteca.gerenciador.services.GeneroService
import com.biblioteca.gerenciador.services.LivroService
import com.biblioteca.gerenciador.services.PerfilService
import com.biblioteca.gerenciador.services.Relogio
import com.biblioteca.gerenciador.services.UsuarioService
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate

/**
 * Dados de exemplo para usar o sistema localmente: uma conta de cada tipo, acervo, leitores e empréstimos
 * em situações variadas (no prazo, atrasado, devolvido, leitor suspenso).
 *
 * Só roda com app.dados-exemplo=true (perfil dev e ./run.sh --postgres); nunca nos testes nem em produção.
 * É criado uma única vez: se a conta do funcionário de exemplo já existir, não faz nada.
 * Usa os services e as regras do domínio, então os dados respeitam as mesmas regras da API.
 * As datas são relativas ao dia em que o banco foi criado.
 */
@Component
@ConditionalOnProperty(name = ["app.dados-exemplo"], havingValue = "true")
@Order(Ordered.LOWEST_PRECEDENCE)
class DadosExemplo(
    private val generoService: GeneroService,
    private val livroService: LivroService,
    private val exemplarService: ExemplarService,
    private val usuarioService: UsuarioService,
    private val perfilService: PerfilService,
    private val emprestimoService: EmprestimoService,
    private val relogio: Relogio,
    private val transacao: TransactionTemplate,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        criarSeNecessario()
    }

    /** Retorna true se criou os dados; false se eles já existiam. */
    fun criarSeNecessario(): Boolean = transacao.execute {
        if (usuarioService.buscarPorEmail(EMAIL_FUNCIONARIO) != null) {
            log.info("Dados de exemplo já existem; nada a fazer.")
            return@execute false
        }
        Criacao().executar()
        log.info(
            "Dados de exemplo criados. Contas: {} / {} (admin), {} / {} (funcionário), {} / {} (leitor).",
            EMAIL_ADMIN, "senha do ADMIN_SENHA_INICIAL", EMAIL_FUNCIONARIO, SENHA_FUNCIONARIO, EMAIL_LEITOR, SENHA_LEITOR,
        )
        true
    }!!

    /** Uma execução da criação (guarda o "hoje" e o admin usados em todos os registros). */
    private inner class Criacao {
        val hoje: LocalDate = relogio.hoje()
        val admin: Usuario = usuarioService.buscarPorEmail(EMAIL_ADMIN)!!
        val perfilLeitor = perfilService.buscarPorNome(Perfil.LEITOR)

        fun executar() {
            // ---------- contas ----------
            val funcionario = usuarioService.salvar(
                Usuario(nome = "Fernanda Lima", perfil = perfilService.buscarPorNome(Perfil.FUNCIONARIO), email = EMAIL_FUNCIONARIO, telefone = "(88) 99999-1000"),
            )
            usuarioService.definirSenha(funcionario, SENHA_FUNCIONARIO)

            val lucas = leitor("Lucas Almeida", TipoDocumento.CPF, "529.982.247-25", email = EMAIL_LEITOR, senha = SENHA_LEITOR)
            val ana = leitor("Ana Souza", TipoDocumento.CPF, "111.444.777-35", email = "ana.souza@email.com", telefone = "(88) 98888-0001")
            val bruno = leitor("Bruno Lima", TipoDocumento.CPF, "123.456.789-09", telefone = "(88) 98888-0002")
            val carla = leitor("Carla Mendes", TipoDocumento.CPF, "935.411.347-80", email = "carla.mendes@email.com")
            val diego = leitor("Diego Rocha", TipoDocumento.RG, "20.123.456-7 SSP")

            // ---------- acervo ----------
            val g = listOf("Romance", "Clássico", "Ficção Científica", "Fantasia", "Infantil", "Poesia", "Suspense", "Biografia")
                .associateWith { generoService.salvar(Genero(it)) }

            val domCasmurro = livro("Dom Casmurro", "Machado de Assis", "Garnier", 1899, 3, g["Romance"], g["Clássico"], isbn = "978-85-7232-001-0")
            val memorias = livro("Memórias Póstumas de Brás Cubas", "Machado de Assis", "Tipografia Nacional", 1881, 2, g["Romance"], g["Clássico"])
            val alienista = livro("O Alienista", "Machado de Assis", "Lombaerts", 1882, 1, g["Clássico"], g["Suspense"])
            val iracema = livro("Iracema", "José de Alencar", "Typ. Vianna", 1865, 2, g["Romance"], g["Clássico"])
            val cortico = livro("O Cortiço", "Aluísio Azevedo", "Garnier", 1890, 1, g["Romance"], g["Clássico"])
            val vidasSecas = livro("Vidas Secas", "Graciliano Ramos", "José Olympio", 1938, 2, g["Romance"], g["Clássico"])
            livro("A Hora da Estrela", "Clarice Lispector", "Rocco", 1977, 1, g["Romance"])
            val duna = livro("Duna", "Frank Herbert", "Aleph", 1965, 2, g["Ficção Científica"], isbn = "978-85-7657-313-0")
            val mil984 = livro("1984", "George Orwell", "Companhia das Letras", 1949, 2, g["Ficção Científica"], g["Clássico"])
            val hobbit = livro("O Hobbit", "J. R. R. Tolkien", "HarperCollins", 1937, 2, g["Fantasia"], g["Infantil"])
            val principe = livro("O Pequeno Príncipe", "Antoine de Saint-Exupéry", "Agir", 1943, 2, g["Infantil"], g["Clássico"])
            val eu = livro("Eu", "Augusto dos Anjos", "Edição do autor", 1912, 1, g["Poesia"])

            mudarStatus(alienista[0], StatusExemplar.MANUTENCAO) // em manutenção
            mudarStatus(eu[0], StatusExemplar.BAIXADO)            // extraviado: livro sem exemplar disponível

            // ---------- empréstimos (em ordem cronológica) ----------
            // Histórico já devolvido, em meses anteriores (alimenta o relatório por mês).
            devolvido(diego, hobbit[0], emprestadoHa = 100, devolvidoHa = 92, condicao = CondicaoExemplar.DANIFICADO) // foi para manutenção
            devolvido(lucas, iracema[0], emprestadoHa = 70, devolvidoHa = 60)
            devolvido(ana, duna[0], emprestadoHa = 45, devolvidoHa = 33)
            devolvido(bruno, domCasmurro[0], emprestadoHa = 40, devolvidoHa = 30)
            devolvido(diego, mil984[1], emprestadoHa = 25, devolvidoHa = 20)

            // Carla devolveu 3 dias atrasada hoje: fica suspensa por 3 dias (RN07).
            val daCarla = emprestar(carla, vidasSecas[0], ha = 17)
            emprestimoService.devolver(daCarla, listOf(daCarla.itens[0].id!! to CondicaoExemplar.CONSERVADO), admin, hoje, relogio.agora())

            // Bruno está com 2 exemplares 5 dias atrasados (O Cortiço fica sem exemplar disponível).
            emprestar(bruno, mil984[0], cortico[0], ha = 19, observacao = "Leitor avisado por telefone sobre o atraso.")

            // Lucas (conta de leitor): um empréstimo que vence em 2 dias e outro já renovado uma vez.
            emprestar(lucas, hobbit[1], ha = 12)
            val renovado = emprestar(lucas, memorias[0], ha = 5)
            emprestimoService.renovar(renovado.itens[0], hoje)

            // Ana: empréstimo recente com dois livros, no prazo.
            emprestar(ana, domCasmurro[1], principe[0], ha = 3)
        }

        fun leitor(
            nome: String,
            tipo: TipoDocumento,
            documento: String,
            email: String? = null,
            telefone: String? = null,
            senha: String? = null,
        ): Usuario {
            val leitor = usuarioService.salvar(
                Usuario(
                    nome = nome,
                    perfil = perfilLeitor,
                    email = email,
                    tipoDocumento = tipo,
                    documento = Documento.normalizar(tipo, documento),
                    telefone = telefone,
                    endereco = "Quixadá - CE",
                ),
            )
            senha?.let { usuarioService.definirSenha(leitor, it) }
            return leitor
        }

        fun livro(titulo: String, autor: String, editora: String, ano: Int, exemplares: Int, vararg generos: Genero?, isbn: String? = null): List<Exemplar> {
            val livro = livroService.salvar(
                Livro(
                    titulo = titulo,
                    autor = autor,
                    isbn = isbn,
                    editora = editora,
                    anoPublicacao = ano,
                    generos = generos.filterNotNull().toMutableSet(),
                    criadoPor = admin,
                ),
            )
            return exemplarService.criar(livro, exemplares, CondicaoExemplar.CONSERVADO)
        }

        fun mudarStatus(exemplar: Exemplar, status: StatusExemplar) {
            exemplar.atualizar(novoCodigo = null, novaCondicao = null, novoStatus = status)
            exemplarService.salvar(exemplar)
        }

        /** Empréstimo registrado [ha] dias atrás, com o prazo padrão (14 dias). */
        fun emprestar(leitor: Usuario, vararg exemplares: Exemplar, ha: Long, observacao: String? = null) =
            emprestimoService.registrar(
                leitor = leitor,
                operador = admin,
                exemplares = exemplares.toList(),
                prazoDias = null,
                observacao = observacao,
                hoje = hoje.minusDays(ha),
                agora = instante(hoje.minusDays(ha)),
            )

        fun devolvido(leitor: Usuario, exemplar: Exemplar, emprestadoHa: Long, devolvidoHa: Long, condicao: CondicaoExemplar = CondicaoExemplar.CONSERVADO) {
            val emprestimo = emprestar(leitor, exemplar, ha = emprestadoHa)
            val dia = hoje.minusDays(devolvidoHa)
            emprestimoService.devolver(emprestimo, listOf(emprestimo.itens[0].id!! to condicao), admin, dia, instante(dia))
        }

        /** 10h da manhã do [dia], no fuso da biblioteca. */
        fun instante(dia: LocalDate) = dia.atTime(10, 0).atZone(relogio.zona).toInstant()
    }

    companion object {
        const val EMAIL_ADMIN = "admin@biblioteca.local"
        const val EMAIL_FUNCIONARIO = "funcionario@biblioteca.local"
        const val SENHA_FUNCIONARIO = "func12345"
        const val EMAIL_LEITOR = "leitor@biblioteca.local"
        const val SENHA_LEITOR = "leitor12345"
    }
}

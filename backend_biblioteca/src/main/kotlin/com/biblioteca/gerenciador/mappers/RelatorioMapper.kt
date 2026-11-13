package com.biblioteca.gerenciador.mappers

import com.biblioteca.gerenciador.repositories.EmprestimoItemRepository.LivroMaisEmprestado
import com.biblioteca.gerenciador.rest.responses.EmprestimosPorMesResponse
import com.biblioteca.gerenciador.rest.responses.LeitorEmAtrasoResponse
import com.biblioteca.gerenciador.rest.responses.LivroMaisEmprestadoResponse
import com.biblioteca.gerenciador.rest.responses.ResumoCirculacaoResponse
import com.biblioteca.gerenciador.services.RelatorioService.LeitorEmAtraso
import com.biblioteca.gerenciador.services.RelatorioService.ResumoCirculacao
import java.time.LocalDate
import java.time.YearMonth

fun ResumoCirculacao.toResponse(hoje: LocalDate) = ResumoCirculacaoResponse(
    dataReferencia = hoje,
    emprestimosAtivos = emprestimosAtivos,
    emprestimosAtrasados = emprestimosAtrasados,
    emprestimosNoPrazo = emprestimosNoPrazo,
    exemplaresEmprestados = exemplaresEmprestados,
    exemplaresAtrasados = exemplaresAtrasados,
    leitoresComAtraso = leitoresComAtraso,
    leitoresSuspensos = leitoresSuspensos,
    emprestimosNoMes = emprestimosNoMes,
)

fun Pair<YearMonth, Long>.toResponse() = EmprestimosPorMesResponse(mes = first.toString(), quantidade = second)

fun List<LivroMaisEmprestado>.toResponse() = mapIndexed { i, l ->
    LivroMaisEmprestadoResponse(posicao = i + 1, livroId = l.livroId, titulo = l.titulo, autor = l.autor, emprestimos = l.quantidade)
}

fun LeitorEmAtraso.toResponse() = LeitorEmAtrasoResponse(
    leitorId = leitor.id!!,
    nome = leitor.nome,
    email = leitor.email,
    telefone = leitor.telefone,
    exemplaresAtrasados = itens.size,
    maiorAtrasoDias = maiorAtrasoDias,
    vencidoDesde = vencidoDesde,
    titulos = itens.map { it.exemplar.livro.titulo }.sorted(),
)

// ---------- CSV (RF14) ----------

fun ResumoCirculacaoResponse.toCsv() = Csv.gerar(
    listOf("indicador", "valor"),
    listOf(
        listOf("data de referência", dataReferencia),
        listOf("empréstimos ativos", emprestimosAtivos),
        listOf("empréstimos atrasados", emprestimosAtrasados),
        listOf("empréstimos no prazo", emprestimosNoPrazo),
        listOf("exemplares emprestados", exemplaresEmprestados),
        listOf("exemplares atrasados", exemplaresAtrasados),
        listOf("leitores com atraso", leitoresComAtraso),
        listOf("leitores suspensos", leitoresSuspensos),
        listOf("empréstimos no mês", emprestimosNoMes),
    ),
)

@JvmName("porMesToCsv")
fun List<EmprestimosPorMesResponse>.toCsv() =
    Csv.gerar(listOf("mês", "empréstimos"), map { listOf(it.mes, it.quantidade) })

@JvmName("maisEmprestadosToCsv")
fun List<LivroMaisEmprestadoResponse>.toCsv() =
    Csv.gerar(listOf("posição", "título", "autor", "empréstimos"), map { listOf(it.posicao, it.titulo, it.autor, it.emprestimos) })

@JvmName("leitoresEmAtrasoToCsv")
fun List<LeitorEmAtrasoResponse>.toCsv() = Csv.gerar(
    listOf("leitor", "e-mail", "telefone", "exemplares atrasados", "maior atraso (dias)", "vencido desde", "títulos"),
    map { listOf(it.nome, it.email, it.telefone, it.exemplaresAtrasados, it.maiorAtrasoDias, it.vencidoDesde, it.titulos.joinToString(" | ")) },
)

/**
 * CSV no formato que o Excel em português abre direto: separador ";", UTF-8 com BOM (acentos corretos),
 * aspas em volta dos campos que contêm ";", aspas ou quebra de linha.
 */
object Csv {
    private const val SEPARADOR = ";"
    private val BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

    fun gerar(cabecalho: List<String>, linhas: List<List<Any?>>): ByteArray {
        val texto = (listOf(cabecalho) + linhas).joinToString("\r\n", postfix = "\r\n") { linha ->
            linha.joinToString(SEPARADOR) { escapar(it?.toString().orEmpty()) }
        }
        return BOM + texto.toByteArray(Charsets.UTF_8)
    }

    private fun escapar(valor: String): String =
        if (valor.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) "\"" + valor.replace("\"", "\"\"") + "\"" else valor
}

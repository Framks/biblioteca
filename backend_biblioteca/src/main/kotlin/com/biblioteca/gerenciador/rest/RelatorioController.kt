package com.biblioteca.gerenciador.rest

import com.biblioteca.gerenciador.facades.RelatorioFacade
import com.biblioteca.gerenciador.mappers.toCsv
import com.biblioteca.gerenciador.rest.responses.EmprestimosPorMesResponse
import com.biblioteca.gerenciador.rest.responses.LeitorEmAtrasoResponse
import com.biblioteca.gerenciador.rest.responses.LivroMaisEmprestadoResponse
import com.biblioteca.gerenciador.rest.responses.ResumoCirculacaoResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.YearMonth

/** Relatórios de circulação (RF13) e exportação em CSV (RF14). */
@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasAuthority('report_read')")
class RelatorioController(private val facade: RelatorioFacade) {

    @GetMapping("/summary")
    fun resumo(): ResumoCirculacaoResponse = facade.resumo()

    /** GET /reports/loans-per-month?inicio=2026-01&fim=2026-12 (padrão: últimos 12 meses). */
    @GetMapping("/loans-per-month")
    fun porMes(
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") inicio: YearMonth?,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") fim: YearMonth?,
    ): List<EmprestimosPorMesResponse> = facade.porMes(inicio, fim)

    /** GET /reports/top-books?de=2026-01-01&ate=2026-12-31&limite=10 (sem datas: todo o histórico). */
    @GetMapping("/top-books")
    fun maisEmprestados(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) de: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) ate: LocalDate?,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) limite: Int,
    ): List<LivroMaisEmprestadoResponse> = facade.maisEmprestados(de, ate, limite)

    @GetMapping("/overdue-readers")
    fun leitoresEmAtraso(): List<LeitorEmAtrasoResponse> = facade.leitoresEmAtraso()

    // ---------- exportação CSV (RF14): mesmos parâmetros dos relatórios acima ----------

    @GetMapping("/summary/export")
    fun exportarResumo() = csv("resumo-circulacao", facade.resumo().toCsv())

    @GetMapping("/loans-per-month/export")
    fun exportarPorMes(
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") inicio: YearMonth?,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") fim: YearMonth?,
    ) = csv("emprestimos-por-mes", facade.porMes(inicio, fim).toCsv())

    @GetMapping("/top-books/export")
    fun exportarMaisEmprestados(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) de: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) ate: LocalDate?,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) limite: Int,
    ) = csv("livros-mais-emprestados", facade.maisEmprestados(de, ate, limite).toCsv())

    @GetMapping("/overdue-readers/export")
    fun exportarLeitoresEmAtraso() = csv("leitores-em-atraso", facade.leitoresEmAtraso().toCsv())

    private fun csv(nome: String, conteudo: ByteArray): ResponseEntity<ByteArray> =
        ResponseEntity.ok()
            .contentType(MediaType("text", "csv", Charsets.UTF_8))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename("$nome.csv").build().toString(),
            )
            .body(conteudo)
}

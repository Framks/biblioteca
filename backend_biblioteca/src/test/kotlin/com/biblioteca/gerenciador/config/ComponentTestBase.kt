package com.biblioteca.gerenciador.config

import com.biblioteca.gerenciador.domain.Usuario
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.transaction.annotation.Transactional

/**
 * Base dos testes de componente: sobe a aplicação inteira com H2 em memória
 * e testa pela API (entrada HTTP -> saída HTTP).
 * Cada teste roda numa transação desfeita ao final, então um teste não enxerga os dados do outro.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ClockFixoConfig::class, DadosTeste::class, SegurancaTesteConfig::class, CredenciaisTeste::class)
@Transactional
abstract class ComponentTestBase {

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var dados: DadosTeste

    @Autowired
    protected lateinit var credenciais: CredenciaisTeste

    /** Faz a requisição como [usuario] (sessão nova). Ex.: mockMvc.get(url) { with(comoUsuario(leitor)) } */
    protected fun comoUsuario(usuario: Usuario): RequestPostProcessor = credenciais.criar(usuario).aplicar()

    /** Faz a requisição sem login (visitante). */
    protected fun anonimo(): RequestPostProcessor = CredenciaisTeste.anonimo()

    protected fun postJson(url: String, json: String, vararg extras: RequestPostProcessor) = mockMvc.post(url) {
        contentType = MediaType.APPLICATION_JSON
        content = json
        extras.forEach { with(it) }
    }

    protected fun putJson(url: String, json: String, vararg extras: RequestPostProcessor) = mockMvc.put(url) {
        contentType = MediaType.APPLICATION_JSON
        content = json
        extras.forEach { with(it) }
    }
}

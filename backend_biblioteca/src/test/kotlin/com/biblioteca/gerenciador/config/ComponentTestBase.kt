package com.biblioteca.gerenciador.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.transaction.annotation.Transactional

/**
 * Base dos testes de componente: sobe a aplicação inteira com H2 em memória
 * e testa pela API (entrada HTTP -> saída HTTP).
 * Cada teste roda numa transação desfeita ao final, então um teste não enxerga os dados do outro.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ClockFixoConfig::class, DadosTeste::class)
@Transactional
abstract class ComponentTestBase {

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var dados: DadosTeste

    protected fun postJson(url: String, json: String) = mockMvc.post(url) {
        contentType = MediaType.APPLICATION_JSON
        content = json
    }

    protected fun putJson(url: String, json: String) = mockMvc.put(url) {
        contentType = MediaType.APPLICATION_JSON
        content = json
    }
}

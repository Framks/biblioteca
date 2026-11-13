package com.biblioteca.gerenciador.leitor

import com.biblioteca.gerenciador.config.ComponentTestBase
import com.biblioteca.gerenciador.config.DadosTeste.Companion.CPFS_VALIDOS
import com.biblioteca.gerenciador.repositories.UsuarioRepository
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LeitorComponentTest : ComponentTestBase() {

    @Autowired
    private lateinit var usuarioRepository: UsuarioRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    // ---------- cadastro ----------

    @Test
    fun `cadastra leitor normalizando cpf e email`() {
        postJson(
            "/api/v1/readers",
            """
            {
              "nome": "  Maria da Silva ",
              "documento": "529.982.247-25",
              "email": "Maria@Email.COM",
              "telefone": "(88) 99999-0000",
              "endereco": "Rua A, 10 - Quixadá"
            }
            """,
        ).andExpect {
            status { isCreated() }
            header { exists("Location") }
            jsonPath("$.nome") { value("Maria da Silva") }
            jsonPath("$.tipoDocumento") { value("CPF") }
            jsonPath("$.documento") { value("52998224725") }
            jsonPath("$.email") { value("maria@email.com") }
            jsonPath("$.acessoHabilitado") { value(false) }
            jsonPath("$.suspenso") { value(false) }
            jsonPath("$.senhaHash") { doesNotExist() }
        }
    }

    @Test
    fun `cadastra leitor com RG`() {
        postJson("/api/v1/readers", """{ "nome": "João", "tipoDocumento": "RG", "documento": "20.123.456-7 SSP" }""")
            .andExpect {
                status { isCreated() }
                jsonPath("$.documento") { value("201234567SSP") }
            }
    }

    @Test
    fun `cpf com digito verificador errado retorna 400 no campo documento`() {
        postJson("/api/v1/readers", """{ "nome": "Maria", "documento": "529.982.247-26" }""").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_ERROR") }
            jsonPath("$.fields[0].field") { value("documento") }
            jsonPath("$.fields[0].message") { value("CPF inválido") }
        }
    }

    @Test
    fun `cpf com todos os digitos iguais e invalido`() {
        postJson("/api/v1/readers", """{ "nome": "Maria", "documento": "111.111.111-11" }""").andExpect {
            status { isBadRequest() }
            jsonPath("$.fields[0].field") { value("documento") }
        }
    }

    @Test
    fun `valida obrigatorios e formato do email`() {
        postJson("/api/v1/readers", """{ "email": "nao-e-email" }""").andExpect {
            status { isBadRequest() }
            jsonPath("$.fields[*].field") { value(org.hamcrest.Matchers.containsInAnyOrder("nome", "documento", "email")) }
        }
    }

    @Test
    fun `nao permite cpf repetido, mesmo com outra formatacao`() {
        dados.leitor(cpf = "52998224725")

        postJson("/api/v1/readers", """{ "nome": "Outra", "documento": "529 982 247 25" }""").andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("DOCUMENT_ALREADY_EXISTS") }
        }
    }

    @Test
    fun `nao permite email repetido, ignorando maiusculas`() {
        dados.leitor(cpf = CPFS_VALIDOS[0], email = "ana@email.com")

        postJson("/api/v1/readers", """{ "nome": "Outra", "documento": "${CPFS_VALIDOS[1]}", "email": "ANA@email.com" }""")
            .andExpect {
                status { isConflict() }
                jsonPath("$.code") { value("EMAIL_ALREADY_EXISTS") }
            }
    }

    // ---------- consulta ----------

    @Test
    fun `pesquisa por nome, documento ou email e nao lista funcionarios`() {
        dados.leitor(nome = "Ana Souza", cpf = CPFS_VALIDOS[0], email = "ana@email.com")
        dados.leitor(nome = "Bruno Lima", cpf = CPFS_VALIDOS[1])
        dados.leitor(nome = "Carla Souza", cpf = CPFS_VALIDOS[2])

        mockMvc.get("/api/v1/readers").andExpect {
            jsonPath("$.content[*].nome") { value(contains("Ana Souza", "Bruno Lima", "Carla Souza")) } // sem o Administrador
        }
        mockMvc.get("/api/v1/readers?busca=souza").andExpect {
            jsonPath("$.content[*].nome") { value(contains("Ana Souza", "Carla Souza")) }
        }
        mockMvc.get("/api/v1/readers?busca=111.444").andExpect { // trecho do CPF 111.444.777-35, formatado
            jsonPath("$.content[*].nome") { value(contains("Bruno Lima")) }
        }
        mockMvc.get("/api/v1/readers?busca=ANA@EMAIL").andExpect {
            jsonPath("$.content[*].nome") { value(contains("Ana Souza")) }
        }
    }

    @Test
    fun `busca so com letras nao casa com todos os documentos`() {
        dados.leitor(nome = "Ana", cpf = CPFS_VALIDOS[0])

        mockMvc.get("/api/v1/readers?busca=zzz").andExpect {
            jsonPath("$.totalElements") { value(0) }
        }
    }

    @Test
    fun `leitor inexistente retorna 404`() {
        mockMvc.get("/api/v1/readers/9999").andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("READER_NOT_FOUND") }
        }
    }

    @Test
    fun `administrador nao e acessivel como leitor`() {
        val admin = usuarioRepository.findByEmail("admin@biblioteca.local")!!

        mockMvc.get("/api/v1/readers/${admin.id}").andExpect { status { isNotFound() } }
    }

    // ---------- atualização e inativação ----------

    @Test
    fun `atualiza dados do leitor mantendo o proprio cpf`() {
        val leitor = dados.leitor(nome = "Nome antigo", cpf = CPFS_VALIDOS[0])

        putJson("/api/v1/readers/${leitor.id}", """{ "nome": "Nome novo", "documento": "${CPFS_VALIDOS[0]}", "telefone": "8899" }""")
            .andExpect {
                status { isOk() }
                jsonPath("$.nome") { value("Nome novo") }
                jsonPath("$.telefone") { value("8899") }
            }
    }

    @Test
    fun `atualizar para cpf de outro leitor retorna 409`() {
        dados.leitor(cpf = CPFS_VALIDOS[0])
        val leitor = dados.leitor(cpf = CPFS_VALIDOS[1])

        putJson("/api/v1/readers/${leitor.id}", """{ "nome": "X", "documento": "${CPFS_VALIDOS[0]}" }""").andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("DOCUMENT_ALREADY_EXISTS") }
        }
    }

    @Test
    fun `leitor inativado some das consultas`() {
        val leitor = dados.leitor(nome = "Inativo")

        mockMvc.delete("/api/v1/readers/${leitor.id}").andExpect { status { isNoContent() } }

        mockMvc.get("/api/v1/readers/${leitor.id}").andExpect { status { isNotFound() } }
        mockMvc.get("/api/v1/readers?busca=inativo").andExpect { jsonPath("$.totalElements") { value(0) } }
    }

    // ---------- acesso (RF08) ----------

    @Test
    fun `habilita acesso guardando apenas o hash da senha`() {
        val leitor = dados.leitor(email = "leitor@email.com")

        postJson("/api/v1/readers/${leitor.id}/access", """{ "senha": "senhaForte123" }""").andExpect {
            status { isOk() }
            jsonPath("$.acessoHabilitado") { value(true) }
        }

        val hash = usuarioRepository.findById(leitor.id!!).get().senhaHash!!
        assertNotEquals("senhaForte123", hash)
        assertTrue(hash.startsWith("$2"), "deveria ser um hash BCrypt")
        assertTrue(passwordEncoder.matches("senhaForte123", hash))
    }

    @Test
    fun `nao habilita acesso sem email`() {
        val leitor = dados.leitor(email = null)

        postJson("/api/v1/readers/${leitor.id}/access", """{ "senha": "senhaForte123" }""").andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("EMAIL_REQUIRED_FOR_ACCESS") }
        }
    }

    @Test
    fun `senha curta retorna 400`() {
        val leitor = dados.leitor(email = "leitor@email.com")

        postJson("/api/v1/readers/${leitor.id}/access", """{ "senha": "123" }""").andExpect {
            status { isBadRequest() }
            jsonPath("$.fields[0].field") { value("senha") }
        }
    }

    @Test
    fun `leitor com acesso nao pode ficar sem email`() {
        val leitor = dados.leitor(cpf = CPFS_VALIDOS[0], email = "leitor@email.com")
        postJson("/api/v1/readers/${leitor.id}/access", """{ "senha": "senhaForte123" }""")

        putJson("/api/v1/readers/${leitor.id}", """{ "nome": "X", "documento": "${CPFS_VALIDOS[0]}" }""").andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("EMAIL_REQUIRED_FOR_ACCESS") }
        }
    }
}

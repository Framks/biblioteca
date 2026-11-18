package com.biblioteca.gerenciador.administracao

import com.biblioteca.gerenciador.config.ComponentTestBase
import com.biblioteca.gerenciador.config.DadosTeste.Companion.CPFS_VALIDOS
import com.biblioteca.gerenciador.domain.Perfil
import com.biblioteca.gerenciador.repositories.PerfilRepository
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get

/** Usuários da equipe (RF17), perfis (RF18) e auditoria (RF21). */
class AdministracaoComponentTest : ComponentTestBase() {

    @Autowired
    private lateinit var perfilRepository: PerfilRepository

    private fun login(email: String, senha: String) =
        postJson("/api/v1/auth/login", """{ "email": "$email", "senha": "$senha" }""", anonimo())

    // ---------- usuários da equipe ----------

    @Test
    fun `cadastra funcionario que ja consegue fazer login`() {
        postJson(
            "/api/v1/users",
            """{ "nome": "Carlos", "email": "Carlos@Biblioteca.local", "senha": "senha-do-carlos", "perfil": "FUNCIONARIO" }""",
        ).andExpect {
            status { isCreated() }
            jsonPath("$.email") { value("carlos@biblioteca.local") }
            jsonPath("$.perfil") { value("FUNCIONARIO") }
            jsonPath("$.senhaHash") { doesNotExist() }
        }

        login("carlos@biblioteca.local", "senha-do-carlos").andExpect {
            status { isOk() }
            jsonPath("$.usuario.permissoes") { value(hasItem("loan_create")) }
        }
    }

    @Test
    fun `valida dados do novo usuario`() {
        postJson("/api/v1/users", """{ "nome": "X", "email": "x@x.com", "senha": "curta", "perfil": "LEITOR" }""").andExpect {
            status { isBadRequest() }
            jsonPath("$.fields[*].field") { value(org.hamcrest.Matchers.containsInAnyOrder("senha", "perfil")) }
        }
        postJson("/api/v1/users", """{ "nome": "X", "email": "admin@biblioteca.local", "senha": "senha-boa-123", "perfil": "ADMIN" }""")
            .andExpect {
                status { isConflict() }
                jsonPath("$.code") { value("EMAIL_ALREADY_EXISTS") }
            }
    }

    @Test
    fun `lista so funcionarios e administradores`() {
        dados.usuarioComAcesso(Perfil.FUNCIONARIO, "f@b.local", nome = "Fernanda")
        dados.leitor(nome = "Leitor Comum", cpf = CPFS_VALIDOS[0])

        mockMvc.get("/api/v1/users").andExpect {
            jsonPath("$.content[*].nome") { value(contains("Administrador", "Fernanda")) }
        }
        mockMvc.get("/api/v1/users?busca=fern").andExpect { jsonPath("$.totalElements") { value(1) } }
    }

    @Test
    fun `promove funcionario a administrador`() {
        val f = dados.usuarioComAcesso(Perfil.FUNCIONARIO, "f@b.local", nome = "Fernanda")

        putJson("/api/v1/users/${f.id}", """{ "nome": "Fernanda", "email": "f@b.local", "perfil": "ADMIN" }""").andExpect {
            status { isOk() }
            jsonPath("$.perfil") { value("ADMIN") }
        }
    }

    @Test
    fun `nao inativa a si mesmo nem deixa o sistema sem administrador`() {
        val admin = dados.admin()

        mockMvc.delete("/api/v1/users/${admin.id}").andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("CANNOT_DEACTIVATE_SELF") }
        }
        putJson("/api/v1/users/${admin.id}", """{ "nome": "Adm", "email": "admin@biblioteca.local", "perfil": "FUNCIONARIO" }""")
            .andExpect {
                status { isUnprocessableContent() }
                jsonPath("$.code") { value("LAST_ADMIN") }
            }
    }

    @Test
    fun `inativar usuario derruba as sessoes dele`() {
        val f = dados.usuarioComAcesso(Perfil.FUNCIONARIO, "f@b.local", nome = "Fernanda")
        val sessaoDele = credenciais.criar(f)

        mockMvc.delete("/api/v1/users/${f.id}").andExpect { status { isNoContent() } }

        mockMvc.get("/api/v1/auth/me") { with(sessaoDele.aplicar()) }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("SESSION_REVOKED") }
        }
        login("f@b.local", "senha-correta-123").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `redefinir senha derruba as sessoes e libera a nova senha`() {
        val f = dados.usuarioComAcesso(Perfil.FUNCIONARIO, "f@b.local", nome = "Fernanda")
        val sessaoDele = credenciais.criar(f)

        putJson("/api/v1/users/${f.id}/password", """{ "senha": "senha-nova-789" }""").andExpect { status { isNoContent() } }

        mockMvc.get("/api/v1/auth/me") { with(sessaoDele.aplicar()) }.andExpect { status { isUnauthorized() } }
        login("f@b.local", "senha-nova-789").andExpect { status { isOk() } }
    }

    @Test
    fun `leitor nao e acessivel em users`() {
        val leitor = dados.leitor(cpf = CPFS_VALIDOS[0])

        mockMvc.get("/api/v1/users/${leitor.id}").andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("USER_NOT_FOUND") }
        }
    }

    // ---------- perfis e permissões ----------

    @Test
    fun `lista perfis e permissoes da matriz da secao 7`() {
        mockMvc.get("/api/v1/roles").andExpect {
            status { isOk() }
            jsonPath("$[*].nome") { value(contains("VISITANTE", "LEITOR", "FUNCIONARIO", "ADMIN")) }
            jsonPath("$[?(@.nome == 'ADMIN')].editavel") { value(contains(false)) }
            jsonPath("$[?(@.nome == 'LEITOR')].permissoes[*]") {
                value(contains("book_read", "loan_read_own", "loan_renew_own", "session_manage_own"))
            }
        }
        mockMvc.get("/api/v1/permissions").andExpect { jsonPath("$.length()") { value(24) } }
    }

    @Test
    fun `perfil ADMIN nao pode ser alterado e codigo inexistente e recusado`() {
        val admin = perfilRepository.findByNome(Perfil.ADMIN)!!
        val leitor = perfilRepository.findByNome(Perfil.LEITOR)!!

        putJson("/api/v1/roles/${admin.id}/permissions", """{ "permissoes": [] }""").andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("ROLE_NOT_EDITABLE") }
        }
        putJson("/api/v1/roles/${leitor.id}/permissions", """{ "permissoes": ["book_read", "voar"] }""").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("UNKNOWN_PERMISSION") }
        }
    }

    // ---------- auditoria ----------

    @Test
    fun `operacoes ficam registradas com usuario e entidade`() {
        val genero = dados.genero("Poesia")
        val criado = postJson("/api/v1/books", """{ "titulo": "Auditado", "autor": "X", "generoIds": [${genero.id}] }""")
            .andReturn().response.contentAsString
        val livroId = com.jayway.jsonpath.JsonPath.read<Int>(criado, "$.id")

        mockMvc.get("/api/v1/audit?acao=LIVRO_CRIADO").andExpect {
            status { isOk() }
            jsonPath("$.content[0].usuario.nome") { value("Administrador") }
            jsonPath("$.content[0].entidade") { value("Livro") }
            jsonPath("$.content[0].entidadeId") { value(livroId) }
            jsonPath("$.content[0].detalhes") { value("Auditado") }
            jsonPath("$.content[0].ip") { value("127.0.0.1") }
        }
    }

    @Test
    fun `login com falha fica registrado mesmo sendo recusado`() {
        login("ninguem@x.com", "qualquer").andExpect { status { isUnauthorized() } }

        mockMvc.get("/api/v1/audit?acao=LOGIN_FALHA").andExpect {
            jsonPath("$.content[0].usuario") { value(null as Any?) }
            jsonPath("$.content[0].detalhes") { value("e-mail: ninguem@x.com") }
        }
    }

    @Test
    fun `filtra auditoria por usuario`() {
        val f = dados.usuarioComAcesso(Perfil.FUNCIONARIO, "f@b.local", nome = "Fernanda")
        postJson("/api/v1/genres", """{ "nome": "Feito pela Fernanda" }""", comoUsuario(f)).andExpect { status { isCreated() } }

        mockMvc.get("/api/v1/audit?usuarioId=${f.id}").andExpect {
            jsonPath("$.content[*].acao") { value(contains("GENERO_CRIADO")) }
        }
        mockMvc.get("/api/v1/audit?acao=GENERO_CRIADO&usuarioId=${dados.admin().id}").andExpect {
            jsonPath("$.content[*].detalhes") { value(not(hasItem("Feito pela Fernanda"))) }
        }
    }

    @Test
    fun `acao inexistente no filtro retorna 400`() {
        mockMvc.get("/api/v1/audit?acao=VOAR").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_PARAMETER") }
        }
    }
}

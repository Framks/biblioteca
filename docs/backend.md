# Backend: arquitetura, convenções e como rodar

API REST da biblioteca comunitária em **Kotlin + Spring Boot 4**. Os requisitos (RF, RN, RNF) estão em [`objetivo.md`](objetivo.md); o contrato de cada endpoint está na coleção do Postman em [`postman/`](postman/).

## 1. Como rodar

Pré-requisitos: JDK 21+. Docker é opcional (só para PostgreSQL).

```bash
./run.sh              # backend com H2 em arquivo (perfil dev) + frontend, se existir
./run.sh --postgres   # backend com PostgreSQL em Docker (perfil prod)
./run.sh --back       # só o backend
./run.sh --test       # roda os testes
```

- API: `http://localhost:8080/api/v1`
- Login inicial: `admin@biblioteca.local` / `admin12345` (troque em `PUT /auth/password`)
- Console do H2 (perfil dev): `http://localhost:8080/h2-console`, JDBC URL `jdbc:h2:file:./data/biblioteca`, usuário `sa`
- Para zerar o banco de dev, apague `backend_biblioteca/data/`.

Direto pelo Gradle, dentro de `backend_biblioteca/`: `./gradlew bootRun` (dev) e `./gradlew test`.

### Perfis e variáveis de ambiente

| Perfil | Banco | Uso |
|---|---|---|
| `dev` (padrão) | H2 em arquivo | desenvolvimento local |
| `test` | H2 em memória | testes (o CI também roda em PostgreSQL) |
| `prod` | PostgreSQL | produção / `./run.sh --postgres` |

| Variável (prod) | Obrigatória | Descrição |
|---|---|---|
| `JWT_SEGREDO` | sim | segredo HS256, 32+ caracteres |
| `ADMIN_SENHA_INICIAL` | na 1ª execução | senha do `admin@biblioteca.local` (só se ele ainda não tiver senha) |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | não | padrão `jdbc:postgresql://localhost:5432/biblioteca`, `biblioteca`/`biblioteca` |
| `COOKIE_SEGURO` | não | `true` (padrão) = cookies só por HTTPS |
| `CORS_ORIGENS` | não | origens do frontend, separadas por vírgula (padrão `http://localhost:5173`) |

## 2. Arquitetura

Camadas, com um pacote por tipo de classe:

```
com.biblioteca.gerenciador/
├── config/        Spring: segurança, CORS, BCrypt, Clock, senha do admin inicial
├── domain/        entidades JPA com as regras de negócio (modelo rico)
├── enums/         status, condições, ações de auditoria...
├── repositories/  Spring Data JPA
├── services/      operações e regras de UMA entidade (não conhecem DTOs)
├── facades/       ponto de entrada dos controllers: orquestra services, converte DTOs, define a transação
├── rest/          controllers
│   ├── requests/  DTOs de entrada (com Bean Validation)
│   └── responses/ DTOs de saída
├── mappers/       conversões DTO <-> entidade (extension functions) e CSV
├── security/      JWT, filtro de autenticação, cookies
└── exceptions/    exceções da API e o GlobalExceptionHandler
```

**Fluxo de uma requisição:** `Controller → Facade → Service(s) → Repository`

| Camada | Faz | Não faz |
|---|---|---|
| Controller | rota, `@Valid`, `@PreAuthorize`, status HTTP | regra de negócio |
| Facade | coordena services, converte DTO ↔ entidade, `@Transactional`, auditoria | acessar repository direto |
| Service | regras e consultas de uma entidade | conhecer DTOs |
| Entidade | regras do próprio objeto (`Exemplar.emprestar()`, `EmprestimoItem.renovar()`) | acessar banco |

Exemplo: `EmprestimoFacade.registrar()` busca o leitor (`UsuarioService`), os exemplares (`ExemplarService`) e o operador (`UsuarioLogado`), chama `EmprestimoService.registrar()`, que valida RN01–RN05, e grava a auditoria, tudo numa transação.

### Padrões de projeto usados

| Padrão | Onde |
|---|---|
| **Facade** | `facades/`: uma interface simples para cada caso de uso, escondendo vários services |
| **Repository** | `repositories/` (Spring Data) |
| **DTO + Mapper** | `rest/requests`, `rest/responses`, `mappers/`: a API não expõe entidades |
| **Modelo de domínio rico** | regras dentro das entidades: `Exemplar`, `Emprestimo`, `EmprestimoItem`, `Usuario`, `Sessao` |
| **State (com enum)** | `StatusExemplar.podeMudarPara()`: transições permitidas do exemplar |
| **Injeção de dependência / Singleton** | beans do Spring |
| **Strategy (leve)** | `UsuarioLogado` (interface) com a implementação que lê o JWT |
| **Template de resposta** | `ErroResponse` e `PageResponse`: formato único de erro e de paginação |

## 3. Convenções (siga ao criar endpoints novos)

### Erros
- Lance as exceções de `exceptions/ApiException.kt`: `NaoEncontradoException` (404), `ConflitoException` (409), `NegocioException` (422), `RequisicaoInvalidaException` (400), `NaoAutenticadoException` (401), `ProibidoException` (403).
- Cada uma tem um **código estável em inglês** (`BOOK_NOT_FOUND`) e uma **mensagem em português** para o usuário. O frontend decide pelo código.
- O `GlobalExceptionHandler` converte tudo no formato da seção 10.2 do `objetivo.md`.

### Requests
- Campos obrigatórios têm **valor padrão vazio** (`""`, `emptySet()`, `0` para ids com `@Positive`). Assim, um campo ausente no JSON vira `400 VALIDATION_ERROR` com o nome do campo, e não um erro genérico de leitura do JSON.
- Validação que depende de mais de um campo: anotação de classe (ver `@DocumentoValido`).

### Banco e JPA
- **Exclusão lógica** (`inativar()`); os services buscam só os ativos.
- **Migrations** em `src/main/resources/db/migration`, só SQL que funcione em **H2 e PostgreSQL** (sem `jsonb`, `serial`, arrays). Nunca altere uma migration que já foi para a `master`: crie outra.
- **Datas:** `Instant` (UTC) para momentos; `LocalDate` para prazos. O "hoje" vem do `Relogio` (fuso America/Fortaleza), nunca de `LocalDate.now()`.

### Armadilhas já encontradas (e como evitar)
1. **Parâmetro nulo em `lower(:param)`/`like` quebra no PostgreSQL** (`function lower(bytea) does not exist`), mas passa no H2. Não mande nulo nesses filtros: monte o padrão no service (`"%"` = qualquer). Ver `LivroRepository.pesquisar`.
2. **Checagem de unicidade depois de alterar a entidade** faz o Hibernate gravar a alteração antes da consulta (auto-flush), e a constraint estoura com 500 em vez de 409. Os `existsBy...` de unicidade têm `@QueryHints(flushMode = COMMIT)`.
3. **`sort` com campo inexistente em `@Query`** vira 500. Use `pageable.validarOrdenacao("campo1", "campo2")` no controller.
4. **Revogar e recusar na mesma transação:** um método que grava algo (bloqueio de login, revogação de sessão) e depois lança exceção precisa de `@Transactional(noRollbackFor = [ApiException::class])`, senão o rollback desfaz o que foi gravado.

### Auditoria
Toda operação que altera dados chama `auditoria.registrar(AcaoAuditoria.X, "Entidade", id, detalhes)` na facade. O usuário e o IP são preenchidos automaticamente.

## 4. Segurança (seção 6 do `objetivo.md`)

```
Login ──► sessão (presa ao device_id + User-Agent) ──► access token JWT (15 min) + refresh token (cookie, 7 dias)

Cada requisição: JwtAutenticacaoFiltro
  1. assinatura e validade do JWT ............ 401 INVALID_TOKEN
  2. jti fora da blocklist ................... 401 TOKEN_REVOKED
  3. sessão ativa ............................ 401 SESSION_REVOKED
  4. mesmo device_id e User-Agent do login ... senão revoga a sessão: 401 SESSION_REVOKED
  5. permissões do perfil (lidas do banco) ... @PreAuthorize: 403 FORBIDDEN
```

- **Rotas públicas:** `POST /auth/login`, `POST /auth/refresh`, `GET /catalog/**`, `GET /genres`. Todo o resto de `/api/**` exige login.
- **Permissões:** `@PreAuthorize("hasAuthority('book_create')")` em cada endpoint, com os códigos da seção 7. Como são lidas do banco a cada requisição, alterar um perfil vale na hora.
- **Usuário logado:** injete `UsuarioLogado` (`obter()` devolve a entidade; `autenticado()` devolve id, sessão e permissões sem consultar o banco).
- **Frontend:** guarde o access token em memória, envie `Authorization: Bearer ...` e use `credentials: "include"` (os cookies `refresh_token` e `device_id` são HttpOnly). Ao receber `401 INVALID_TOKEN`, chame `POST /auth/refresh` e repita a requisição; qualquer outro 401 → tela de login.

## 5. Testes

Testes de **componente**: sobem a aplicação inteira (H2 em memória) e testam pela API com MockMvc, passando pelo filtro de segurança de verdade.

```
src/test/kotlin/com/biblioteca/gerenciador/
├── config/        ComponentTestBase, ClockFixoConfig, DadosTeste, SegurancaTesteConfig
├── genero/ livro/ exemplar/ leitor/ emprestimo/ configuracao/
├── relatorio/ seguranca/ administracao/
src/test/resources/application-test.yaml
```

- Herde de `ComponentTestBase`. Cada teste roda numa transação desfeita no final.
- **Por padrão, toda requisição vai autenticada como o administrador.** Para outro usuário: `mockMvc.get(url) { with(comoUsuario(leitor)) }`; sem login: `with(anonimo())`.
- **Data fixa:** "hoje" é sempre 13/11/2026 (`ClockFixoConfig.HOJE`). Para simular atraso, crie o empréstimo com `dados.emprestimo(leitor, exemplar, dataPrevista = HOJE.minusDays(3))`.
- **Cenário:** use `dados` (`DadosTeste`): `genero()`, `livro()`, `exemplar()`, `leitor()`, `usuarioComAcesso()`, `emprestimo()`, `suspender()`.
- Limitação: como o teste e a requisição compartilham a transação, o rollback de uma requisição que falhou não é visível no teste.
- O CI (`.github/workflows/backend.yml`) roda a suíte em **H2 e em PostgreSQL**; os dois são obrigatórios para o merge. Para rodar em PostgreSQL localmente:
  ```bash
  SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/biblioteca \
  SPRING_DATASOURCE_USERNAME=biblioteca SPRING_DATASOURCE_PASSWORD=biblioteca ./gradlew test
  ```

## 6. Roteiro para uma funcionalidade nova

1. Migration `V<n>__descricao.sql` (SQL compatível com H2 e PostgreSQL).
2. Entidade em `domain/` com as regras como métodos; enums em `enums/`.
3. Repository; service com as regras da entidade.
4. Requests/responses em `rest/`, mapper em `mappers/`.
5. Facade com `@Transactional` e auditoria.
6. Controller com `@PreAuthorize`; se for ordenável, `validarOrdenacao`.
7. Testes de componente (caminho feliz, validação, cada regra, permissão).
8. Atualize a coleção do Postman e, se mudar regra, o `objetivo.md`.

# Trabalho final da disciplina de Linguagem de Programação

Sistema de gerenciamento de **biblioteca comunitária**.

## 1. Descrição geral

O sistema gerencia uma biblioteca comunitária: organização do acervo, controle de exemplares, empréstimos, devoluções e acompanhamento de prazos.

Público do sistema:

- **Visitante** (sem login): consulta o catálogo público e a disponibilidade dos livros.
- **Leitor** (login opcional): acompanha os próprios empréstimos, prazos e histórico; pode solicitar renovação.
- **Funcionário**: cadastra livros, exemplares e leitores; registra empréstimos, devoluções e renovações; consulta relatórios.
- **Administrador**: tudo o que o funcionário faz, mais gestão de usuários do sistema, perfis/permissões, configurações e auditoria.

Escopo: **uma única biblioteca**. As regras de negócio foram definidas pela equipe (liberado pelo professor).

## 2. Conceitos aplicados

- Programação Orientada a Objetos
- Persistência de dados
- Interface de usuário
- Regras de negócio
- Geração de relatórios
- Segurança (autenticação, autorização e gestão de sessão)

## 3. Requisitos funcionais (RF)

### Acervo

- **RF01 – Cadastro de livros:** cadastrar, editar, consultar e inativar livros com: título, autor, gênero(s), editora, ano de publicação e ISBN (opcional, único quando informado).
- **RF02 – Cadastro de gêneros:** cadastrar e listar gêneros literários. Um livro pode ter **um ou mais gêneros**.
- **RF03 – Cadastro de exemplares:** cada livro possui N exemplares físicos, cada um com **código único**, condição de conservação e status. A quantidade de exemplares disponíveis de um livro é **calculada** a partir dos exemplares (não é digitada).
- **RF04 – Consulta ao acervo:** pesquisar por título, autor, gênero e ISBN, com filtros:
  - livros com exemplares disponíveis;
  - livros sem exemplares disponíveis;
  - livros/exemplares emprestados.
- **RF05 – Catálogo público:** visitantes (sem login) podem pesquisar livros e ver a quantidade disponível, sem acesso a dados de leitores.

### Leitores

- **RF06 – Cadastro de leitores:** cadastrar, editar, consultar e inativar leitores com: nome, documento (CPF ou RG), e-mail, telefone e endereço.
- **RF07 – Histórico do leitor:** exibir todos os empréstimos (ativos, atrasados e devolvidos) de um leitor.
- **RF08 – Acesso do leitor:** o funcionário pode habilitar login para o leitor; o leitor logado vê apenas os próprios empréstimos e prazos.

### Circulação

- **RF09 – Registro de empréstimo:** registrar um empréstimo com:
  - leitor;
  - um ou mais exemplares;
  - data do empréstimo (automática);
  - prazo previsto de devolução (padrão configurável, podendo ser ajustado até o máximo permitido);
  - funcionário que registrou (automático).
- **RF10 – Devolução:** registrar a devolução de um ou mais exemplares de um empréstimo, informando a condição do exemplar na devolução. A disponibilidade é atualizada automaticamente.
- **RF11 – Renovação:** renovar o prazo de um item emprestado, respeitando as regras de renovação. Pode ser feita pelo funcionário ou pelo próprio leitor.
- **RF12 – Empréstimos em aberto:** listar empréstimos ativos e atrasados, com filtro por leitor, livro e período.

### Relatórios

- **RF13 – Relatórios de circulação:**
  - número de empréstimos ativos;
  - número de empréstimos atrasados;
  - quantidade de empréstimos realizados por mês (filtro por período);
  - livros mais emprestados (top N);
  - leitores com empréstimos em atraso.
- **RF14 – Exportação:** exportar os relatórios em CSV.

### Segurança e administração

- **RF15 – Login:** autenticação por e-mail e senha.
- **RF16 – Logout:** encerra a sessão e revoga os tokens.
- **RF17 – Gestão de usuários do sistema:** o administrador cadastra funcionários e atribui perfis.
- **RF18 – Perfis e permissões:** cada funcionalidade exige uma permissão; permissões são agrupadas em perfis (ver seção 7).
- **RF19 – Configurações:** o administrador ajusta parâmetros de negócio (prazos, limites etc.) sem alterar código.
- **RF20 – Gestão de sessões:** o usuário vê suas sessões ativas e pode encerrá-las; o administrador pode encerrar sessões de qualquer usuário.
- **RF21 – Auditoria:** ações relevantes (login, falha de login, sessão revogada, criação/edição/exclusão, empréstimo, devolução) ficam registradas com usuário, data e IP.

## 4. Regras de negócio (RN)

Os valores entre parênteses são o **padrão**, todos parametrizáveis na tabela `CONFIGURACAO`.

### Empréstimo

- **RN01:** um leitor pode ter no máximo (**3**) exemplares emprestados ao mesmo tempo.
- **RN02:** prazo padrão de devolução de (**14**) dias; o funcionário pode definir outro prazo até o máximo de (**30**) dias.
- **RN03:** só podem ser emprestados exemplares com status `disponivel`. Exemplares em `manutencao`, `emprestado` ou `baixado` não podem ser emprestados.
- **RN04:** um leitor não pode ter dois exemplares do **mesmo livro** ao mesmo tempo.
- **RN05:** leitor **inativo**, **suspenso** ou com **qualquer item em atraso** não pode realizar novo empréstimo nem renovação.

### Devolução e atraso

- **RN06:** um item está **atrasado** quando não foi devolvido e a data prevista já passou. O atraso é **calculado na consulta**, não gravado no banco.
- **RN07:** ao devolver com atraso, o leitor fica **suspenso** por (**1**) dia para cada dia de atraso. Não há multa em dinheiro.
- **RN08:** na devolução é registrada a condição do exemplar. Se a condição for `danificado`, o exemplar vai para `manutencao`; caso contrário, volta para `disponivel`.
- **RN09:** cada item de um empréstimo pode ser devolvido separadamente; o empréstimo é `encerrado` quando todos os itens forem devolvidos.

### Renovação

- **RN10:** cada item pode ser renovado no máximo (**2**) vezes, acrescentando o prazo padrão a partir da data da renovação.
- **RN11:** não é possível renovar um item já atrasado.

### Cadastros

- **RN12:** exclusão é sempre **lógica** (soft delete), preservando o histórico.
- **RN13:** não é possível inativar livro, exemplar ou leitor que tenha empréstimo ativo.
- **RN14:** documento (CPF/RG) e e-mail de usuário são únicos; CPF deve ser validado (dígitos verificadores).
- **RN15:** código do exemplar é único e gerado automaticamente (ex.: `LIV000123-EX02`), podendo ser editado.
- **RN16:** exemplar `baixado` (extraviado ou descartado) não volta a circular.

## 5. Requisitos não funcionais (RNF)

- **RNF01 – Linguagem do backend:** Kotlin.
- **RNF02 – API:** REST, JSON, versionada em `/api/v1`, documentada com OpenAPI/Swagger e coleções do Postman.
- **RNF03 – Senhas:** armazenadas como **hash** (bcrypt ou argon2), nunca em texto puro nem criptografadas de forma reversível.
- **RNF04 – Paginação:** listagens paginadas (`page`, `size`) com ordenação (`sort`).
- **RNF05 – Padrão de erros:** respostas de erro em formato único (ver seção 10.2).
- **RNF06 – Validação:** entradas validadas no backend, com mensagens claras por campo.
- **RNF07 – Testes:** testes unitários das regras de negócio e testes de integração da API; meta de cobertura mínima de (**70%**) na camada de serviço.
- **RNF08 – Execução local:** o projeto inteiro sobe com um único comando (`./run.sh`).
- **RNF09 – Datas:** armazenadas em UTC; exibidas no fuso `America/Fortaleza`.

## 6. Segurança: autenticação e sessão

### 6.1 Senhas e login

- Hash com **bcrypt/argon2** e salt.
- Após (**5**) tentativas de login erradas seguidas, a conta fica bloqueada por (**15**) minutos.
- Mensagem de erro genérica ("e-mail ou senha inválidos"), sem revelar se o e-mail existe.

### 6.2 Tokens

| Token | Duração | Onde fica | Conteúdo |
|---|---|---|---|
| Access token (JWT) | (**15 min**) | memória do frontend, enviado no header `Authorization: Bearer` | `sub` (usuário), `sid` (sessão), `jti` (id do token), `perfil`, `exp` |
| Refresh token | (**7 dias**) | cookie `HttpOnly`, `Secure`, `SameSite=Strict` | valor aleatório opaco; no banco fica só o hash |

### 6.3 Renovação (rotação de refresh token)

1. O frontend chama `POST /auth/refresh` quando o access token expira.
2. O backend valida o refresh token, **marca-o como usado** e emite um **novo par** (access + refresh).
3. **Detecção de reuso:** se um refresh token já usado for apresentado de novo, significa que ele foi copiado. A **sessão inteira é revogada** e o usuário precisa fazer login novamente.

### 6.4 Blocklist

- A blocklist guarda o `jti` de access tokens **revogados que ainda não expiraram** (logout, troca de senha, sessão suspeita). Tokens expirados já são recusados pela validação normal, então não precisam entrar na blocklist.
- Cada registro tem `expira_em` igual ao `exp` do token; uma rotina remove periodicamente os registros vencidos.

## 7. Perfis e permissões

Cada funcionalidade exige uma permissão (`recurso_acao`). As permissões são associadas a perfis, e cada usuário possui um perfil.

| Permissão | Descrição | Visitante | Leitor | Funcionário | Admin |
|---|---|:-:|:-:|:-:|:-:|
| `book_read` | consultar livros e disponibilidade | ✔ | ✔ | ✔ | ✔ |
| `book_create` | cadastrar livro | | | ✔ | ✔ |
| `book_update` | editar livro | | | ✔ | ✔ |
| `book_delete` | inativar livro | | | ✔ | ✔ |
| `genre_manage` | cadastrar/editar gêneros | | | ✔ | ✔ |
| `copy_read` | consultar exemplares | | | ✔ | ✔ |
| `copy_manage` | cadastrar/editar/baixar exemplares | | | ✔ | ✔ |
| `reader_read` | consultar leitores | | | ✔ | ✔ |
| `reader_create` | cadastrar leitor | | | ✔ | ✔ |
| `reader_update` | editar leitor / habilitar login | | | ✔ | ✔ |
| `reader_delete` | inativar leitor | | | ✔ | ✔ |
| `loan_create` | registrar empréstimo | | | ✔ | ✔ |
| `loan_read` | consultar todos os empréstimos | | | ✔ | ✔ |
| `loan_read_own` | consultar os próprios empréstimos | | ✔ | | |
| `loan_return` | registrar devolução | | | ✔ | ✔ |
| `loan_renew` | renovar qualquer empréstimo | | | ✔ | ✔ |
| `loan_renew_own` | renovar o próprio empréstimo | | ✔ | | |
| `report_read` | relatórios e exportação | | | ✔ | ✔ |
| `session_manage_own` | ver/encerrar as próprias sessões | | ✔ | ✔ | ✔ |
| `session_manage` | encerrar sessões de qualquer usuário | | | | ✔ |
| `user_manage` | gerenciar funcionários/administradores | | | | ✔ |
| `role_manage` | gerenciar perfis e permissões | | | | ✔ |
| `config_manage` | alterar configurações de negócio | | | | ✔ |
| `audit_read` | consultar auditoria | | | | ✔ |

## 8. Modelagem de dados

```mermaid
erDiagram
    PERFIL ||--o{ USUARIO : define
    PERFIL ||--|{ PERFIL_PERMISSAO : possui
    PERMISSAO ||--|{ PERFIL_PERMISSAO : compoe

    LIVRO ||--|{ LIVRO_GENERO : possui
    GENERO ||--|{ LIVRO_GENERO : classifica
    LIVRO ||--o{ EXEMPLAR : possui
    USUARIO ||--o{ LIVRO : "cadastrou"

    USUARIO ||--o{ EMPRESTIMO : "toma (leitor)"
    USUARIO ||--o{ EMPRESTIMO : "registra (operador)"
    EMPRESTIMO ||--|{ EMPRESTIMO_ITEM : contem
    EXEMPLAR ||--o{ EMPRESTIMO_ITEM : "eh emprestado"

    USUARIO ||--o{ SESSAO : abre
    SESSAO ||--o{ REFRESH_TOKEN : emite
    USUARIO ||--o{ AUDITORIA : gera

    PERFIL {
        Int id PK
        String nome UK "visitante, leitor, funcionario, admin"
        String descricao
    }

    PERMISSAO {
        Int id PK
        String codigo UK "ex.: book_create"
        String descricao
    }

    PERFIL_PERMISSAO {
        Int perfil_id PK, FK
        Int permissao_id PK, FK
    }

    USUARIO {
        Int id PK
        String nome
        String email UK
        String senha_hash "nulo = leitor sem acesso ao sistema"
        String documento UK
        String tipo_documento "CPF, RG"
        String endereco
        String telefone
        Int perfil_id FK
        String status "ativo, inativo"
        DATE suspenso_ate "RN07"
        Int tentativas_login_falhas
        TIMESTAMP bloqueado_ate "6.1"
        TIMESTAMP data_cadastro
        TIMESTAMP data_atualizacao
    }

    LIVRO {
        Int id PK
        String isbn UK "opcional"
        String titulo
        String autor
        String editora
        Int ano_publicacao
        String status "ativo, inativo"
        Int criado_por FK
        TIMESTAMP data_cadastro
        TIMESTAMP data_atualizacao
    }

    GENERO {
        Int id PK
        String nome UK
    }

    LIVRO_GENERO {
        Int livro_id PK, FK
        Int genero_id PK, FK
    }

    EXEMPLAR {
        Int id PK
        String codigo UK
        Int livro_id FK
        String condicao "novo, excelente, conservado, aceitavel, mal_conservado, danificado"
        String status "disponivel, emprestado, manutencao, baixado"
        TIMESTAMP data_cadastro
        TIMESTAMP data_atualizacao
    }

    EMPRESTIMO {
        Int id PK
        Int leitor_id FK
        Int operador_id FK
        TIMESTAMP data_emprestimo
        String status "aberto, encerrado"
        String observacao
    }

    EMPRESTIMO_ITEM {
        Int id PK
        Int emprestimo_id FK
        Int exemplar_id FK
        DATE data_devolucao_prevista
        TIMESTAMP data_devolucao_real "nulo = nao devolvido"
        Int renovacoes
        String condicao_retirada
        String condicao_devolucao
        Int recebido_por FK
    }

    SESSAO {
        UUID id PK "sid no JWT"
        Int usuario_id FK
        String device_id_hash
        String ip
        String user_agent
        String localizacao "cidade/pais via GeoIP"
        TIMESTAMP criado_em
        TIMESTAMP ultimo_acesso
        TIMESTAMP expira_em
        TIMESTAMP revogada_em
        String motivo_revogacao
    }

    REFRESH_TOKEN {
        Int id PK
        UUID sessao_id FK
        String token_hash UK
        TIMESTAMP emitido_em
        TIMESTAMP expira_em
        TIMESTAMP usado_em "preenchido = ja rotacionado; reuso revoga a sessao"
    }

    TOKEN_BLOCKLIST {
        String jti PK
        TIMESTAMP expira_em
        String motivo
    }

    CONFIGURACAO {
        String chave PK
        String valor
        String descricao
    }

    AUDITORIA {
        Int id PK
        Int usuario_id FK
        String acao
        String entidade
        Int entidade_id
        String detalhes "JSON"
        String ip
        TIMESTAMP data
    }
```

### 8.1 Campos calculados (não armazenados)

- **Exemplares disponíveis de um livro:** `COUNT(exemplar WHERE livro_id = X AND status = 'disponivel')`.
- **Situação de um item de empréstimo:**
  - `devolvido`: `data_devolucao_real` preenchida;
  - `atrasado`: não devolvido e `data_devolucao_prevista < hoje`;
  - `ativo`: não devolvido e dentro do prazo.

### 8.2 Configurações iniciais (`CONFIGURACAO`)

| Chave | Valor padrão |
|---|---|
| `emprestimo.max_itens_por_leitor` | 3 |
| `emprestimo.prazo_padrao_dias` | 14 |
| `emprestimo.prazo_maximo_dias` | 30 |
| `emprestimo.max_renovacoes` | 2 |
| `suspensao.dias_por_dia_atraso` | 1 |
| `login.max_tentativas` | 5 |
| `login.bloqueio_minutos` | 15 |
| `jwt.access_minutos` | 15 |
| `jwt.refresh_dias` | 7 |
| `sessao.viagem_impossivel_km_h` | 500 |

## 9. Organização do repositório

```
biblioteca/
├── .github/workflows/      # CI (política de .md, build/testes)
├── .githooks/              # hooks locais (pre-commit)
├── backend_biblioteca/     # API em Kotlin
├── frontend_biblioteca/    # interface web
├── docs/
│   ├── objetivo.md         # este documento
│   ├── pull_request_template.md
│   └── postman/            # coleções e ambientes do Postman
├── README.md
└── run.sh                  # sobe banco, backend e frontend localmente
```

Repositório: `git@github.com:Framks/biblioteca.git` (monorepo com backend e frontend). Regras de colaboração no `README.md`.

## 10. Contrato da API (rascunho para o frontend)

Base: `/api/v1`. Os detalhes de cada endpoint (corpo, respostas, exemplos) serão definidos nas coleções do Postman.

### 10.1 Endpoints

| Método | Rota | Permissão | Descrição |
|---|---|---|---|
| POST | `/auth/login` | pública | login; retorna access token e seta cookies |
| POST | `/auth/refresh` | cookie refresh | rotaciona tokens |
| POST | `/auth/logout` | autenticado | revoga sessão atual |
| GET | `/auth/me` | autenticado | dados e permissões do usuário logado |
| GET | `/auth/sessions` | `session_manage_own` | sessões ativas do usuário |
| DELETE | `/auth/sessions/{id}` | `session_manage_own` | encerra uma sessão |
| GET | `/catalog/books` | pública | catálogo público com disponibilidade |
| GET | `/books` | `book_read` | lista/pesquisa livros (filtros RF04) |
| GET | `/books/{id}` | `book_read` | detalhe do livro com exemplares |
| POST | `/books` | `book_create` | cadastra livro |
| PUT | `/books/{id}` | `book_update` | edita livro |
| DELETE | `/books/{id}` | `book_delete` | inativa livro |
| GET | `/genres` | `book_read` | lista gêneros |
| POST | `/genres` | `genre_manage` | cadastra gênero |
| GET | `/books/{id}/copies` | `copy_read` | exemplares do livro |
| POST | `/books/{id}/copies` | `copy_manage` | adiciona exemplar(es) |
| PATCH | `/copies/{id}` | `copy_manage` | altera condição/status (manutenção, baixa) |
| GET | `/readers` | `reader_read` | lista/pesquisa leitores |
| GET | `/readers/{id}` | `reader_read` | detalhe do leitor |
| POST | `/readers` | `reader_create` | cadastra leitor |
| PUT | `/readers/{id}` | `reader_update` | edita leitor |
| POST | `/readers/{id}/access` | `reader_update` | habilita login do leitor |
| DELETE | `/readers/{id}` | `reader_delete` | inativa leitor |
| GET | `/readers/{id}/loans` | `loan_read` | histórico do leitor |
| GET | `/loans` | `loan_read` | lista empréstimos (filtro: ativo, atrasado, devolvido) |
| GET | `/loans/me` | `loan_read_own` | empréstimos do leitor logado |
| POST | `/loans` | `loan_create` | registra empréstimo |
| GET | `/loans/{id}` | `loan_read` | detalhe do empréstimo |
| POST | `/loans/{id}/return` | `loan_return` | devolve um ou mais itens |
| POST | `/loans/items/{itemId}/renew` | `loan_renew` / `loan_renew_own` | renova item |
| GET | `/reports/summary` | `report_read` | ativos, atrasados, total |
| GET | `/reports/loans-per-month` | `report_read` | empréstimos por mês |
| GET | `/reports/top-books` | `report_read` | livros mais emprestados |
| GET | `/reports/overdue-readers` | `report_read` | leitores em atraso |
| GET | `/reports/{tipo}/export` | `report_read` | exportação CSV |
| GET/POST/PUT | `/users` | `user_manage` | funcionários e administradores |
| GET/PUT | `/roles` | `role_manage` | perfis e permissões |
| GET/PUT | `/settings` | `config_manage` | configurações de negócio |
| GET | `/audit` | `audit_read` | auditoria |

### 10.2 Formato de erro

```json
{
  "status": 422,
  "code": "LOAN_LIMIT_EXCEEDED",
  "message": "O leitor já possui 3 exemplares emprestados.",
  "fields": [
    { "field": "exemplarIds", "message": "limite excedido" }
  ],
  "timestamp": "2026-11-09T20:00:00Z",
  "path": "/api/v1/loans"
}
```

Códigos HTTP: `400` entrada inválida, `401` não autenticado/token inválido, `403` sem permissão, `404` não encontrado, `409` conflito (ex.: duplicidade), `422` regra de negócio violada.

### 10.3 Formato de listagem paginada

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

## 11. Fluxo de trabalho

- **Backend**
  - [x] Melhorar requisitos do `objetivo.md`
  - [x] Definir linguagem: **Kotlin**
  - [ ] Definir framework
  - [ ] Definir arquitetura
  - [ ] Definir padrões de projeto
  - [ ] Definir banco de dados
  - [ ] Definir estrutura dos testes
  - [ ] Criar coleções do Postman (contrato para o frontend)
- **Frontend**
  - [ ] Definir tecnologia
  - [ ] Implementar telas a partir das coleções do Postman
- **Integração**
  - [ ] Criar `run.sh` para rodar todo o projeto na máquina local

## 12. Decisões já tomadas

- O professor liberou a equipe para definir as regras de negócio e a forma de construir o sistema.
- Sistema para **uma única biblioteca**.
- **Livro ≠ exemplar:** cada exemplar tem identificação única, estado de conservação e rastreabilidade de quem o pegou.
- Um livro pode ter **vários gêneros**.
- Limites, prazos e renovações são **parametrizáveis** (seção 8.2).
- Há uma **área pública** de consulta ao acervo para visitantes.
- **Divisão do time:** backend em Kotlin; frontend com outro membro do time.

# Biblioteca Comunitária

Sistema de gerenciamento de biblioteca comunitária: acervo, exemplares, leitores, empréstimos, devoluções e relatórios.
Trabalho final da disciplina de Linguagem de Programação (UFC).

- Requisitos, regras de negócio, modelagem e contrato da API: [`docs/objetivo.md`](docs/objetivo.md)
- Arquitetura, convenções e testes do backend: [`docs/backend.md`](docs/backend.md)
- Coleção do Postman (contrato para o frontend): [`docs/postman/`](docs/postman/)

## Rodando localmente

Pré-requisitos: JDK 21+ (e Docker, só para `--postgres`; Node, quando o frontend existir).

```bash
./run.sh              # backend (H2) + frontend
./run.sh --postgres   # backend com PostgreSQL em Docker
./run.sh --test       # testes do backend
```

API em `http://localhost:8080/api/v1`. Login inicial: `admin@biblioteca.local` / `admin12345`.

## Estrutura (monorepo)

```
.
├── backend_biblioteca/   # API REST em Kotlin
├── frontend_biblioteca/  # interface web
├── docs/                 # requisitos, contrato da API, coleções do Postman
├── .github/workflows/    # CI
├── .githooks/            # hooks locais de git
└── run.sh                # sobe o projeto inteiro localmente
```

## Primeiros passos

```bash
git clone git@github.com:Framks/biblioteca.git
cd biblioteca
git config core.hooksPath .githooks   # ativa o pre-commit do projeto
```

## Como contribuir

1. **Nunca** faça commit direto na `master`: ela é protegida.
2. Crie uma branch a partir da `master` atualizada:
   - `back/<descricao>`: mudanças no backend (ex.: `back/cadastro-livros`)
   - `front/<descricao>`: mudanças no frontend (ex.: `front/tela-login`)
   - `docs/<descricao>`: documentação e coleções do Postman
   - `infra/<descricao>`: CI, `run.sh`, configurações
3. Commits no padrão [Conventional Commits](https://www.conventionalcommits.org/pt-br/), indicando a área:
   - `feat(back): cadastro de livros`
   - `fix(front): validação do formulário de leitor`
   - `docs: endpoints de empréstimo no Postman`
4. Abra um Pull Request para a `master`. Para fazer o merge é preciso:
   - **1 aprovação** de outro membro do time (o autor não pode aprovar o próprio PR);
   - os checks **`md-policy`**, **`backend (h2)`** e **`backend (postgres)`** passando;
   - novos commits depois da aprovação invalidam a aprovação.
5. Se o PR muda a API, atualize a coleção do Postman em `docs/postman/` no mesmo PR.

## Política de arquivos Markdown

Só podem ser versionados:

- `README.md` (em qualquer pasta);
- arquivos `.md` dentro de `docs/`.

Qualquer outro `.md` (anotações pessoais, `CLAUDE.md`, rascunhos etc.) fica só na sua máquina.
A regra é aplicada em três camadas: `.gitignore`, hook `pre-commit` e o check `md-policy` no CI, obrigatório para o merge.

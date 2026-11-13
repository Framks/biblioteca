#!/usr/bin/env bash
# Sobe o projeto na máquina local.
#
#   ./run.sh              backend (H2 em arquivo, perfil dev) + frontend, se existir
#   ./run.sh --postgres   backend com PostgreSQL em Docker (perfil prod) + frontend
#   ./run.sh --back       só o backend (combina com --postgres)
#   ./run.sh --test       roda os testes do backend e sai
#   ./run.sh --help
#
# Ctrl+C encerra tudo o que o script iniciou.
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACK="$RAIZ/backend_biblioteca"
FRONT="$RAIZ/frontend_biblioteca"
API="http://localhost:8080/api/v1"

USAR_POSTGRES=false
SO_BACK=false
SO_TESTES=false
for arg in "$@"; do
  case "$arg" in
    --postgres) USAR_POSTGRES=true ;;
    --back) SO_BACK=true ;;
    --test) SO_TESTES=true ;;
    -h|--help) sed -n '2,10p' "$0"; exit 0 ;;
    *) echo "Opção desconhecida: $arg (veja ./run.sh --help)"; exit 1 ;;
  esac
done

info() { printf '\033[1;34m▶ %s\033[0m\n' "$*"; }
aviso() { printf '\033[1;33m⚠ %s\033[0m\n' "$*"; }
erro() { printf '\033[1;31m✖ %s\033[0m\n' "$*"; exit 1; }

command -v java >/dev/null || erro "Java não encontrado. Instale o JDK 21 ou superior."

# Ativa os hooks do repositório (política de arquivos .md) se ainda não estiverem ativos.
if git -C "$RAIZ" rev-parse >/dev/null 2>&1 && [ "$(git -C "$RAIZ" config core.hooksPath || true)" != ".githooks" ]; then
  git -C "$RAIZ" config core.hooksPath .githooks && info "Hooks do git ativados (.githooks)."
fi

if $SO_TESTES; then
  info "Rodando os testes do backend..."
  cd "$BACK" && exec ./gradlew test
fi

PIDS=()
CONTAINER=""
# Encerra o processo e todos os filhos (ex.: npm -> vite).
matar_arvore() {
  local pid=$1 filho
  for filho in $(pgrep -P "$pid" 2>/dev/null); do matar_arvore "$filho"; done
  kill "$pid" 2>/dev/null || true
}
encerrar() {
  trap - INT TERM EXIT
  echo
  info "Encerrando..."
  for pid in "${PIDS[@]}"; do matar_arvore "$pid"; done
  wait 2>/dev/null || true
  if [ -n "$CONTAINER" ]; then docker stop "$CONTAINER" >/dev/null 2>&1 || true; fi
}
trap encerrar INT TERM EXIT

# ---------- banco ----------
if $USAR_POSTGRES; then
  command -v docker >/dev/null || erro "Docker não encontrado (necessário para --postgres)."
  CONTAINER="biblioteca-postgres"
  PG_PORTA="${PG_PORTA:-5432}"
  info "Subindo PostgreSQL na porta $PG_PORTA (dados no volume 'biblioteca-pgdata')..."
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
  docker run -d --rm --name "$CONTAINER" -p "$PG_PORTA:5432" \
    -e POSTGRES_DB=biblioteca -e POSTGRES_USER=biblioteca -e POSTGRES_PASSWORD=biblioteca \
    -v biblioteca-pgdata:/var/lib/postgresql/data postgres:17-alpine >/dev/null
  until docker exec "$CONTAINER" pg_isready -U biblioteca -d biblioteca >/dev/null 2>&1; do sleep 1; done

  export SPRING_PROFILES_ACTIVE=prod
  export DB_URL="jdbc:postgresql://localhost:$PG_PORTA/biblioteca"
  export COOKIE_SEGURO="${COOKIE_SEGURO:-false}" # http://localhost não tem HTTPS
  if [ -z "${JWT_SEGREDO:-}" ]; then
    export JWT_SEGREDO="$(head -c 48 /dev/urandom | base64 | tr -d '\n')"
    aviso "JWT_SEGREDO não definido: gerei um aleatório (os logins caem a cada reinício)."
  fi
  if [ -z "${ADMIN_SENHA_INICIAL:-}" ]; then
    export ADMIN_SENHA_INICIAL="admin12345"
    aviso "ADMIN_SENHA_INICIAL não definido: usando admin12345 (só vale se o admin ainda não tiver senha)."
  fi
else
  info "Usando H2 em arquivo (backend_biblioteca/data/). Apague a pasta para zerar o banco."
fi

# ---------- backend ----------
# Compila o jar e roda com "java -jar" (e não "gradlew bootRun"): assim a aplicação é filha deste script
# e é encerrada junto com ele. No bootRun ela seria filha do daemon do Gradle e ficaria rodando.
info "Compilando o backend (na primeira vez o Gradle baixa as dependências)..."
( cd "$BACK" && ./gradlew -q bootJar ) || erro "Falha ao compilar o backend."
JAR="$(ls "$BACK"/build/libs/*.jar | grep -v -- '-plain.jar' | head -1)"
info "Subindo o backend..."
( cd "$BACK" && exec java -jar "$JAR" ) &
PIDS+=($!)

for _ in $(seq 1 180); do
  curl -sf "$API/genres" >/dev/null 2>&1 && break
  kill -0 "${PIDS[0]}" 2>/dev/null || erro "O backend parou durante a inicialização (veja a saída acima)."
  sleep 1
done
curl -sf "$API/genres" >/dev/null 2>&1 || erro "O backend não respondeu em 3 minutos."

# ---------- frontend ----------
if ! $SO_BACK && [ -f "$FRONT/package.json" ]; then
  command -v npm >/dev/null || erro "npm não encontrado (necessário para o frontend)."
  info "Subindo o frontend..."
  ( cd "$FRONT" && { [ -d node_modules ] || npm install; } && npm run dev ) &
  PIDS+=($!)
elif ! $SO_BACK; then
  aviso "frontend_biblioteca/package.json não encontrado: subindo só o backend."
fi

echo
info "Pronto!"
echo "  API:          $API   (coleção do Postman em docs/postman/)"
$USAR_POSTGRES || echo "  Console H2:   http://localhost:8080/h2-console  (JDBC URL: jdbc:h2:file:./data/biblioteca)"
echo "  Login admin:  admin@biblioteca.local / ${ADMIN_SENHA_INICIAL:-admin12345} (troque em PUT /auth/password)"
echo "  Ctrl+C para encerrar."
wait

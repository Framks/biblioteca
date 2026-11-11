#!/usr/bin/env bash
# Política de Markdown do repositório:
# só são permitidos README.md (em qualquer pasta) e arquivos .md dentro de docs/.
#
# Uso:
#   check-md-policy.sh            -> verifica arquivos staged (pre-commit)
#   check-md-policy.sh --all      -> verifica todos os arquivos versionados (CI)
set -euo pipefail

if [[ "${1:-}" == "--all" ]]; then
  files=$(git ls-files)
else
  files=$(git diff --cached --name-only --diff-filter=ACMR)
fi

violations=$(printf '%s\n' "$files" \
  | grep -iE '\.md$' \
  | grep -vE '(^|/)README\.md$' \
  | grep -vE '^docs/' || true)

if [[ -n "$violations" ]]; then
  echo "❌ Arquivos .md fora da política (permitido apenas README.md ou arquivos em docs/):"
  echo "$violations" | sed 's/^/   - /'
  exit 1
fi

echo "✔ Política de .md ok"

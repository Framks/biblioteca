# Frontend — Estante Livre

Interface em Vue 3 e Vite, baseada na prototipação Estante Livre. Requer Node.js 18 ou superior e a API da biblioteca em execução.

```bash
cd frontend_biblioteca
npm install
npm run dev
```

O servidor de desenvolvimento encaminha `/api` para `http://localhost:8080`. Para usar outro endereço, execute `API_TARGET=http://localhost:PORTA npm run dev`. Para gerar os arquivos de produção, execute `npm run build`; o servidor que os hospedar deve encaminhar `/api` para o backend.

A branch `master` ainda não contém o backend. Os contratos usados aqui foram consultados na branch `origin/back/backend-completo`. O acesso às telas depende das permissões retornadas pela API. O access token é mantido em memória; o refresh token é gerenciado pelo cookie da API.

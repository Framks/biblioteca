# Frontend — Estante Livre

Interface em Vue 3 e Vite, baseada na prototipação Estante Livre. Requer Node.js 18 ou superior.

Na raiz do repositório, execute `./run.sh` para iniciar a API e o frontend. O endereço padrão da interface é `http://localhost:5173`. Com a API já em execução, também é possível iniciar apenas o frontend:

```bash
cd frontend_biblioteca
npm install
npm run dev
```

O servidor de desenvolvimento encaminha `/api` para `http://localhost:8080`. Para usar outro endereço, execute `API_TARGET=http://localhost:PORTA npm run dev`. Para gerar os arquivos de produção, execute `npm run build`; o servidor que os hospedar deve encaminhar `/api` para o backend.

O contrato da API está na [coleção do Postman](../docs/postman/biblioteca.postman_collection.json). O acesso às telas depende das permissões retornadas pela API. O token de acesso fica em memória, e a API gerencia o token de renovação por cookie.

# Fluxo de Testes — Chatbot API

> **Nota:** Autenticação removida. Todos os endpoints estão abertos (sem JWT).
> **Nota:** Ainda sem RAG context — nenhum documento foi enviado para a base de conhecimento.

---

## 1. Health Check

Verificar se a aplicação está no ar.

```
GET http://localhost:8080/health
```

**Resposta esperada (200):**
```json
{
  "status": "UP",
  "timestamp": "2026-06-30T19:00:00"
}
```

---

## 2. Criar Conversa

Toda mensagem precisa pertencer a uma conversa. Crie uma primeiro.

```
POST http://localhost:8080/api/conversations
Content-Type: application/json

{
  "title": "Minha primeira conversa"
}
```

**Resposta esperada (201):**
```json
{
  "id": 1,
  "title": "Minha primeira conversa",
  "createdAt": "2026-06-30T19:00:00",
  "updatedAt": "2026-06-30T19:00:00"
}
```

> Guarde o `id` retornado — você vai usá-lo para enviar mensagens.

### Validação — título em branco (400):

```
POST http://localhost:8080/api/conversations
Content-Type: application/json

{
  "title": ""
}
```

**Resposta esperada (400):**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "title: must not be blank",
  "path": "/api/conversations",
  "timestamp": "2026-06-30T19:00:00"
}
```

---

## 3. Listar Conversas

```
GET http://localhost:8080/api/conversations
```

**Resposta esperada (200):**
```json
[
  {
    "id": 1,
    "title": "Minha primeira conversa",
    "createdAt": "2026-06-30T19:00:00",
    "updatedAt": "2026-06-30T19:00:00"
  }
]
```

### Buscar por ID:

```
GET http://localhost:8080/api/conversations/1
```

**Resposta esperada (200):** mesma estrutura acima.

**ID inexistente (404):**
```
GET http://localhost:8080/api/conversations/999
```

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Conversa não encontrada com id: 999",
  "path": "/api/conversations/999",
  "timestamp": "2026-06-30T19:00:00"
}
```

---

## 4. Enviar Mensagem (Chat)

> Este é o fluxo principal que você quer testar.
> Como não há documentos na base, o RAG retornará **sources vazio** e **chunksConsumed = 0**.
> O modelo vai responder com base apenas no conhecimento dele (sem contexto adicional).

```
POST http://localhost:8080/api/chat/send-v2
Content-Type: application/json

{
  "conversationId": 1,
  "message": "O que é RAG?"
}
```

**Resposta esperada (200):**
```json
{
  "userMessage": {
    "id": 1,
    "role": "USER",
    "content": "O que é RAG?",
    "createdAt": "2026-06-30T19:01:00"
  },
  "botMessage": {
    "id": 2,
    "role": "BOT",
    "content": "RAG (Retrieval-Augmented Generation) é uma técnica que combina...",
    "createdAt": "2026-06-30T19:01:05"
  },
  "answer": "RAG (Retrieval-Augmented Generation) é uma técnica que combina...",
  "sources": [],
  "executionTimeMs": 3500,
  "chunksConsumed": 0
}
```

### O que observar:

| Campo | O que indica |
|-------|-------------|
| `answer` | Resposta gerada pelo modelo (Gemma 2 9B via OpenRouter) |
| `sources` | **Vazio** — nenhum documento foi indexado ainda |
| `chunksConsumed` | **0** — sem chunks para consultar |
| `executionTimeMs` | Tempo total (embedding + LLM). Varia entre 2–10s dependendo do modelo |
| `userMessage` / `botMessage` | Mensagens salvas no banco, vinculadas à conversa |

### Faça perguntas variadas para testar o modelo:

| Pergunta | O que testar |
|----------|-------------|
| `"O que é RAG?"` | Conhecimento geral do modelo |
| `"Explique o conceito de embeddings"` | Tema técnico |
| `"Qual a capital do Brasil?"` | Fato simples |
| `"Me ajude a escrever um email de apresentação"` | Geração criativa |
| `"Com base nos documentos que enviei, responda..."` | **Observação:** sem documentos, o modelo vai responder do conhecimento geral dele, ignorando o "com base nos documentos" |

### Validações — conversa inexistente (404):

```
POST http://localhost:8080/api/chat/send-v2
Content-Type: application/json

{
  "conversationId": 999,
  "message": "teste"
}
```

**Resposta:** 404 Not Found.

### Validações — mensagem em branco (400):

```
POST http://localhost:8080/api/chat/send-v2
Content-Type: application/json

{
  "conversationId": 1,
  "message": ""
}
```

**Resposta:** 400 Bad Request.

---

## 5. Histórico da Conversa

Após enviar algumas mensagens, consulte o histórico:

```
GET http://localhost:8080/api/chat/history/1
```

**Resposta esperada (200):**
```json
[
  {
    "id": 1,
    "role": "USER",
    "content": "O que é RAG?",
    "createdAt": "2026-06-30T19:01:00"
  },
  {
    "id": 2,
    "role": "BOT",
    "content": "RAG (Retrieval-Augmented Generation) é uma técnica...",
    "createdAt": "2026-06-30T19:01:05"
  }
]
```

Os registros vêm ordenados por `createdAt` crescente (do mais antigo para o mais novo).

### Conversa sem mensagens:
Se criou uma conversa mas nunca enviou mensagens, o retorno será:
```json
[]
```

### ID inexistente (404):
```
GET http://localhost:8080/api/chat/history/999
```
Retorna 404.

---

## 6. Fluxo Completo (Teste de Ponta a Ponta)

Sugestão de sequência no Postman:

```
 1. GET  /health                        → 200 (app no ar)
 2. POST /api/conversations             → 201 (criar conversa, guardar id)
 3. POST /api/chat/send-v2              → 200 (enviar pergunta)
 4. POST /api/chat/send-v2              → 200 (enviar pergunta de follow-up)
 5. GET  /api/chat/history/{id}         → 200 (histórico com 4 mensagens: 2 user + 2 bot)
 6. GET  /api/conversations             → 200 (listar todas as conversas)
 7. GET  /api/conversations/{id}        → 200 (detalhe de uma conversa)
 8. DELETE /api/conversations/{id}      → 204 (deletar conversa e mensagens vinculadas)
 9. GET  /api/conversations/{id}        → 404 (confirmar que foi deletada)
```

> O DELETE em cascata remove conversa + mensagens + attachments vinculados.

---

## 7. Upload de Arquivo (Anexo)

Você pode anexar arquivos a uma conversa (sem processamento de conteúdo — apenas armazenamento).

```
POST http://localhost:8080/api/files/upload?conversationId=1
Content-Type: multipart/form-data

file: (selecionar arquivo .txt ou .pdf)
```

**Resposta esperada (201):**
```json
{
  "id": 1,
  "fileName": "notas.txt",
  "contentType": "text/plain",
  "size": 1024,
  "uploadDate": "2026-06-30T19:00:00"
}
```

**Regras:**
- Formatos aceitos: `.txt` e `.pdf`
- Tamanho máximo: **10MB**
- Content-Type deve ser `text/plain` ou `application/pdf`

---

## 8. Upload de Documento (Pipeline RAG)

Quando quiser começar a testar com RAG de fato, use este endpoint. Ele:
1. Extrai texto do arquivo (.txt, .pdf, .docx)
2. Chunk o texto
3. Gera embeddings para cada chunk
4. Salva no banco (pronto para consulta via similaridade)

```
POST http://localhost:8080/api/documents/upload
Content-Type: multipart/form-data

file: (selecionar arquivo .txt, .pdf ou .docx)
```

**Resposta esperada (201):**
```json
{
  "id": 1,
  "fileName": "artigo-rag.txt",
  "contentType": "text/plain",
  "fileSize": 51200,
  "chunkCount": 12,
  "uploadedAt": "2026-06-30T19:00:00"
}
```

**Consultar status do processamento:**
```
GET http://localhost:8080/api/documents/1/status
```

Retorna: `PROCESSING`, `COMPLETED` ou `FAILED`.

> Após o documento ser processado (status = COMPLETED), as próximas chamadas a `/api/chat/send-v2` vão incluir contexto dos chunks nos `sources` e `chunksConsumed > 0`.

---

## 9. Endpoints Legados (Deprecados)

```
POST http://localhost:8080/api/chat/send
```

Mesmo body do `send-v2`, mas retorna apenas `ChatResponse` (sem sources, executionTimeMs, chunksConsumed).
**Não usar para testes novos** — mantido apenas para compatibilidade.

---

## 10. Cenários de Erro para Testar

| Teste | Chamada | Esperado |
|-------|---------|----------|
| Criar conversa sem título | `POST /api/conversations` com body vazio | 400 |
| Mensagem em conversa inexistente | `POST /api/chat/send-v2` com conversationId=999 | 404 |
| Mensagem sem conteúdo | Body com `message: ""` | 400 |
| Histórico de conversa inexistente | `GET /api/chat/history/999` | 404 |
| Deletar conversa inexistente | `DELETE /api/conversations/999` | 404 |
| Upload arquivo extensão inválida | `.png` ou `.exe` | 400 |
| Upload arquivo > 10MB | Arquivo grande | 413 |

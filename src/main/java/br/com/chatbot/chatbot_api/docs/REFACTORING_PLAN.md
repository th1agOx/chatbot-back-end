# Plano de Refatoração — Chatbot Back-End

## Diagnóstico Geral

Com base na análise dos logs de execução (capturados em 30/06/2026), foram identificados **3 problemas ativos** que impedem o funcionamento correto da API:

| #   | Problema                                            | Gravidade   | Impacto                             |
| --- | --------------------------------------------------- | ----------- | ----------------------------------- |
| 1   | Dimensão do vector no banco incorreta (768 vs 1536) | **CRÍTICO** | Upload de documentos retorna 500    |
| 2   | Chat model free rate-limited (429)                  | **MÉDIO**   | Chat quebra após poucas requisições |
| 3   | n8n webhook offline                                 | **BAIXO**   | Notificação falha silenciosamente   |

---

## Problema 1 — Dimensão do Vector no Banco (CRÍTICO)

### Sintoma

```
ERROR: expected 768 dimensions, not 1536
Caused by: org.postgresql.util.PSQLException: ERROR: expected 768 dimensions, not 1536
    at org.hibernate.engine.jdbc.spi.SqlExceptionHelper : ERROR: expected 768 dimensions, not 1536
```

### Causa Raiz

A migration `V6__create_document_chunks.sql` foi alterada **após já ter sido executada** no banco:

- **Arquivo atual em disco:** `embedding vector(1536)`
- **Banco real:** coluna `embedding` como `vector(768)` (versão original da migration)

O Flyway já aplicou a V6 e, com `spring.flyway.validate-on-migrate: false`, não detecta a divergência de checksum. O Hibernate com `ddl-auto: validate` também não valida colunas com `columnDefinition` customizada contra o schema real.

Como a `EmbeddingService` gera embeddings de 1536 dimensões (`text-embedding-3-small`) e o banco só aceita 768, o `INSERT` no `document_chunks` falha e a transação é revertida, resultando em 500.

### O Fluxo do Erro

```
Upload (POST /api/documents/upload)
  → DocumentService.upload()
    → buildChunks()
      → embeddingService.generateEmbedding() → List<Float> (1536 dims)
      → new PGvector(float[]) → DocumentChunk com embedding
    → documentRepository.save(document) → flush → INSERT
      → PostgreSQL rejeita: "expected 768 dimensions, not 1536"
      → DataIntegrityViolationException → rollback
    → failureRecorder.recordFailure() → registra em tabela própria
  → GlobalExceptionHandler → 500
```

### O que já foi feito

✅ Criado arquivo `V8__fix_embedding_dimension.sql` com:

```sql
ALTER TABLE document_chunks ALTER COLUMN embedding TYPE vector(1536);
```

### Ações necessárias

1. **Aplicar a migration manualmente** — ao subir a aplicação, o Flyway executará a V8 automaticamente. Verificar no log: `Successfully applied 1 migration to schema "public" (execution time ...)`.

2. **Validar** — após aplicar, fazer upload de um documento .txt confirmando que o INSERT funciona.

### Prevenção futura

- Manter `spring.flyway.validate-on-migrate: true` em produção para detectar alterações em migrations já aplicadas.
- Nunca editar uma migration já commitada — criar uma nova migration incremental.

---

## Problema 2 — Chat Model Free Rate-Limited (MÉDIO)

### Sintoma

```
429 - meta-llama/llama-3.3-70b-instruct:free is temporarily rate-limited upstream
```

### Causa Raiz

O modelo `meta-llama/llama-3.3-70b-instruct:free` é servido via provedor **Venice** na OpenRouter, que impõe rate limits agressivos (estima-se ~20 req/min) no tier gratuito. Após algumas requisições consecutivas, o provedor retorna 429.

Como o Spring AI trata 429 como `NonTransientAiException` (não retenta por padrão), a chamada falha imediatamente e propaga o erro para o `GlobalExceptionHandler`.

### O Fluxo do Erro

```
POST /api/chat/send
  → ChatController.send()
    → ChatServiceImpl.sendMessage()
      → BotServiceImpl.responseGenerateWithMetadata()
        → ragService.retrieveContext()   ← pode funcionar
        → chatModel.call(prompt)          ← OpenRouter → 429
          → OpenAiChatModel.internalCall()
            → RetryTemplate.execute()     ← retenta, mas 429 é NonTransient
            → NonTransientAiException
          → GlobalExceptionHandler → 500
```

### O que já foi feito

✅ Trocado o modelo no `application.yml`:

```yaml
# antes
model: meta-llama/llama-3.3-70b-instruct:free
# depois
model: qwen/qwen3-235b-a22b:free
```

O `qwen/qwen3-235b-a22b:free` utiliza o provedor **Qwen** (Alibaba Cloud), que historicamente oferece rate limits mais generosos no tier gratuito.

### Ações Recomendadas

1. **Monitorar** — verificar se o novo modelo também sofre rate limiting nos primeiros dias de uso.
2. **Adicionar fallback** — configurar um roteador no OpenRouter usando o endpoint `openrouter/free` que faz auto-fallback entre modelos free disponíveis. Isso exige alteração no `OpenAiConfig.java`.
3. **Melhorar tratamento de erro 429** — capturar o `NonTransientAiException` especificamente no `GlobalExceptionHandler` e retornar uma mensagem amigável (`"Serviço temporariamente indisponível, tente novamente em alguns segundos"`) em vez de 500 genérico.

---

## Problema 3 — n8n Webhook Offline (BAIXO)

### Sintoma

```
Falha ao notificar n8n para documento 1: I/O error on POST request for "http://localhost:5678/webhook/chatbot-ingest"
```

### Causa Raiz

A variável de ambiente `N8N_WEBHOOK_URL` (`http://localhost:5678/webhook/chatbot-ingest`) aponta para uma instância do n8n que não está rodando. O erro é esperado em ambiente de desenvolvimento quando o n8n não é necessário.

O `N8nWebhookNotifier` já captura a exceção genericamente e apenas loga o erro — **não quebra o fluxo de upload**. Impacto real: zero para o funcionamento da API.

### Ações Recomendadas

1. **Tornar a notificação opcional** — usar `@Value("${app.n8n.webhook-url:#{null}}")` e só notificar se a URL estiver configurada, eliminando o warning no log.
2. **Separar webhook em ambiente** — usar perfis Spring (`dev`, `prod`) para controlar se o n8n é notificado.

---

## Problemas Adicionais Identificados na Análise

### 4. Inconsistência nas base-urls do OpenRouter

**Arquivo:** `application.yml:27-32`

```yaml
openai:
  base-url: https://openrouter.ai/api # chat
  embedding:
    base-url: https://openrouter.ai/api/v1 # embedding
```

O chat usa `/api` e o embedding usa `/api/v1`. Apesar de funcionar porque o `OpenAiConfig.java` define `embeddingsPath("/embeddings")`, a configuração ficou frágil:

- Se a OpenRouter mudar o path de embeddings, o sistema quebra.
- A diferença de versão na URL pode causar erros obscuros.

**Recomendação:** Unificar ambas para a mesma base e confirmar os paths com a documentação atual da OpenRouter.

### 5. Tratamento genérico de erro no N8nWebhookNotifier

**Arquivo:** `N8nWebhookNotifier.java:28`

```java
catch (Exception e) {
    log.error("Falha ao notificar n8n para documento {}: {}", documentId, e.getMessage());
}
```

Engole qualquer exceção sem distinção (timeout, DNS, 4xx, 5xx). Idealmente:

- Logar o stack trace completo no primeiro erro e apenas a mensagem nas repetições.
- Implementar retry com backoff para falhas transitórias.

### 6. Ausência de validação de dimensão do embedding

**Arquivo:** `DocumentService.java:108-109`

```java
var vector = embeddingService.generateEmbedding(chunks.get(i));
var pgVector = new PGvector(vector.stream().mapToDouble(Float::doubleValue).toArray());
```

Se o modelo de embedding mudar de dimensão (ex: `text-embedding-3-small` → `text-embedding-3-large` com 3072 dims), o erro só aparece na hora do INSERT. Uma validação explícita no `DocumentService` evitaria isso:

```java
if (vector.size() != 1536) {
    throw new DocumentProcessingException(
        "Dimensão do embedding inesperada: " + vector.size() + " (esperado: 1536)");
}
```

---

## Resumo das Ações

| Prioridade | Ação                                                     | Arquivo                           | Status      |
| ---------- | -------------------------------------------------------- | --------------------------------- | ----------- |
| 🔴 CRÍTICA | Migration V8: `ALTER COLUMN embedding TYPE vector(1536)` | `V8__fix_embedding_dimension.sql` | ✅ Criado   |
| 🟡 MÉDIA   | Trocar chat model para `qwen/qwen3-235b-a22b:free`       | `application.yml:30`              | ✅ Aplicado |
| 🟡 MÉDIA   | Unificar base-urls do OpenRouter                         | `application.yml:27-32`           | ⏳ Pendente |
| 🟢 BAIXA   | Tornar n8n webhook opcional com fallback `#{null}`       | `N8nWebhookNotifier.java`         | ⏳ Pendente |
| 🟢 BAIXA   | Adicionar validação de dimensão do embedding             | `DocumentService.java`            | ⏳ Pendente |
| 🟢 BAIXA   | Melhorar tratamento de 429 no GlobalExceptionHandler     | `GlobalExceptionHandler.java`     | ⏳ Pendente |
| 🟢 BAIXA   | Separar webhook n8n por perfil Spring                    | `application-{profile}.yml`       | ⏳ Pendente |

---

## Como testar a correção do Problema 1

1. Subir o container PostgreSQL: `docker compose up -d`
2. Subir a aplicação: `./mvnw spring-boot:run`
3. Verificar no log: `Successfully applied 1 migration to schema "public" (execution time ...)`
4. Fazer upload de um arquivo .txt via `POST /api/documents/upload` (multipart)
5. Verificar retorno `201 Created` com status `COMPLETED`
6. Consultar `GET /api/documents/{id}/status` → `COMPLETED`

---

## Histórico de Alterações

| Data       | Autor            | Descrição                                     |
| ---------- | ---------------- | --------------------------------------------- |
| 30/06/2026 | Refactoring Plan | Criação do documento com diagnóstico completo |

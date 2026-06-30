# Debuging 2 — Conflitos de Migrations e Configurações

## Erro Atual — Conflito de versão V3

```
Found more than one migration with version 3
Offenders:
-> V3__create_attachments.sql (SQL)
-> V3__init_extensions.sql (SQL)
```

**Causa**: Dois arquivos com version `3` no mesmo classpath:

- `V3__create_attachments.sql` (original — cria tabela `attachments`)
- `V3__init_extensions.sql` (novo — `CREATE EXTENSION IF NOT EXISTS vector`)

Flyway não permite versões duplicadas.

---

## Issues Identificados

### 🔴 1. Conflito V3 — BLOQUEANTE

// FEITO POR HUMANO

**O que acontece**: Spring nem termina de iniciar porque Flyway valida as migrations antes de aplicar.

**Por que foi criado**: O usuário adicionou `V3__init_extensions.sql` para instalar a extensão `vector` (pgvector) antes de V6 rodar. Porém a V6 **já** tem `CREATE EXTENSION IF NOT EXISTS vector;` na primeira linha — a extensão é instalada automaticamente antes de criar a tabela `document_chunks`.

**Solução**: Remover `V3__init_extensions.sql` — é redundante com V6.

---

### 🔴 2. Base URL do OpenRouter Incorreta — BLOQUEANTE EM RUNTIME

**Arquivo**: `application.yml:28`
**Valor atual**:

```yaml
base-url: https://openrouter.ai/api/v1/chat/completions
```

**Problema**: O `OpenAiApi` do Spring AI (v1.0.0-M6) constrói a URL final como `{baseUrl}{path}` onde:

- `path` para chat = `/v1/chat/completions`
- `path` para embeddings = `/v1/embeddings`

Com o valor atual, as chamadas vão para:
| Tipo | URL gerada |
|------|-----------|
| Chat | `https://openrouter.ai/api/v1/chat/completions/v1/chat/completions` |
| Embedding | `https://openrouter.ai/api/v1/chat/completions/v1/embeddings` |

Ambas são inválidas — a OpenRouter espera:
| Tipo | URL correta |
|------|------------|
| Chat | `POST https://openrouter.ai/api/v1/chat/completions` |
| Embedding | `POST https://openrouter.ai/api/v1/embeddings` |

**Solução**: Alterar `base-url` para `https://openrouter.ai`.

> O Spring AI anexa automaticamente `/v1/chat/completions` e `/v1/embeddings`.

---

### 🔴 3. Variáveis `.env` não carregadas pelo Spring Boot — BLOQUEANTE

**Arquivo**: `.env`
**Problema**: Spring Boot **não** lê arquivos `.env` por padrão. As seguintes envs ficam indisponíveis:

| Env                                     | Usada em                                    | Efeito se faltar                            |
| --------------------------------------- | ------------------------------------------- | ------------------------------------------- |
| `JWT_SECRET`                            | `api.security.token.secret`                 | App não sobe (já vimos esse erro)           |
| `OPEN_ROUTER_API_KEY`                   | `spring.ai.openai.api-key`                  | Chamadas de chat/embedding falham com 401   |
| `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | `spring.datasource.*`                       | Fallback pros defaults do YML (pode ser ok) |
| `N8N_WEBHOOK_URL`                       | `app.n8n.webhook-url` (via relaxed binding) | Webhook não é chamado                       |

**Soluções possíveis**:

1. Adicionar dependência `me.paulschwarz:spring-dotenv` ao `pom.xml`
2. Ou adicionar `spring.config.import=file:.env[.properties]` no `application.yml`
3. Ou setar manualmente as variáveis no terminal antes de rodar

---

### 🟡 4. Construtor `OpenAiApi` deprecated

**Arquivo**: `OpenAiConfig.java:23,36`
**Código atual**: `new OpenAiApi(baseUrl, apiKey)` — marcado como `@Deprecated(since = "1.0.0.M6")`
**Recomendação**: Migrar para `OpenAiApi.builder()`. Exemplo:

```java
var api = OpenAiApi.builder()
    .baseUrl(baseUrl)
    .apiKey(apiKey)
    .build();
```

Não bloqueante, mas será removido em versões futuras.

---

### 🟡 5. Docker Compose — default password mismatch

// FEITO POR HUMANO

**Arquivo**: `docker-compose.yml:8`

```yaml
POSTGRES_PASSWORD: ${DB_PASSWORD-Th123}
```

**Problema**: Default é `Th123` (sem o `4`), enquanto o `application.yml` usa `Th1234`.

Se `DB_PASSWORD` não estiver definida no ambiente, o container PostgreSQL inicia com senha `Th123`, mas a aplicação tenta conectar com `Th1234`.

**Solução**: Corrigir o default para `Th1234`:

```yaml
POSTGRES_PASSWORD: ${DB_PASSWORD-Th1234}
```

---

### 🔴 6. V6 — pgvector ainda pendente

Após corrigir os issues 1–5, a migration V6 continuará falhando com:

```
ERRO: a extensão "vector" não está disponível
Dica: The extension must first be installed on the system where PostgreSQL is running.
```

**Solução**: Usar o `docker-compose.yml` (imagem `pgvector/pgvector:pg17` já vem com pgvector instalado) ou instalar pgvector manualmente no PostgreSQL local.

---

## Ordem recomendada de correção

```
1.  🔴 Remover V3__init_extensions.sql         → desbloqueia Flyway
2.  🔴 Corrigir base-url do OpenRouter           → chat/embedding funcionam
3.  🔴 Carregar .env no Spring Boot              → JWT_SECRET e demais envs disponíveis
4.  🔴 Instalar pgvector ou usar Docker           → V6 executa
5.  🟡 Corrigir default password no docker-compose → consistência
6.  🟡 Migrar OpenAiApi para Builder              → preparar para future-proof
```

## Dependência entre os erros

```
V3 conflito (Flyway falha) → nada adianta
   ↓ (corrigir V3)
base-url errada (runtime) → chat/embedding 404
   ↓ (corrigir base-url)
.env não carregado (runtime) → JWT_SECRET/API_KEY faltando
   ↓ (corrigir .env)
pgvector ausente (V6 falha) → app não finaliza migrations
```

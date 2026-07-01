# Plano de Migração: OpenRouter → Ollama Local

## Objetivo

Migrar de OpenRouter (nuvem) para Ollama local com:
- **Chat model:** `llama3.2:1b`
- **Embedding model:** `nomic-embed-text`

## Arquivos afetados

| # | Arquivo | Ação |
|---|---------|------|
| 1 | `pom.xml` | Trocar `spring-ai-openai-spring-boot-starter` → `spring-ai-ollama-spring-boot-starter` |
| 2 | `application.yml` | Substituir bloco `spring.ai.openai.*` por `spring.ai.ollama.*` |
| 3 | `OpenAiConfig.java` | **Remover** — auto-configuration do Ollama cobre tudo |
| 4 | `BotServiceImpl.java` | Trocar `OpenAiChatModel` → `OllamaChatModel`, `OpenAiChatOptions` → `OllamaChatOptions` |
| 5 | `EmbeddingServiceImpl.java` | Trocar `OpenAiEmbeddingModel` → `OllamaEmbeddingModel`, `OpenAiEmbeddingOptions` → `OllamaEmbeddingOptions` |
| 6 | `BotServiceImplTest.java` | Trocar mock `OpenAiChatModel` → `OllamaChatModel` |
| 7 | `.env` | Remover `OPEN_ROUTER_API_KEY` |
| 8 | `docker-compose.yml` | Adicionar serviço `ollama` |

## Passos

### Passo 1 — Instalar Ollama (fora do código)

```bash
ollama pull llama3.2:1b
ollama pull nomic-embed-text
```

### Passo 2 — `pom.xml`

```xml
<!-- ANTES -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>

<!-- DEPOIS -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
</dependency>
```

### Passo 3 — `application.yml`

```yaml
# ANTES
spring:
  ai:
    openai:
      api-key: ${OPEN_ROUTER_API_KEY}
      base-url: https://openrouter.ai/api
      chat:
        options:
          model: llama3.2:1b
      embedding:
        options:
          model: nomic-embed-text

# DEPOIS
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: llama3.2:1b
      embedding:
        options:
          model: nomic-embed-text
```

### Passo 4 — Remover `OpenAiConfig.java`

Arquivo: `src/main/java/br/com/chatbot/chatbot_api/config/OpenAiConfig.java`

O Ollama starter auto-configura `OllamaChatModel` e `OllamaEmbeddingModel`.

### Passo 5 — `BotServiceImpl.java`

- Import: `OpenAiChatModel` → `OllamaChatModel`
- Import: `OpenAiChatOptions` → `OllamaChatOptions`
- Campo: `OpenAiChatModel` → `OllamaChatModel`
- `@Value`: `spring.ai.openai.chat.options.model` → `spring.ai.ollama.chat.options.model`
- Options builder: `OpenAiChatOptions.builder()` → `OllamaChatOptions.builder()`

### Passo 6 — `EmbeddingServiceImpl.java`

- Import: `OpenAiEmbeddingModel` → `OllamaEmbeddingModel`
- Import: `OpenAiEmbeddingOptions` → `OllamaEmbeddingOptions`
- Campo: `OpenAiEmbeddingModel` → `OllamaEmbeddingModel`
- `@Value`: `spring.ai.openai.embedding.options.model` → `spring.ai.ollama.embedding.options.model`
- Options builder: `OpenAiEmbeddingOptions.builder()` → `OllamaEmbeddingOptions.builder()`

### Passo 7 — `BotServiceImplTest.java`

- Import: `OpenAiChatModel` → `OllamaChatModel`
- Mock: `@Mock private OpenAiChatModel` → `@Mock private OllamaChatModel`
- `chatModelId` value: `"gpt-4o"` → `"llama3.2:1b"`

### Passo 8 — `.env`

Remover linha: `OPEN_ROUTER_API_KEY=sk-or-v1-...`

### Passo 9 — `docker-compose.yml`

Adicionar serviço:

```yaml
ollama:
  image: ollama/ollama
  container_name: chatbot-ollama
  ports:
    - "11434:11434"
  volumes:
    - ollama_data:/root/.ollama
  deploy:
    resources:
      reservations:
        devices:
          - capabilities: [gpu]
```

E adicionar `ollama_data:` em `volumes:`.

### Passo 10 — Verificar

```bash
./mvnw clean compile -q
./mvnw test -q
```

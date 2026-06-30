# Debuging 3 — Pontos de Infra Remanescentes

## Status atual

Após as correções do debuging_1 (migrations) e debuging_2 (config + pgvector), a aplicação **sobe e responde na porta 8080**. Este documento mapeia os pontos de melhoria restantes para garantir que a build seja 100% reproduzível e os serviços externos funcionem corretamente.

---

## 🔴 1. spring-dotenv — `.env` não era carregado como ambiente

### Problema
O Spring Boot **não lê arquivos `.env` por padrão**. As chaves de serviços externos ficavam indisponíveis:
- `OPEN_ROUTER_API_KEY` → chamadas de chat/embedding falham
- `JWT_SECRET` → app não sobe
- `N8N_WEBHOOK_URL` → notificação não enviada

### O que foi feito
Adicionada dependência `me.paulschwarz:spring-dotenv:4.0.0` no `pom.xml`. Esta biblioteca:
1. Lê o arquivo `.env` da raiz do projeto
2. Injeta os valores como **environment variables simuladas**
3. Torna as chaves acessíveis via `${CHAVE}` em qualquer lugar do Spring
4. Permite que Relaxed Binding funcione (ex: `N8N_WEBHOOK_URL` → `app.n8n.webhook-url`)

Removida a tentativa anterior com `spring.config.import=file:.env[.properties]` que não garantia o binding correto para todas as chaves.

### Verificação
```bash
# Confirmar que a dependência está no classpath
mvn dependency:tree | findstr spring-dotenv
```

---

## 🔴 2. Construtores `OpenAiChatModel` / `OpenAiApi` deprecated

### Problema
O log exibe:
```
WARN o.s.ai.openai.OpenAiChatModel: This constructor is deprecated and will be removed in the next milestone.
```

O `OpenAiConfig.java` usa `new OpenAiChatModel(api, options)` e `new OpenAiApi(baseUrl, apiKey)`, ambos marcados como `@Deprecated(since = "1.0.0-M6")`. Em versões futuras do Spring AI, estes construtores serão removidos.

### Solução
Migrar `OpenAiConfig.java` para usar os Builders:

**Antes (deprecated):**
```java
var api = new OpenAiApi(baseUrl, apiKey);
var options = OpenAiChatOptions.builder().model(model).build();
return new OpenAiChatModel(api, options);
```

**Depois:**
```java
var api = OpenAiApi.builder()
    .baseUrl(baseUrl)
    .apiKey(apiKey)
    .build();
var options = OpenAiChatOptions.builder().model(model).build();
return OpenAiChatModel.builder()
    .openAiApi(api)
    .defaultOptions(options)
    .build();
```

### Impacto
- 🟡 **Não bloqueante hoje**, mas quebra na próxima milestone do Spring AI.

---

## 🟡 3. `JWT_TIMEOUT` não declarado no `.env`

### Problema
O `application.yml` usa `${JWT_TIMEOUT:86400000}` que funciona com o default. Mas para consistência, deveria estar explícito no `.env`.

### Solução
Adicionar ao `.env`:
```env
JWT_TIMEOUT=86400000
```

---

## 🟡 4. Chave `API_KEY_OPEN_CODE` no `.env` sem uso

### Problema
O `.env` contém `API_KEY_OPEN_CODE=sk-...` que não é referenciada por nenhuma configuração ou código. Provavelmente sobrou de uma versão anterior.

### Solução
Remover do `.env` para evitar confusão:
- Verificar se há alguma referência no código com `grep -r "API_KEY_OPEN_CODE"`
- Se não houver, remover a linha

---

## 🟢 5. `spring.jpa.properties.hibernate.dialect` removido

O log exibia:
```
HHH90000025: PostgreSQLDialect does not need to be specified explicitly
```

Foi removido do `application.yml`. O Hibernate detecta automaticamente a partir da URL JDBC. ✅

---

## 🟢 6. Flyway baseline-on-migrate

`spring.flyway.baseline-on-migrate: true` permite que o Flyway aplique migrations em schemas não-vazios. ✅ Porém, em produção, isso pode mascarar problemas — recomenda-se usar `false` com `baseline-version` explícito quando necessário.

---

## 🔴 7. Serviços externos — dependentes do `.env`

| Serviço | Chave no `.env` | Efeito se ausente |
|---------|----------------|-------------------|
| OpenRouter (chat) | `OPEN_ROUTER_API_KEY` | Chat/embedding retornam 401 |
| JWT | `JWT_SECRET` | App não inicia |
| n8n webhook | `N8N_WEBHOOK_URL` | Notificação de documentos não enviada |
| PostgreSQL | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | App não conecta ao banco |

Após adicionar `spring-dotenv`, todas estas chaves são carregadas automaticamente do `.env`.

---

## Ordem de verificação pós-correção

```
1.  🔴 Confirmar spring-dotenv no classpath
2.  🔴 Compilar sem erros (mvn clean compile)
3.  🔴 Rodar testes (mvn test)
4.  🔴 Subir app (mvn spring-boot:run)
5.  🟡 Migrar OpenAiConfig para Builder
6.  🟡 Adicionar JWT_TIMEOUT ao .env
7.  🟡 Remover API_KEY_OPEN_CODE do .env
```

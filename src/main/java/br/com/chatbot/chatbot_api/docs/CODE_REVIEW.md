# Code Review — Chatbot API

> **Data:** 26/06/2026  
> **Revisor:** Arquiteto de Software  
> **Escopo:** Código existente (excluindo service/repository/mapper — sob responsabilidade de outro colaborador)

---

## Sumário

| Gravidade                                  | Qtd |
| ------------------------------------------ | --- |
| 🔴 Crítico (impede build ou funcionamento) | 3   |
| 🟡 Alto (comportamento incorreto)          | 7   |
| 🔵 Médio (violação de boas práticas)       | 4   |
| ⚪ Baixo (cosmético/sugestão)              | 3   |

---

## 1. 🔴 Críticos

### 1.1 POM: Spring Boot parent version `4.1.0` — não existe

**Arquivo:** `pom.xml:8`  
**Problema:** A versão `4.1.0` do `spring-boot-starter-parent` não existe no ecossistema Spring. Spring Boot 4 não foi lançado (atualmente o stable é 3.3.x ou 3.4.x).  
**Impacto:** Build quebra imediatamente (`mvn compile` falha).  
**Correção:**

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.6</version>
    <relativePath/>
</parent>
```

---

### 1.2 POM: Dependências com artifactId inválido (5 ocorrências)

**Arquivo:** `pom.xml:35,51,71-89`  
**Problema:** Os seguintes artifact IDs não existem no ecossistema Spring Boot oficial:

| Linha | Artifact                                                          |
| ----- | ----------------------------------------------------------------- |
| 35    | `spring-boot-h2console`                                           |
| 51    | `spring-boot-starter-webmvc` (correto: `spring-boot-starter-web`) |
| 72    | `spring-boot-starter-data-jpa-test`                               |
| 76    | `spring-boot-starter-flyway-test`                                 |
| 80    | `spring-boot-starter-validation-test`                             |
| 84    | `spring-boot-starter-webmvc-test`                                 |

**Impacto:** Maven não resolve essas dependências → build falha.  
**Correção:** Substituir por:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

Remover `spring-boot-h2console`, `spring-boot-starter-*-test` (exceto `spring-boot-starter-test`).

---

### 1.3 POM: Swagger sem dependência — `SwaggerConfig.java` não compila

**Arquivo:** `pom.xml`, `SwaggerConfig.java:3-6`  
**Problema:** O código importa `io.swagger.v3.oas.models.OpenAPI` mas não há dependência `springdoc-openapi-starter-webmvc-ui` no pom.xml.  
**Impacto:** O arquivo `SwaggerConfig.java` não compila.  
**Correção:** Adicionar a dependência listada no item 1.2.

---

## 2. 🟡 Alta Gravidade

### 2.1 Entities: Nomes de tabela divergentes do Flyway (3 ocorrências)

| Arquivo                       | `@Table(name = ...)`      | Esperado (Flyway)        |
| ----------------------------- | ------------------------- | ------------------------ |
| `entity/Conversation.java:22` | `conversation` (singular) | `conversations` (plural) |
| `entity/Message.java:24`      | `message` (singular)      | `messages` (plural)      |
| `entity/Attachment.java:21`   | `attachment` (singular)   | `attachments` (plural)   |

**Problema:** As migrations Flyway (V1, V2, V3) criam as tabelas no plural conforme a especificação (`conversations`, `messages`, `attachments`). As entidades JPA apontam para nomes no singular.  
**Impacto:** `NoSuchTableException` em runtime — a aplicação sobe mas qualquer operação JPA falha.  
**Correção:** Alterar `@Table(name = "conversation")` → `@Table(name = "conversations")` (e同理 para Message e Attachment).

---

### 2.2 Entities: Ausência de `@PrePersist`/`@PreUpdate` — timestamps null

**Arquivo:** `entity/Conversation.java:37-41`, `entity/Message.java:47-48`, `entity/Attachment.java:46-47`  
**Problema:** Os campos `createdAt`, `updatedAt` e `uploadDate` são marcados como `nullable = false` mas **não possuem `@PrePersist`** para popular automaticamente. Se o Service não setar manualmente, o banco lança `ConstraintViolationException`.  
**Impacto:** Ao criar uma Conversation, se o Service esquecer de setar `createdAt`, o INSERT falha.  
**Correção:** Adicionar nas entities:

```java
@PrePersist
public void prePersist() {
    var now = LocalDateTime.now();
    this.createdAt = now;
    this.updatedAt = now;
}

@PreUpdate
public void preUpdate() {
    this.updatedAt = LocalDateTime.now();
}
```

E nos casos de Message e Attachment, `@PrePersist` para `createdAt`/`uploadDate`.

---

### 2.3 Attachment: `size` é palavra reservada no H2

**Arquivo:** `entity/Attachment.java:43-44`  
**Problema:** A coluna `size` é uma palavra-chave reservada no H2 (e em alguns bancos SQL).  
**Impacto:** Pode causar erro de sintaxe SQL em certos contextos ou limitação em queries futuras.  
**Correção:**

```java
@Column(name = "file_size", nullable = false)
private Long size;
```

Atualizar também o DTO `AttachmentResponse` e a migration V3 se já existir.

---

### 2.4 ChatController: HTTP status 201 CREATED — deve ser 200 OK

**Arquivo:** `controller/ChatController.java:33-34`  
**Problema:** `POST /api/chat/send` retorna `HttpStatus.CREATED` (201). Segundo a especificação, este endpoint **não cria um recurso novo** — ele processa uma mensagem e retorna o resultado da interação.  
**Impacto:** Cliente pode interpretar que um novo recurso foi criado em uma URL específica, o que não é verdade.  
**Correção:** Mudar para `ResponseEntity.ok(response)` (200 OK).

---

### 2.5 ErrorResponse: Campo `path` ausente — fora da especificação

**Arquivo:** `dto/response/ErrorResponse.java:6-11`  
**Problema:** A especificação define `ErrorResponse` com campo `path` (caminho da requisição que gerou o erro). O código atual tem `List<String> details` no lugar.  
**Impacto:** Respostas de erro inconsistentes com o contrato da API — consumidores esperam `path` e não `details`.  
**Correção:**

```java
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,            // ← adicionar
        LocalDateTime timestamp
) {}
```

E no `GlobalExceptionHandler`, injetar `HttpServletRequest` para obter o `path`:

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleResourceNotFound(
        ResourceNotFoundException ex, HttpServletRequest request) {
    var response = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getRequestURI(),
            LocalDateTime.now()
    );
    ...
}
```

---

### 2.6 GlobalExceptionHandler: Não captura `HttpMessageNotReadableException`

**Arquivo:** `exception/GlobalExceptionHandler.java`  
**Problema:** Se o cliente enviar um JSON malformado no corpo da requisição, o Spring lança `HttpMessageNotReadableException`, que não é tratada → cai no handler genérico (500 Internal Server Error).  
**Impacto:** Erro de sintaxe JSON retorna 500 em vez de 400 Bad Request.  
**Correção:** Adicionar handler:

```java
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<ErrorResponse> handleMalformedJson(
        HttpMessageNotReadableException ex, HttpServletRequest request) {
    var response = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Malformed JSON request body",
            request.getRequestURI(),
            LocalDateTime.now()
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
}
```

---

### 2.7 GlobalExceptionHandler: Handler genérico suprime a mensagem real

**Arquivo:** `exception/GlobalExceptionHandler.java:75`  
**Problema:** O handler de `Exception.class` retorna a mensagem genérica `"An unexpected error occurred"` em vez de incluir `ex.getMessage()`. Isso dificulta debugging em produção.  
**Impacto:** Erros inesperados são ocultados do cliente e dificultam diagnóstico.  
**Correção:**

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGeneral(
        Exception ex, HttpServletRequest request) {
    log.error("Unexpected error", ex);  // log para o servidor
    var response = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            ex.getMessage(),            // ← mensagem real
            request.getRequestURI(),
            LocalDateTime.now()
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
}
```

---

## 3. 🔵 Média Gravidade

### 3.1 Faltando `repository/` — services do outro colaborador precisam

**Arquivo:** `src/main/java/br/com/chatbot/chatbot_api/`  
**Problema:** O pacote `repository` não existe. Os services (a serem criados por outro colaborador) dependerão de `ConversationRepository`, `MessageRepository` e `AttachmentRepository`. Sem eles, os services não compilam.  
**Correção:** Criar as interfaces básicas:

```java
public interface ConversationRepository extends JpaRepository<Conversation, Long> {}
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {}
```

---

### 3.2 Faltando `mapper/` — services precisarão converter Entity → DTO

**Arquivo:** `src/main/java/br/com/chatbot/chatbot_api/`  
**Problema:** O pacote `mapper` não existe. Services precisarão de mappers para converter Entity → DTO sem expor entidades para os controllers.  
**Correção:** Criar mappers como componentes Spring ou classes estáticas (ex: `ConversationMapper`, `MessageMapper`, `AttachmentMapper`).

---

### 3.3 Faltando `application.yml` — Flyway e H2 não configurados

**Arquivo:** `src/main/resources/application.properties`  
**Problema:** O arquivo atual só contém `spring.application.name=chatbot-api`. Não há configuração de datasource H2, Flyway, JPA, console H2, etc.  
**Impacto:** A aplicação sobe mas não conecta a banco nenhum — JPA/Hibernate não tem datasource configurado.  
**Correção:** Converter para `application.yml` com:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:chatbotdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true
    database-platform: org.hibernate.dialect.H2Dialect
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

---

### 3.4 Faltando migrations Flyway — banco vazio

**Arquivo:** `src/main/resources/db/migration/` (vazio)  
**Problema:** Sem migrations, as tabelas `conversations`, `messages` e `attachments` nunca são criadas. Com `ddl-auto: validate`, a aplicação nem sobe.  
**Correção:** Criar:

- `V1__create_conversations.sql`
- `V2__create_messages.sql`
- `V3__create_attachments.sql`

---

## 4. ⚪ Baixa Gravidade (Sugestões)

### 4.1 `HealthResponse` com `@JsonProperty` redundante

**Arquivo:** `dto/response/HealthResponse.java`  
**Sugestão:** `@JsonProperty` é desnecessário em records Java — os nomes dos componentes já são usados como nomes de campo pelo Jackson. Pode remover para simplificar.

---

### 4.2 `SwaggerConfig.java` — Nome inconsistente com a especificação

**Arquivo:** `config/SwaggerConfig.java`  
**Sugestão:** A especificação sugere `OpenApiConfig.java` (sem o "w"). Renomear para manter consistência com o documento de especificação.

---

### 4.3 `ConversationService` será chamado pelo controller mas não existe

**Arquivo:** `controller/ConversationController.java:28`  
**Sugestão:** Já combinar com o colaborador dos services a assinatura exata da interface `ConversationService` para evitar incompatibilidade na integração. Definir o contrato (interface) antes da implementação reduz risco de retrabalho.

---

## 5. Resumo por Arquivo

| Arquivo                                  | Achados                                                                   |
| ---------------------------------------- | ------------------------------------------------------------------------- |
| `pom.xml`                                | 🔴 Parent 4.1.0; 🔴 6 dependências inválidas; 🔴 Falta Swagger, Web, Test |
| `ChatbotApiApplication.java`             | ✅ OK                                                                     |
| `config/SwaggerConfig.java`              | 🔴 Não compila sem dep. Swagger; ⚪ Renomear                              |
| `controller/HealthController.java`       | ✅ OK                                                                     |
| `controller/ConversationController.java` | ✅ OK                                                                     |
| `controller/ChatController.java`         | 🟡 201 → 200 OK                                                           |
| `controller/FileController.java`         | ✅ OK (depende de FileService)                                            |
| `entity/Conversation.java`               | 🟡 @Table singular; 🟡 sem @PrePersist                                    |
| `entity/Message.java`                    | 🟡 @Table singular; 🟡 sem @PrePersist                                    |
| `entity/Attachment.java`                 | 🟡 @Table singular; 🟡 sem @PrePersist; 🟡 coluna `size`                  |
| `enums/MessageRole.java`                 | ✅ OK                                                                     |
| `dto/request/*.java`                     | ✅ OK                                                                     |
| `dto/response/ErrorResponse.java`        | 🟡 falta `path`                                                           |
| `dto/response/HealthResponse.java`       | ⚪ @JsonProperty redundante                                               |
| `dto/response/*.java` (demais)           | ✅ OK                                                                     |
| `exception/GlobalExceptionHandler.java`  | 🟡 sem path; 🟡 falta HttpMessageNotReadable; 🟡 suprime msg real         |
| `exception/*.java` (demais)              | ✅ OK                                                                     |
| `ChatbotApiApplicationTests.java`        | ✅ OK                                                                     |
| `application.properties`                 | 🔵 sem H2/Flyway/JPA config                                               |
| `db/migration/`                          | 🔵 vazio — sem migrations                                                 |

---

## 6. Checklist de Correções Prioritárias

- [ ] 1. **pom.xml**: Parent `3.3.6`, remover deps inválidas, adicionar `spring-boot-starter-web`, `spring-boot-starter-test`, `springdoc-openapi-starter-webmvc-ui`
- [ ] 2. **Entities**: `@Table` nomes no plural (`conversations`, `messages`, `attachments`)
- [ ] 3. **Entities**: Adicionar `@PrePersist`/`@PreUpdate` em todas
- [ ] 4. **Attachment**: Renomear coluna `size` → `file_size`
- [ ] 5. **ChatController**: 201 → 200 OK
- [ ] 6. **ErrorResponse**: Adicionar campo `path`
- [ ] 7. **GlobalExceptionHandler**: Injetar `HttpServletRequest`, adicionar `HttpMessageNotReadableException`, expor mensagem real no handler genérico
- [ ] 8. **application.yml**: Configurar H2, Flyway, JPA
- [ ] 9. **Migrations**: Criar V1, V2, V3
- [ ] 10. **repository/ + mapper/**: Criar interfaces básicas (combinar com colaborador dos services)

---

_Documento gerado automaticamente após análise estática do código-fonte. Itens marcados como responsabilidade de outro colaborador (services) não foram analisados em profundidade._

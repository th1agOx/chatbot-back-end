# Chatbot API — Documento de Especificação do Sistema

> **Versão:** 1.1  
> **Última atualização:** 25/06/2026  
> **Stack:** Java 21 + Spring Boot 3 + Maven + H2 + Flyway + Swagger

---

## Índice

1. [Stack Tecnológica](#1-stack-tecnológica)
2. [Arquitetura — Clean Architecture Adaptada](#2-arquitetura--clean-architecture-adaptada)
3. [Responsabilidades e Regras de Isolamento](#3-responsabilidades-e-regras-de-isolamento)
4. [Modelo de Dados](#4-modelo-de-dados)
5. [Contratos da API REST](#5-contratos-da-api-rest)
6. [Estrutura de Pacotes Final](#6-estrutura-de-pacotes-final)
7. [Flyway Migrations](#7-flyway-migrations)
8. [Tratamento de Exceções](#8-tratamento-de-exceções)
9. [Swagger/OpenAPI](#9-swaggeropenapi)
10. [Estratégia de Testes](#10-estratégia-de-testes)
11. [Ordem de Implementação](#11-ordem-de-implementação)
12. [Regras Arquiteturais Inegociáveis](#12-regras-arquiteturais-inegociáveis)

---

## 1. Stack Tecnológica

| Camada       | Tecnologia                                  |
| ------------ | ------------------------------------------- |
| Linguagem    | Java 21                                     |
| Framework    | Spring Boot 3.3.x                           |
| Build        | Maven                                       |
| Web          | Spring Web (MVC)                            |
| Persistência | Spring Data JPA                             |
| Banco        | H2 (memória)                                |
| Migrations   | Flyway                                      |
| Validação    | Spring Validation (Jakarta Bean Validation) |
| Documentação | SpringDoc OpenAPI (Swagger UI)              |
| Testes       | JUnit 5 + Mockito                           |
| Utilidades   | Lombok                                      |

### Dependências Maven (pom.xml)

```xml
<!-- Parent -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.6</version>
</parent>

<!-- Dependências principais -->
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-validation
spring-boot-starter-flyway
springdoc-openapi-starter-webmvc-ui
h2 (runtime)
lombok (optional)

<!-- Testes -->
spring-boot-starter-test (test)
```

> ⚠ **Importante:** Não usar `spring-boot-starter-webmvc`, `spring-boot-h2console`, nem `spring-boot-starter-data-jpa-test` — essas coordenadas não existem no ecossistema Spring oficial.

---

## 2. Arquitetura — Clean Architecture Adaptada

```
┌──────────────────────────────────────────────────────────┐
│                    Controller Layer                       │
│  Apenas recebe HTTP, delega para Service, retorna DTO    │
├──────────────────────────────────────────────────────────┤
│                     Service Layer                         │
│  Lógica de negócio, orquestração, regras                 │
├──────────────────────────────────────────────────────────┤
│                    Repository Layer                       │
│  Acesso a dados (Spring Data JPA)                        │
├──────────────────────────────────────────────────────────┤
│                     Entity Layer                          │
│  Mapeamento JPA — NUNCA exposto para fora                │
└──────────────────────────────────────────────────────────┘
```

### Fluxo de dados

```
HTTP Request
    ↓
Controller (recebe DTO Request)
    ↓
Service (lógica de negócio, chama repository)
    ↓
Repository (Spring Data JPA)
    ↓
Database (H2)
    ↓
Repository → retorna Entity
    ↓
Service → Mapper → converte Entity → DTO Response
    ↓
Controller → retorna DTO Response
    ↓
HTTP Response (JSON)
```

---

## 3. Responsabilidades e Regras de Isolamento

### 3.1 Controller Layer (`controller/`)

**Responsabilidade:** Parsear requisições HTTP, chamar services, retornar respostas HTTP com DTOs.

**Regras:**

- Nenhuma lógica de negócio
- Nenhum acesso a repositórios
- Nenhuma dependência de entidades JPA
- Anotações: `@RestController`, `@RequestMapping`, `@Valid`

**Controllers:**
| Controller | Endpoints |
|-----------|-----------|
| `HealthController` | `GET /health` |
| `ConversationController` | `POST/GET /api/conversations`, `GET/DELETE /api/conversations/{id}` |
| `ChatController` | `POST /api/chat/send`, `GET /api/chat/history/{conversationId}` |
| `FileController` | `POST /api/files/upload` |

### 3.2 Service Layer (`service/` + `service/impl/`)

**Responsabilidade:** Toda lógica de negócio — criar conversas, gerar respostas mock, validar regras, orquestrar persistência.

**Regras:**

- Services dependem de interfaces (contratos)
- Implementações em `impl/`
- Nunca expõem entidades — sempre convertem para DTOs via Mapper
- `BotService` é uma interface — implementação mock trocável futuramente por IA real

**Services (interfaces + impl):**

| Interface             | Implementação             | Responsabilidade               |
| --------------------- | ------------------------- | ------------------------------ |
| `ConversationService` | `ConversationServiceImpl` | CRUD de conversas              |
| `ChatService`         | `ChatServiceImpl`         | Envio de mensagens, histórico  |
| `BotService`          | `MockBotServiceImpl`      | Geração de resposta simulada   |
| `FileService`         | `FileServiceImpl`         | Upload e validação de arquivos |

**Estratégia de substituição do BotService:**

```
BotService (interface)
    ├── MockBotServiceImpl     ← atual (mock)
    ├── OpenAiBotServiceImpl   ← futuro
    ├── GeminiBotServiceImpl   ← futuro
    └── OllamaBotServiceImpl   ← futuro
```

A troca ocorre apenas na configuração do Spring (injeção de dependência) — **nenhum Controller ou Service existente é alterado.**

### 3.3 Repository Layer (`repository/`)

**Responsabilidade:** Operações de banco via Spring Data JPA.

**Regras:**

- Apenas métodos de consulta/persistência
- Nenhuma regra de negócio
- Extendem `JpaRepository<T, Long>`

**Repositories:**

- `ConversationRepository`
- `MessageRepository`
- `AttachmentRepository`

### 3.4 Entity Layer (`entity/`)

**Responsabilidade:** Mapeamento objeto-relacional (JPA).

**Regras:**

- Anotações `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@ManyToOne`, `@Enumerated`
- Usar `@Column` com nomes explícitos (snake_case)
- `@CreatedDate` / `@UpdatedDate` ou controle manual via `LocalDateTime.now()`
- Jamais retornadas em controllers
- Convertidas para DTOs via **Mapper Layer**

**Entidades:**

- `Conversation`
- `Message`
- `Attachment`

### 3.5 Mapper Layer (`mapper/`)

**Responsabilidade:** Conversão Entity ↔ DTO.

**Regras:**

- Implementação manual (sem MapStruct para evitar complexidade inicial)
- Métodos estáticos ou componentes Spring `@Component`
- Conversão bidirecional quando necessário

**Mappers:**

- `ConversationMapper`
- `MessageMapper`
- `AttachmentMapper`

### 3.6 DTO Layer (`dto/request/`, `dto/response/`)

**Responsabilidade:** Contratos de entrada/saída da API.

**Regras:**

- Record classes imutáveis (Java 14+)
- `request/` para entrada com validação (`@NotBlank`, `@NotNull`)
- `response/` para saída
- Anotações Swagger nos campos quando necessário (`@Schema`)

**DTOs Request:**

- `CreateConversationRequest` — `String title`
- `SendMessageRequest` — `Long conversationId`, `String message`

**DTOs Response:**

- `HealthResponse` — `String status`, `String timestamp`
- `ConversationResponse` — `Long id`, `String title`, `LocalDateTime createdAt`, `LocalDateTime updatedAt`
- `MessageResponse` — `Long id`, `String role`, `String content`, `LocalDateTime createdAt`
- `ChatResponse` — `MessageResponse userMessage`, `MessageResponse botMessage`
- `AttachmentResponse` — `Long id`, `String fileName`, `String contentType`, `Long size`, `LocalDateTime uploadDate`
- `ErrorResponse` — `int status`, `String error`, `String message`, `String path`, `LocalDateTime timestamp`

### 3.7 Exception Handler (`exception/`)

**Responsabilidade:** Tratamento global de exceções.

**Regras:**

- `@RestControllerAdvice`
- Respostas padronizadas no formato `ErrorResponse`
- Tratar: `MethodArgumentNotValidException`, `ResponseStatusException`, `ResourceNotFoundException`, `InvalidFileTypeException`, `ConstraintViolationException`, `Exception` genérica

**Classes:**

- `GlobalExceptionHandler` — handler central
- `ResourceNotFoundException` — exceção customizada para 404
- `InvalidFileTypeException` — exceção customizada para upload inválido

### 3.8 Validation Layer (`validation/`)

**Responsabilidade:** Validadores customizados.

**Regras:**

- Anotações customizadas para validação de arquivo
- Implementação de `ConstraintValidator`

**Classes:**

- `FileTypeValidator` — valida extensão/content-type do arquivo

### 3.9 Config Layer (`config/`)

**Responsabilidade:** Beans de configuração.

**Classes:**

- `OpenApiConfig` — configuração do Swagger/OpenAPI
- `CorsConfig` — configuração CORS (permitir requisições do front-end)

### 3.10 Enums (`enums/`)

**Responsabilidade:** Tipos enumerados.

**Enum:**

- `Role` — `USER`, `BOT`

### 3.11 Util (`util/`)

**Responsabilidade:** Funções auxiliares puras.

**Classes:**

- `DateTimeUtil` — formatação de timestamps

### 3.12 Docs (`docs/`)

**Responsabilidade:** Constantes de documentação Swagger para manter controllers limpos.

**Conteúdo:** Constantes `String` com summaries e descriptions de cada endpoint.

---

## 4. Modelo de Dados

### 4.1 Diagrama Entidade-Relacionamento

```
┌───────────────────────┐       ┌───────────────────────┐
│     conversations     │       │       messages        │
├───────────────────────┤       ├───────────────────────┤
│ id (BIGINT, PK)       │──┐    │ id (BIGINT, PK)       │
│ title (VARCHAR(255))  │  │    │ conversation_id (FK)  │── FK → conversations.id
│ created_at (DATETIME) │  │    │ role (VARCHAR(10))    │
│ updated_at (DATETIME) │  │    │ content (CLOB/TEXT)   │
└───────────────────────┘  │    │ created_at (DATETIME) │
                           │    └───────────────────────┘
                           │
                           │    ┌───────────────────────┐
                           │    │      attachments      │
                           │    ├───────────────────────┤
                           └────│ id (BIGINT, PK)       │
                                │ conversation_id (FK)  │── FK → conversations.id
                                │ file_name (VARCHAR)   │
                                │ content_type (VARCHAR)│
                                │ size (BIGINT)         │
                                │ upload_date (DATETIME)│
                                └───────────────────────┘
```

### 4.2 Relacionamentos JPA

| Entidade       | Relacionamento                          | Entidade alvo  |
| -------------- | --------------------------------------- | -------------- |
| `Conversation` | `@OneToMany(mappedBy = "conversation")` | `Message`      |
| `Conversation` | `@OneToMany(mappedBy = "conversation")` | `Attachment`   |
| `Message`      | `@ManyToOne(fetch = LAZY)`              | `Conversation` |
| `Attachment`   | `@ManyToOne(fetch = LAZY)`              | `Conversation` |

---

## 5. Contratos da API REST

### 5.1 Health Check

```
GET /health

Response 200 (application/json):
{
    "status": "UP",
    "timestamp": "2026-06-25T10:30:00.123456Z"
}
```

### 5.2 Conversas

#### Criar conversa

```
POST /api/conversations
Content-Type: application/json

Request Body:
{
    "title": "Minha primeira conversa"
}

Response 201 (application/json):
{
    "id": 1,
    "title": "Minha primeira conversa",
    "createdAt": "2026-06-25T10:30:00.123",
    "updatedAt": "2026-06-25T10:30:00.123"
}

Response 400 (validação):
{
    "status": 400,
    "error": "Bad Request",
    "message": "title: não deve estar em branco",
    "path": "/api/conversations",
    "timestamp": "2026-06-25T10:30:00.123"
}
```

#### Listar conversas

```
GET /api/conversations

Response 200 (application/json):
[
    {
        "id": 1,
        "title": "Minha primeira conversa",
        "createdAt": "2026-06-25T10:30:00.123",
        "updatedAt": "2026-06-25T10:30:00.123"
    }
]
```

#### Buscar conversa por ID

```
GET /api/conversations/{id}

Response 200 (application/json):
{
    "id": 1,
    "title": "Minha primeira conversa",
    "createdAt": "2026-06-25T10:30:00.123",
    "updatedAt": "2026-06-25T10:30:00.123"
}

Response 404:
{
    "status": 404,
    "error": "Not Found",
    "message": "Conversa não encontrada com id: 1",
    "path": "/api/conversations/1",
    "timestamp": "2026-06-25T10:30:00.123"
}
```

#### Excluir conversa

```
DELETE /api/conversations/{id}

Response 204 (no content)

Response 404:
{
    "status": 404,
    "error": "Not Found",
    "message": "Conversa não encontrada com id: 1",
    "path": "/api/conversations/1",
    "timestamp": "2026-06-25T10:30:00.123"
}
```

### 5.3 Chat (Mensagens)

#### Enviar mensagem

```
POST /api/chat/send
Content-Type: application/json

Request Body:
{
    "conversationId": 1,
    "message": "Olá!"
}

Fluxo interno:
1. Buscar Conversation pelo conversationId (404 se não existir)
2. Salvar Message com role=USER
3. Chamar BotService.generateResponse(message)
4. Salvar Message com role=BOT
5. Retornar ChatResponse com ambas as mensagens

Response 200 (application/json):
{
    "userMessage": {
        "id": 1,
        "role": "USER",
        "content": "Olá!",
        "createdAt": "2026-06-25T10:30:00.123"
    },
    "botMessage": {
        "id": 2,
        "role": "BOT",
        "content": "Você disse: Olá!",
        "createdAt": "2026-06-25T10:30:00.456"
    }
}

Response 404 (conversationId inválido):
{
    "status": 404,
    "error": "Not Found",
    "message": "Conversa não encontrada com id: 999",
    "path": "/api/chat/send",
    "timestamp": "2026-06-25T10:30:00.123"
}
```

#### Buscar histórico de mensagens

```
GET /api/chat/history/{conversationId}

Response 200 (application/json):
[
    {
        "id": 1,
        "role": "USER",
        "content": "Olá!",
        "createdAt": "2026-06-25T10:30:00.123"
    },
    {
        "id": 2,
        "role": "BOT",
        "content": "Você disse: Olá!",
        "createdAt": "2026-06-25T10:30:00.456"
    }
]

Response 404:
{
    "status": 404,
    "error": "Not Found",
    "message": "Conversa não encontrada com id: 999",
    "path": "/api/chat/history/999",
    "timestamp": "2026-06-25T10:30:00.123"
}
```

### 5.4 Upload de Arquivos

#### Fazer upload

```
POST /api/files/upload
Content-Type: multipart/form-data

Request:
- file: (arquivo .txt ou .pdf)
- conversationId: (long)

Validações:
- Tipo: apenas PDF (application/pdf) e TXT (text/plain)
- Tamanho: máximo 10 MB

Response 201 (application/json):
{
    "id": 1,
    "fileName": "documento.pdf",
    "contentType": "application/pdf",
    "size": 1024000,
    "uploadDate": "2026-06-25T10:30:00.123"
}

Response 400 (tipo inválido):
{
    "status": 400,
    "error": "Bad Request",
    "message": "Tipo de arquivo não suportado: image/png. Tipos aceitos: application/pdf, text/plain",
    "path": "/api/files/upload",
    "timestamp": "2026-06-25T10:30:00.123"
}

Response 400 (tamanho excedido):
{
    "status": 400,
    "error": "Bad Request",
    "message": "Arquivo excede o tamanho máximo permitido de 10MB",
    "path": "/api/files/upload",
    "timestamp": "2026-06-25T10:30:00.123"
}
```

### 5.5 DTOs — Definições Java (Records)

```java
// ================ REQUEST ================

public record CreateConversationRequest(
    @NotBlank(message = "O título é obrigatório")
    @Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
    String title
) {}

public record SendMessageRequest(
    @NotNull(message = "O ID da conversa é obrigatório")
    Long conversationId,

    @NotBlank(message = "A mensagem é obrigatória")
    @Size(max = 10000, message = "A mensagem deve ter no máximo 10000 caracteres")
    String message
) {}

// ================ RESPONSE ================

public record HealthResponse(
    String status,
    String timestamp
) {}

public record ConversationResponse(
    Long id,
    String title,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

public record MessageResponse(
    Long id,
    String role,
    String content,
    LocalDateTime createdAt
) {}

public record ChatResponse(
    MessageResponse userMessage,
    MessageResponse botMessage
) {}

public record AttachmentResponse(
    Long id,
    String fileName,
    String contentType,
    Long size,
    LocalDateTime uploadDate
) {}

public record ErrorResponse(
    int status,
    String error,
    String message,
    String path,
    LocalDateTime timestamp
) {}
```

---

## 6. Estrutura de Pacotes Final

```
src/main/java/br/com/chatbot/chatbot_api/
├── ChatbotApiApplication.java
│
├── config/
│   ├── OpenApiConfig.java
│   └── CorsConfig.java
│
├── controller/
│   ├── HealthController.java
│   ├── ConversationController.java
│   ├── ChatController.java
│   └── FileController.java
│
├── dto/
│   ├── request/
│   │   ├── CreateConversationRequest.java
│   │   └── SendMessageRequest.java
│   └── response/
│       ├── HealthResponse.java
│       ├── ConversationResponse.java
│       ├── MessageResponse.java
│       ├── ChatResponse.java
│       ├── AttachmentResponse.java
│       └── ErrorResponse.java
│
├── entity/
│   ├── Conversation.java
│   ├── Message.java
│   └── Attachment.java
│
├── enums/
│   └── Role.java
│
├── repository/
│   ├── ConversationRepository.java
│   ├── MessageRepository.java
│   └── AttachmentRepository.java
│
├── service/
│   ├── ConversationService.java          (interface)
│   ├── ChatService.java                 (interface)
│   ├── BotService.java                  (interface)
│   ├── FileService.java                 (interface)
│   └── impl/
│       ├── ConversationServiceImpl.java
│       ├── ChatServiceImpl.java
│       ├── MockBotServiceImpl.java
│       └── FileServiceImpl.java
│
├── mapper/
│   ├── ConversationMapper.java
│   ├── MessageMapper.java
│   └── AttachmentMapper.java
│
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── InvalidFileTypeException.java
│
├── validation/
│   └── FileTypeValidator.java
│
└── util/
    └── DateTimeUtil.java
```

```
src/main/resources/
├── application.yml
├── db/migration/
│   ├── V1__create_conversations.sql
│   ├── V2__create_messages.sql
│   └── V3__create_attachments.sql
├── static/
└── templates/
```

```
src/test/java/br/com/chatbot/chatbot_api/
├── service/
│   ├── ChatServiceImplTest.java
│   ├── ConversationServiceImplTest.java
│   ├── FileServiceImplTest.java
│   └── MockBotServiceImplTest.java
├── exception/
│   └── GlobalExceptionHandlerTest.java
└── ChatbotApiApplicationTest.java
```

---

## 7. Flyway Migrations

### V1\_\_create_conversations.sql

```sql
CREATE TABLE conversations (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

### V2\_\_create_messages.sql

```sql
CREATE TABLE messages (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(10) NOT NULL CHECK (role IN ('USER', 'BOT')),
    content CLOB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id)
        REFERENCES conversations(id) ON DELETE CASCADE
);

CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
```

### V3\_\_create_attachments.sql

```sql
CREATE TABLE attachments (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_attachments_conversation FOREIGN KEY (conversation_id)
        REFERENCES conversations(id) ON DELETE CASCADE
);

CREATE INDEX idx_attachments_conversation_id ON attachments(conversation_id);
```

---

## 8. Tratamento de Exceções

### GlobalExceptionHandler

| Exceção                           | HTTP Status | Formato                              |
| --------------------------------- | ----------- | ------------------------------------ |
| `ResourceNotFoundException`       | 404         | `ErrorResponse`                      |
| `InvalidFileTypeException`        | 400         | `ErrorResponse`                      |
| `MethodArgumentNotValidException` | 400         | `ErrorResponse` (mensagens de campo) |
| `ConstraintViolationException`    | 400         | `ErrorResponse`                      |
| `HttpMessageNotReadableException` | 400         | `ErrorResponse`                      |
| `MaxUploadSizeExceededException`  | 400         | `ErrorResponse`                      |
| `Exception` (genérica)            | 500         | `ErrorResponse`                      |

### Formato de erro padronizado

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Mensagem descritiva do erro",
  "path": "/api/conversations",
  "timestamp": "2026-06-25T10:30:00.123"
}
```

Para erros de validação de campo:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "title: O título é obrigatório; message: A mensagem é obrigatória",
  "path": "/api/chat/send",
  "timestamp": "2026-06-25T10:30:00.123"
}
```

---

## 9. Swagger/OpenAPI

### Configuração

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Chatbot API")
                .version("1.0")
                .description("API REST do backend de chatbot com suporte a conversas, mensagens e upload de arquivos")
            );
    }
}
```

### Documentação nos endpoints

Cada método de controller deve usar `@Operation`:

```java
@Operation(summary = "Enviar mensagem para o chatbot",
           description = "Salva a mensagem do usuário, gera resposta simulada do bot e retorna ambas")
```

### Acesso

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

---

## 10. Estratégia de Testes

### Framework

- JUnit 5 (`@ExtendWith(MockitoExtension.class)`)
- Mockito (`@Mock`, `@InjectMocks`, `when`, `verify`, `assertThat`)

### Escopo: services + exception handler

#### ChatServiceImplTest

| Cenário                           | Teste                                                                |
| --------------------------------- | -------------------------------------------------------------------- |
| Envio de mensagem com sucesso     | `sendMessage_ValidRequest_ReturnsChatResponse()`                     |
| Envio para conversa inexistente   | `sendMessage_ConversationNotFound_ThrowsResourceNotFoundException()` |
| Histórico de conversa existente   | `getHistory_ExistingConversation_ReturnsMessageList()`               |
| Histórico de conversa inexistente | `getHistory_ConversationNotFound_ThrowsResourceNotFoundException()`  |

#### ConversationServiceImplTest

| Cenário                            | Teste                                                        |
| ---------------------------------- | ------------------------------------------------------------ |
| Criar conversa com sucesso         | `create_ValidRequest_ReturnsConversationResponse()`          |
| Listar todas as conversas          | `findAll_ReturnsListOfConversationResponses()`               |
| Buscar conversa por ID existente   | `findById_ExistingId_ReturnsConversationResponse()`          |
| Buscar conversa por ID inexistente | `findById_NonExistingId_ThrowsResourceNotFoundException()`   |
| Excluir conversa existente         | `deleteById_ExistingId_DeletesSuccessfully()`                |
| Excluir conversa inexistente       | `deleteById_NonExistingId_ThrowsResourceNotFoundException()` |

#### FileServiceImplTest

| Cenário                          | Teste                                                           |
| -------------------------------- | --------------------------------------------------------------- |
| Upload de arquivo PDF válido     | `upload_ValidPdfFile_ReturnsAttachmentResponse()`               |
| Upload de arquivo TXT válido     | `upload_ValidTxtFile_ReturnsAttachmentResponse()`               |
| Upload de tipo inválido          | `upload_InvalidFileType_ThrowsInvalidFileTypeException()`       |
| Upload para conversa inexistente | `upload_ConversationNotFound_ThrowsResourceNotFoundException()` |

#### MockBotServiceImplTest

| Cenário        | Teste                                         |
| -------------- | --------------------------------------------- |
| Gerar resposta | `generateResponse_ReturnsFormattedResponse()` |

#### GlobalExceptionHandlerTest

| Cenário                         | Teste                                          |
| ------------------------------- | ---------------------------------------------- |
| ResourceNotFoundException       | `handleResourceNotFoundException_Returns404()` |
| MethodArgumentNotValidException | `handleValidationException_Returns400()`       |
| InvalidFileTypeException        | `handleInvalidFileTypeException_Returns400()`  |
| Exceção genérica                | `handleGenericException_Returns500()`          |

### Convenções de teste

- Nomes de método: `metodo_Condicao_ResultadoEsperado()`
- Padrão: Arrange → Act → Assert (AAA)
- Verificar interações com Mockito: `verify(repository, times(1)).save(...)`

---

## 11. Ordem de Implementação

| Fase   | Etapa                    | Descrição                                            |
| ------ | ------------------------ | ---------------------------------------------------- |
| 1      | Estrutura do projeto     | ✅ Criada                                            |
| 2      | pom.xml                  | ✅ Criado (requer correções)                         |
| **3**  | **application.yml + H2** | ⬜ Configurar datasource, console H2, JPA, Flyway    |
| **4**  | **Flyway migrations**    | ⬜ V1, V2, V3 (conversations, messages, attachments) |
| **5**  | **Entities + Enums**     | ⬜ Conversation, Message, Attachment, Role           |
| **6**  | **DTOs**                 | ⬜ request/ e response/ records                      |
| **7**  | **Repositories**         | ⬜ Interfaces JpaRepository                          |
| **8**  | **Services**             | ⬜ Interfaces + impl (Conversation, Chat, Bot, File) |
| **9**  | **Controllers**          | ⬜ Health, Conversation, Chat, File                  |
| **10** | **Exception Handler**    | ⬜ GlobalExceptionHandler + exceções customizadas    |
| **11** | **Swagger/OpenAPI**      | ⬜ OpenApiConfig + anotações @Operation              |
| **12** | **Testes**               | ⬜ JUnit 5 + Mockito                                 |
| **13** | **README**               | ⬜ Documentação final do projeto                     |

---

## 12. Regras Arquiteturais Inegociáveis

| #   | Regra                                                                                                 | Violação exemplar                                                           |
| --- | ----------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------- |
| 1   | **Controllers não contêm lógica de negócio** — apenas delegam para services e retornam DTOs           | `controller` chamar `repository.save()` diretamente                         |
| 2   | **Services concentram toda lógica** — nunca expõem entidades JPA                                      | `service` retornar `Conversation` (entity) em vez de `ConversationResponse` |
| 3   | **Repositories só fazem acesso a dados** — sem regras de negócio                                      | `repository` ter método que valida regras antes de salvar                   |
| 4   | **Entities jamais são retornadas em controllers** — mapper obrigatório                                | `controller` receber ou devolver `@Entity` como parâmetro                   |
| 5   | **DTOs de request usam Bean Validation** (`@NotBlank`, `@NotNull`, `@Size`)                           | DTO sem validação aceitando campos nulos                                    |
| 6   | **Tratamento de erros é centralizado** em um único `@RestControllerAdvice`                            | Cada controller ter seu próprio try-catch                                   |
| 7   | **BotService é injetado por interface** — troca de provider sem alterar controllers                   | `ChatServiceImpl` instanciar `new MockBotServiceImpl()` diretamente         |
| 8   | **Flyway migrations são a única fonte de verdade do schema** — não usar `ddl-auto=update` em produção | `spring.jpa.hibernate.ddl-auto=update` em produção                          |
| 9   | **Métodos pequenos e reutilizáveis** — máximo ~20 linhas por método                                   | Método de service com 100+ linhas e múltiplas responsabilidades             |
| 10  | **Imports organizados e sem wildcards** — evitar `import br.com.chatbot.**`                           | `import java.util.*`, `import br.com.chatbot.entity.*`                      |

---

> Este documento é o guia oficial de implementação do Chatbot API. Qualquer desvio arquitetural deve ser aprovado pela equipe antes de ser incorporado ao código.

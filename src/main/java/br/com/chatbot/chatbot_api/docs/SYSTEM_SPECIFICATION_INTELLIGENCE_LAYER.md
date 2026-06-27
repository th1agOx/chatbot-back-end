# Chatbot API — Camada de Inteligência Artificial (RAG + Embeddings + Open Router)

> **Versão:** 2.0  
> **Última atualização:** 27/06/2026  
> **Stack:** Java 21 + Spring Boot 3.5 + PostgreSQL + pgvector + Spring AI + Open Router + MapStruct + Spring Security + JWT

---

## Índice

1. [Stack Tecnológica](#1-stack-tecnológica)
2. [Arquitetura — Clean Architecture com Pipelines de IA](#2-arquitetura--clean-architecture-com-pipelines-de-ia)
3. [Responsabilidades e Regras de Isolamento](#3-responsabilidades-e-regras-de-isolamento)
4. [Modelo de Dados — pgvector](#4-modelo-de-dados--pgvector)
5. [Contratos da API REST](#5-contratos-da-api-rest)
6. [Estrutura de Pacotes Final](#6-estrutura-de-pacotes-final)
7. [Flyway Migrations — pgvector](#7-flyway-migrations--pgvector)
8. [Pipeline de Ingestão de Documentos](#8-pipeline-de-ingestão-de-documentos)
9. [Pipeline de Retrieval-Augmented Generation (RAG)](#9-pipeline-de-retrieval-augmented-generation-rag)
10. [Integração com Open Router via Spring AI](#10-integração-com-open-router-via-spring-ai)
11. [Segurança — Spring Security Stateless + JWT](#11-segurança--spring-security-stateless--jwt)
12. [Tratamento de Exceções](#12-tratamento-de-exceções)
13. [Estratégia de Testes](#13-estratégia-de-testes)
14. [Ordem de Implementação](#14-ordem-de-implementação)
15. [Regras Arquiteturais Inegociáveis](#15-regras-arquiteturais-inegociáveis)

---

## 1. Stack Tecnológica

| Camada              | Tecnologia                                           |
| ------------------- | ---------------------------------------------------- |
| Linguagem           | Java 21                                              |
| Framework           | Spring Boot 3.5.16                                   |
| Build               | Maven                                                |
| Web                 | Spring Web (MVC)                                     |
| Persistência        | Spring Data JPA                                      |
| Banco Relacional    | PostgreSQL 16+                                       |
| Banco Vetorial      | pgvector (extensão PostgreSQL)                       |
| Migrations          | Flyway                                               |
| Embedding + Chat    | Spring AI (Open Router via compatibilidade OpenAI)   |
| Modelo Embedding    | `openai/text-embedding-3-small` (1536 dimensões)     |
| Modelo Chat         | `meta-llama/llama-3.1-70b-instruct`                  |
| Validação           | Spring Validation (Jakarta Bean Validation)          |
| Mapeamento          | MapStruct                                            |
| Documentação        | SpringDoc OpenAPI (Swagger UI)                       |
| Segurança           | Spring Security + JWT (jjwt)                         |
| Testes              | JUnit 5 + Mockito + Testcontainers                   |
| Utilidades          | Lombok                                               |
| Parse de PDF        | Apache PDFBox                                        |
| Parse de DOCX       | Apache POI                                           |

### Dependências Maven (pom.xml)

```xml
<!-- Parent -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.16</version>
</parent>

<!-- Dependências principais já existentes (Web, JPA, Validation, etc.) -->

<!-- ========== NOVAS DEPENDÊNCIAS ========== -->

<!-- PostgreSQL + pgvector -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Spring AI — BOM -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-bom</artifactId>
    <version>1.0.0-M6</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>

<!-- Spring AI — OpenAI (compatível com Open Router) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>

<!-- MapStruct -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.6.3</version>
</dependency>
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.6.3</version>
    <scope>provided</scope>
</dependency>

<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT (jjwt) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- Apache PDFBox -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.4</version>
</dependency>

<!-- Apache POI (DOCX) -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.4.0</version>
</dependency>

<!-- Testcontainers (pgvector) -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 2. Arquitetura — Clean Architecture com Pipelines de IA

### 2.1 Visão Geral

```
┌──────────────────────────────────────────────────────────────────┐
│                       Controller Layer                           │
│     Apenas recebe HTTP, delega para Service, retorna DTO         │
├──────────────────────────────────────────────────────────────────┤
│                       Service Layer                               │
│     Orquestração de pipelines (RAG, Ingestão, Chat)               │
│     Serviços puros: RagService, EmbeddingService, TextChunker     │
├──────────────────────────────────────────────────────────────────┤
│                      Security Layer                               │
│     JwtAuthenticationFilter, TokenProvider, CORS config           │
├──────────────────────────────────────────────────────────────────┤
│                      Repository Layer                             │
│     JPA + pgvector (similaridade vetorial via <=>)                │
├──────────────────────────────────────────────────────────────────┤
│                      Entity Layer                                 │
│     Mapeamento JPA — NUNCA exposto para fora                     │
├──────────────────────────────────────────────────────────────────┤
│                      Integration Layer                            │
│     Open Router (via Spring AI), n8n Webhook                     │
└──────────────────────────────────────────────────────────────────┘
```

### 2.2 Fluxo Pipeline de Ingestão

```
Upload (MultipartFile)
    ↓
Controller → DocumentController.upload()
    ↓
FileService (validação de tipo/tamanho)
    ↓
DocumentParser (interface)
    ├── PdfParser  → Apache PDFBox
    ├── DocxParser → Apache POI
    └── TxtParser  → BufferedReader nativo
    ↓ (texto limpo extraído)
TextChunker.chunk(text)
    ↓ (List<DocumentChunk> com texto bruto)
EmbeddingService.generateEmbedding(chunkText)
    ↓ (List<Float> vetor 1536 dimensões)
DocumentRepository.save(document com chunks)
    ↓
n8nWebhookNotifier.notify()
    ↓
Response 201 → AttachmentResponse
```

### 2.3 Fluxo Pipeline de RAG (Chat Inteligente)

```
POST /api/chat/send (conversationId + message)
    ↓
ChatController → ChatService.sendMessage()
    ↓
1. Salva Message(role=USER)
    ↓
2. EmbeddingService.generateEmbedding(pergunta)
    ↓ (List<Float> vetor)
3. DocumentChunkRepository.findSimilarChunks(vetor, topK, minSimilarity)
    ↓ (List<ChunkResult>)
4. RagService.buildContext(chunks)
    ↓ (String contexto)
5. ChatModel.call(systemPrompt + contexto + pergunta)
    ↓ (String resposta)
6. Salva Message(role=BOT)
    ↓
7. Retorna ChatResponse estruturado (resposta + fontes + metadados)
```

---

## 3. Responsabilidades e Regras de Isolamento

### 3.1 Controller Layer (`controller/`)

**Responsabilidade:** Parsear requisições HTTP, validar payloads, chamar services, retornar DTOs.

**Regras:**
- Nenhuma lógica de negócio
- Nenhum acesso a repositórios
- Nenhuma dependência de entidades JPA
- Anotações: `@RestController`, `@RequestMapping`, `@Valid`

**Controllers (novos + alterados):**

| Controller               | Endpoints                                                     | Status     |
| ------------------------ | ------------------------------------------------------------- | ---------- |
| `HealthController`       | `GET /health`                                                 | Existente  |
| `ConversationController` | `POST/GET /api/conversations`, `GET/DELETE /api/conversations/{id}` | Existente  |
| `ChatController`         | `POST /api/chat/send`, `GET /api/chat/history/{conversationId}` | Alterado   |
| `FileController`         | `POST /api/files/upload`                                      | Existente  |
| `DocumentController`     | `POST /api/documents/upload` (upload+parse+chunk+embed)       | **Novo**   |
| `AuthController`         | `POST /api/auth/login`                                        | **Novo**   |
| `UserController`         | `POST /api/users`, `GET /api/users/me`                        | **Novo**   |

### 3.2 Service Layer — Chat (`service/chat/`)

**Responsabilidade:** Gestão de conversação, persistência de histórico, orquestração de mensagens.

**Interfaces + Implementações:**

| Interface           | Implementação         | Responsabilidade                                |
| ------------------- | --------------------- | ----------------------------------------------- |
| `ChatService`       | `ChatServiceImpl`     | Envio de mensagens, histórico                   |
| `BotService`        | `BotServiceImpl`      | Geração de resposta via LLM real (RAG)          |

#### BotServiceImpl (substitui MockBotServiceImpl)

```java
@Service
@RequiredArgsConstructor
public class BotServiceImpl implements BotService {
    private final ChatModel chatModel;
    private final RagService ragService;

    @Value("${app.rag.top-k}")
    private int topK;

    @Value("${app.rag.min-similarity}")
    private double minSimilarity;

    @Value("${app.rag.max-context-size}")
    private int maxContextSize;

    @Value("${app.openrouter.chat-model}")
    private String chatModelId;

    @Override
    public String responseGenerate(String userMessage) {
        var ragContext = ragService.retrieveContext(userMessage, topK, minSimilarity, maxContextSize);
        var prompt = buildPrompt(ragContext.context(), userMessage);
        var response = chatModel.call(
            new Prompt(prompt,
                OpenAiChatOptions.builder()
                    .withModel(chatModelId)
                    .build()
            )
        );
        return response.getResult().getOutput().getContent();
    }

    private String buildPrompt(String context, String question) {
        return """
            Você é um assistente especializado. Use APENAS o contexto abaixo para responder.
            Se o contexto não contiver informação suficiente, diga que não sabe.

            Contexto:
            %s

            Pergunta:
            %s
            """.formatted(context, question);
    }
}
```

### 3.3 Service Layer — RAG (`service/rag/`)

**Responsabilidade:** Orquestrador do fluxo Retrieval-Augmented Generation.

| Classe         | Responsabilidade                                               |
| -------------- | -------------------------------------------------------------- |
| `RagService`   | Recebe pergunta, obtém embedding, consulta pgvector, monta contexto, retorna resultado estruturado |

### 3.4 Service Layer — Embedding (`service/embedding/`)

**Responsabilidade:** Interface e implementação de vetorização via Spring AI EmbeddingModel.

| Interface           | Implementação             | Responsabilidade                          |
| ------------------- | ------------------------- | ----------------------------------------- |
| `EmbeddingService`  | `EmbeddingServiceImpl`    | Gera vetor (List<Float>) a partir de texto |

```java
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {
    private final EmbeddingModel embeddingModel;

    @Value("${app.openrouter.embedding-model}")
    private String embeddingModelId;

    @Override
    public List<Float> generateEmbedding(String text) {
        var request = EmbeddingRequest.builder()
            .withModel(embeddingModelId)
            .withInstructions(List.of(text))
            .build();
        return embeddingModel.embed(request).getResult().getOutput();
    }
}
```

### 3.5 Service Layer — Chunking (`service/chunking/`)

**Responsabilidade:** Algoritmo de fragmentação de texto puro. Sem dependências de banco ou repositório.

| Classe          | Responsabilidade                                               |
| --------------- | -------------------------------------------------------------- |
| `TextChunker`   | Fatiar texto em `chunkSize`, `overlap` e delimitadores configurados |

```java
@Component
public class TextChunker {
    @Value("${app.chunking.chunk-size}")
    private int chunkSize;          // default 1000

    @Value("${app.chunking.overlap}")
    private int overlap;            // default 200

    @Value("${app.chunking.delimiters}")
    private String delimiters;      // default "\n\n|\n|\\.|\\?"

    public List<String> chunk(String text) {
        // 1. Limpeza de espaços em branco excessivos
        // 2. Divisão por delimitadores (regex)
        // 3. Recomposição respeitando chunkSize e overlap
        // 4. Retorno de List<String>
    }
}
```

### 3.6 Service Layer — Parser (`service/parser/`)

**Responsabilidade:** Mecanismos de extração de texto de arquivos.

| Interface         | Implementações          | Formato   |
| ----------------- | ----------------------- | --------- |
| `DocumentParser`  | `PdfParser`             | PDF       |
| `DocumentParser`  | `DocxParser`            | DOCX      |
| `DocumentParser`  | `TxtParser`             | TXT       |

```java
public interface DocumentParser {
    String parse(InputStream inputStream);
}
```

### 3.7 Service Layer — Integração (`service/integration/`)

**Responsabilidade:** Integrações externas.

| Classe                  | Responsabilidade                                   |
| ----------------------- | -------------------------------------------------- |
| `N8nWebhookNotifier`    | Notificação assíncrona ao n8n após ingestão de doc |

### 3.8 Repository Layer (`repository/`)

**Responsabilidade:** Acesso a dados via Spring Data JPA + pgvector.

**Regras:**
- Apenas métodos de consulta/persistência
- Nenhuma regra de negócio
- Extendem `JpaRepository<T, Long>`
- A busca vetorial usa `@Query` nativa com operador `<=>` do pgvector

**Repositories:**

| Interface                    | Entidade          | Responsabilidade                             |
| ---------------------------- | ----------------- | -------------------------------------------- |
| `ConversationRepository`     | `Conversation`    | CRUD de conversas (existente)                |
| `MessageRepository`          | `Message`         | Histórico de mensagens (existente)           |
| `AttachmentRepository`       | `Attachment`      | Metadados de upload (existente)              |
| `DocumentRepository`         | `Document`        | CRUD de documentos                           |
| `DocumentChunkRepository`    | `DocumentChunk`   | Persistência + busca vetorial via pgvector   |
| `UserRepository`             | `User`            | Autenticação e cadastro                      |

#### DocumentChunkRepository — busca por similaridade vetorial

```java
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    @Query(value = """
            SELECT id, document_id, content, chunk_index, created_at,
                   1 - (embedding <=> :queryVector) AS similarity
            FROM document_chunks
            WHERE 1 - (embedding <=> :queryVector) >= :minSimilarity
            ORDER BY embedding <=> :queryVector
            LIMIT :topK
            """, nativeQuery = true)
    List<ChunkSimilarityProjection> findSimilarChunks(
            @Param("queryVector") PGvector queryVector,
            @Param("topK") int topK,
            @Param("minSimilarity") double minSimilarity);
}
```

> **Nota arquitetural:** O operador `<=>` calcula a distância de cosseno. O score de similaridade é `1 - distance`, permitindo corte semântico por `minSimilarity`.

### 3.9 Entity Layer (`entity/`)

**Responsabilidade:** Mapeamento objeto-relacional (JPA + pgvector).

#### Document

```java
@Entity
@Table(name = "documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Document {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "original_text", columnDefinition = "TEXT", nullable = false)
    private String originalText;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentChunk> chunks = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.uploadedAt = LocalDateTime.now();
    }
}
```

#### DocumentChunk

```java
@Entity
@Table(name = "document_chunks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentChunk {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "embedding", columnDefinition = "vector(1536)", nullable = false)
    private PGvector embedding;     // 1536 dimensões (text-embedding-3-small)

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
```

#### User (nova entidade para autenticação)

```java
@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;       // BCrypt hash

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
}
```

### 3.10 Mapper Layer (`mapper/`)

**Responsabilidade:** Conversão Entity ↔ DTO via MapStruct.

**Regras:**
- Interfaces anotadas com `@Mapper(componentModel = "spring")`
- Mapeamento explícito de campos

**Mappers:**

| Interface               | Origem → Destino                       |
| ----------------------- | -------------------------------------- |
| `ConversationMapper`    | `Conversation` ↔ `ConversationResponse` |
| `MessageMapper`         | `Message` ↔ `MessageResponse`          |
| `AttachmentMapper`      | `Attachment` ↔ `AttachmentResponse`     |
| `DocumentMapper`        | `Document` ↔ `DocumentResponse`         |
| `UserMapper`            | `User` ↔ `UserResponse`                |

### 3.11 DTO Layer (`dto/request/`, `dto/response/`)

**Responsabilidade:** Contratos de entrada/saída da API.

**Regras:**
- Record classes imutáveis (Java 14+)
- `request/` para entrada com validação (`@NotBlank`, `@NotNull`)
- `response/` para saída

#### DTOs Request (novos)

```java
public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {}

public record UserCreateRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6) String password,
    @NotBlank @Size(max = 100) String displayName
) {}

public record DocumentUploadRequest(
    @NotNull Long conversationId,
    @NotNull MultipartFile file
) {}   // Nota: MultipartFile não precisa de @NotNull no record — é @RequestParam
```

#### DTOs Response (novos + alterados)

```java
public record LoginResponse(
    String token,
    String type   // "Bearer"
) {}

public record UserResponse(
    Long id,
    String email,
    String displayName,
    LocalDateTime createdAt
) {}

public record DocumentResponse(
    Long id,
    String fileName,
    String contentType,
    Long fileSize,
    int chunkCount,
    LocalDateTime uploadedAt
) {}

public record ChatResponseV2(
    MessageResponse userMessage,
    MessageResponse botMessage,
    String answer,
    List<SourceReference> sources,
    long executionTimeMs,
    int chunksConsumed
) {}

public record SourceReference(
    Long documentId,
    String fileName,
    String excerpt
) {}
```

### 3.12 Security Layer (`security/`)

**Responsabilidade:** Autenticação e autorização stateless via JWT.

| Classe                     | Responsabilidade                                  |
| -------------------------- | ------------------------------------------------- |
| `SecurityConfig`           | Configuração Spring Security (stateless, CORS)    |
| `JwtAuthenticationFilter`  | Filtro OncePerRequest — valida token Bearer       |
| `TokenProvider`            | Geração e validação de tokens JWT                 |
| `UserDetailsServiceImpl`   | Carrega User pelo email para autenticação         |

#### SecurityConfig

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health", "/api/auth/**", "/api/users").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/h2-console/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

### 3.13 Exception Handler (`exception/`)

**Responsabilidade:** Tratamento global de exceções — mesmo formato do sistema existente.

**Novas exceções customizadas:**

| Exceção                       | HTTP Status | Causa                                    |
| ----------------------------- | ----------- | ---------------------------------------- |
| `DocumentProcessingException` | 422         | Falha no parse/embedding de documento    |
| `AuthenticationException`     | 401         | Credenciais inválidas                    |
| `DuplicateResourceException`  | 409         | Email duplicado, chunk duplicado         |

### 3.14 Config Layer (`config/`)

**Responsabilidade:** Beans de configuração da nova camada de IA.

| Classe                        | Responsabilidade                                           |
| ----------------------------- | ---------------------------------------------------------- |
| `OpenAiConfig`                | Beans ChatModel e EmbeddingModel apontando para Open Router |
| `SecurityConfig`              | Spring Security, CORS, JWT                                 |
| `SwaggerConfig`               | OpenAPI (existente)                                        |

#### OpenAiConfig — Beans do Spring AI com Open Router

```java
@Configuration
public class OpenAiConfig {

    @Bean
    public OpenAiChatModel openAiChatModel(
            @Value("${app.openrouter.base-url}") String baseUrl,
            @Value("${app.openrouter.api-key}") String apiKey,
            @Value("${app.openrouter.chat-model}") String model) {
        var metadata = OpenAiChatOptions.builder()
                .withModel(model)
                .build();
        return OpenAiChatModel.builder()
                .openAiChatOptions(metadata)
                .baseUrl(baseUrl + "/chat/completions")
                .apiKey(apiKey)
                .build();
    }

    @Bean
    public OpenAiEmbeddingModel openAiEmbeddingModel(
            @Value("${app.openrouter.base-url}") String baseUrl,
            @Value("${app.openrouter.api-key}") String apiKey,
            @Value("${app.openrouter.embedding-model}") String model) {
        var metadata = OpenAiEmbeddingOptions.builder()
                .withModel(model)
                .build();
        return OpenAiEmbeddingModel.builder()
                .defaultOptions(metadata)
                .baseUrl(baseUrl + "/embeddings")
                .apiKey(apiKey)
                .build();
    }
}
```

---

## 4. Modelo de Dados — pgvector

### 4.1 Diagrama Entidade-Relacionamento Estendido

```
┌───────────────────────┐       ┌───────────────────────┐
│     conversations     │       │       messages        │
├───────────────────────┤       ├───────────────────────┤
│ id (BIGINT, PK)       │──┐    │ id (BIGINT, PK)       │
│ title (VARCHAR(255))  │  │    │ conversation_id (FK)  │── FK → conversations.id
│ created_at (DATETIME) │  │    │ role (VARCHAR(10))    │
│ updated_at (DATETIME) │  │    │ content (TEXT)        │
└───────────────────────┘  │    │ created_at (DATETIME) │
                           │    └───────────────────────┘
                           │
                           │    ┌───────────────────────┐
                           │    │      attachments       │
                           │    ├───────────────────────┤
                           └────│ id (BIGINT, PK)       │
                                │ conversation_id (FK)  │── FK → conversations.id
                                │ file_name (VARCHAR)   │
                                │ content_type (VARCHAR)│
                                │ size (BIGINT)         │
                                │ upload_date (DATETIME)│
                                └───────────────────────┘

┌───────────────────────────────────┐
│            documents              │
├───────────────────────────────────┤
│ id (BIGINT, PK)                   │
│ file_name (VARCHAR(255))          │
│ content_type (VARCHAR(100))       │
│ file_size (BIGINT)                │
│ original_text (TEXT)              │
│ uploaded_at (TIMESTAMP)           │
└───────────────────────────────────┘
        │ 1
        │
       ╱╲
       ──
        │ N
┌─────────────────────────────────────────┐
│            document_chunks              │
├─────────────────────────────────────────┤
│ id (UUID, PK)                           │
│ document_id (BIGINT, FK)                │── FK → documents.id
│ content (TEXT)                          │
│ embedding (vector(1536))                │ ← pgvector
│ chunk_index (INT)                       │
│ created_at (TIMESTAMP)                  │
└─────────────────────────────────────────┘

┌───────────────────────┐
│        users          │
├───────────────────────┤
│ id (BIGINT, PK)       │
│ email (VARCHAR(100))  │ (UNIQUE)
│ password (VARCHAR)    │ (BCrypt hash)
│ display_name (VARCHAR)│
│ created_at (TIMESTAMP)│
│ updated_at (TIMESTAMP)│
└───────────────────────┘
```

### 4.2 Relacionamentos JPA

| Entidade           | Relacionamento                          | Entidade alvo     |
| ------------------ | --------------------------------------- | ----------------- |
| `Conversation`     | `@OneToMany(mappedBy = "conversation")` | `Message`         |
| `Conversation`     | `@OneToMany(mappedBy = "conversation")` | `Attachment`      |
| `Message`          | `@ManyToOne(fetch = LAZY)`              | `Conversation`    |
| `Attachment`       | `@ManyToOne(fetch = LAZY)`              | `Conversation`    |
| `Document`         | `@OneToMany(mappedBy = "document")`     | `DocumentChunk`   |
| `DocumentChunk`    | `@ManyToOne(fetch = LAZY)`              | `Document`        |

---

## 5. Contratos da API REST

### 5.1 Autenticação

#### Login

```
POST /api/auth/login
Content-Type: application/json

Request:
{
    "email": "usuario@email.com",
    "password": "senha123"
}

Response 200:
{
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "type": "Bearer"
}

Response 401:
{
    "status": 401,
    "error": "Unauthorized",
    "message": "Credenciais inválidas",
    "path": "/api/auth/login",
    "timestamp": "2026-06-27T10:30:00.123"
}
```

#### Criar usuário

```
POST /api/users
Content-Type: application/json

Request:
{
    "email": "usuario@email.com",
    "password": "senha123",
    "displayName": "Usuário Teste"
}

Response 201:
{
    "id": 1,
    "email": "usuario@email.com",
    "displayName": "Usuário Teste",
    "createdAt": "2026-06-27T10:30:00.123"
}

Response 409 (email duplicado):
{
    "status": 409,
    "error": "Conflict",
    "message": "Email já cadastrado: usuario@email.com",
    "path": "/api/users",
    "timestamp": "2026-06-27T10:30:00.123"
}
```

### 5.2 Upload de Documentos com Pipeline de IA

```
POST /api/documents/upload
Content-Type: multipart/form-data
Authorization: Bearer <token>

Request:
- file: (arquivo .txt, .pdf ou .docx)
- conversationId: (long)

Pipeline interno:
1. Validar tipo de arquivo (.txt, .pdf, .docx)
2. Selecionar DocumentParser adequado (PdfParser/DocxParser/TxtParser)
3. Extrair texto limpo via parser
4. Fragmentar texto via TextChunker (chunkSize + overlap)
5. Gerar embedding para cada chunk via EmbeddingService
6. Persistir Document + List<DocumentChunk> com vetores
7. Notificar n8n via N8nWebhookNotifier (assíncrono)
8. Retornar DocumentResponse

Response 201:
{
    "id": 1,
    "fileName": "relatorio.pdf",
    "contentType": "application/pdf",
    "fileSize": 2048000,
    "chunkCount": 15,
    "uploadedAt": "2026-06-27T10:30:00.123"
}

Response 400 (tipo inválido):
{
    "status": 400,
    "error": "Bad Request",
    "message": "Tipo de arquivo não suportado: image/png. Tipos aceitos: .txt, .pdf, .docx",
    "path": "/api/documents/upload",
    "timestamp": "2026-06-27T10:30:00.123"
}

Response 422 (falha no processamento):
{
    "status": 422,
    "error": "Unprocessable Entity",
    "message": "Falha ao processar documento: PDF corrompido ou protegido por senha",
    "path": "/api/documents/upload",
    "timestamp": "2026-06-27T10:30:00.123"
}
```

### 5.3 Chat Inteligente (RAG) — endpoint alterado

```
POST /api/chat/send
Content-Type: application/json
Authorization: Bearer <token>

Request:
{
    "conversationId": 1,
    "message": "Qual o teor do documento sobre políticas de segurança?"
}

Pipeline interno:
1. Salvar Message(role=USER) no histórico
2. Gerar embedding da pergunta via EmbeddingService
3. Buscar top K chunks similares no pgvector (DocumentChunkRepository)
4. Filtrar por minSimilarity e montar contexto (RagService)
5. Construir System Prompt + Contexto + Pergunta
6. Enviar ao ChatModel (Open Router / Meta Llama)
7. Salvar Message(role=BOT) com a resposta
8. Retornar ChatResponseV2 estruturado

Response 200:
{
    "userMessage": {
        "id": 1,
        "role": "USER",
        "content": "Qual o teor do documento sobre políticas de segurança?",
        "createdAt": "2026-06-27T10:30:00.123"
    },
    "botMessage": {
        "id": 2,
        "role": "BOT",
        "content": "Conforme o documento 'Políticas de Segurança 2026.pdf', as diretrizes principais são...",
        "createdAt": "2026-06-27T10:30:05.456"
    },
    "answer": "Conforme o documento 'Políticas de Segurança 2026.pdf', as diretrizes principais são...",
    "sources": [
        {
            "documentId": 1,
            "fileName": "Politicas_Seguranca_2026.pdf",
            "excerpt": "As políticas de segurança estabelecem que todos os colaboradores devem..."
        }
    ],
    "executionTimeMs": 4234,
    "chunksConsumed": 3
}
```

### 5.4 DTOs — Definições Java (Records)

```java
// ================ NOVOS REQUEST ================

public record LoginRequest(
    @NotBlank @Email(message = "Email deve ser válido")
    String email,

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    String password
) {}

public record UserCreateRequest(
    @NotBlank @Email(message = "Email deve ser válido")
    String email,

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    String password,

    @NotBlank(message = "Nome de exibição é obrigatório")
    String displayName
) {}

// ================ NOVOS RESPONSE ================

public record LoginResponse(
    String token,
    String type
) {}

public record UserResponse(
    Long id,
    String email,
    String displayName,
    LocalDateTime createdAt
) {}

public record DocumentResponse(
    Long id,
    String fileName,
    String contentType,
    Long fileSize,
    Integer chunkCount,
    LocalDateTime uploadedAt
) {}

public record ChatResponseV2(
    MessageResponse userMessage,
    MessageResponse botMessage,
    String answer,
    List<SourceReference> sources,
    Long executionTimeMs,
    Integer chunksConsumed
) {}

public record SourceReference(
    Long documentId,
    String fileName,
    String excerpt
) {}
```

---

## 6. Estrutura de Pacotes Final

```
src/main/java/br/com/chatbot/chatbot_api/
├── ChatbotApiApplication.java
│
├── config/
│   ├── OpenAiConfig.java              ← NOVO (ChatModel + EmbeddingModel beans)
│   ├── SecurityConfig.java            ← NOVO (Spring Security + CORS)
│   └── SwaggerConfig.java             ← Existente
│
├── controller/
│   ├── AuthController.java            ← NOVO
│   ├── ChatController.java            ← Alterado (retorna ChatResponseV2)
│   ├── ConversationController.java    ← Existente
│   ├── DocumentController.java        ← NOVO
│   ├── FileController.java            ← Existente
│   ├── HealthController.java          ← Existente
│   └── UserController.java            ← NOVO
│
├── dto/
│   ├── request/
│   │   ├── ChatRequest.java           ← Existente
│   │   ├── ConversationRequest.java   ← Existente
│   │   ├── LoginRequest.java          ← NOVO
│   │   └── UserCreateRequest.java     ← NOVO
│   └── response/
│       ├── AttachmentResponse.java    ← Existente
│       ├── ChatResponse.java          ← Existente (depreciado → ChatResponseV2)
│       ├── ChatResponseV2.java        ← NOVO
│       ├── ConversationResponse.java  ← Existente
│       ├── DocumentResponse.java      ← NOVO
│       ├── ErrorResponse.java         ← Existente
│       ├── HealthResponse.java        ← Existente
│       ├── LoginResponse.java         ← NOVO
│       ├── MessageResponse.java       ← Existente
│       ├── SourceReference.java       ← NOVO
│       └── UserResponse.java          ← NOVO
│
├── entity/
│   ├── Attachment.java                ← Existente
│   ├── Conversation.java              ← Existente
│   ├── Document.java                  ← NOVO
│   ├── DocumentChunk.java             ← NOVO
│   ├── Message.java                   ← Existente
│   └── User.java                      ← NOVO
│
├── enums/
│   └── MessageRole.java               ← Existente
│
├── exception/
│   ├── AuthenticationException.java   ← NOVO
│   ├── DocumentProcessingException.java ← NOVO
│   ├── DuplicateResourceException.java  ← NOVO
│   ├── GlobalExceptionHandler.java    ← Alterado (novos handlers)
│   ├── InvalidFileTypeException.java  ← Existente
│   └── ResourceNotFoundException.java ← Existente
│
├── mapper/
│   ├── AttachmentMapper.java          ← NOVO (MapStruct)
│   ├── ConversationMapper.java        ← NOVO (MapStruct)
│   ├── DocumentMapper.java            ← NOVO (MapStruct)
│   ├── MessageMapper.java             ← NOVO (MapStruct)
│   └── UserMapper.java                ← NOVO (MapStruct)
│
├── repository/
│   ├── AttachmentRepository.java      ← Existente
│   ├── ConversationRepository.java    ← Existente
│   ├── DocumentChunkRepository.java   ← NOVO (busca vetorial nativa)
│   ├── DocumentRepository.java        ← NOVO
│   ├── MessageRepository.java         ← Existente
│   └── UserRepository.java            ← NOVO
│
├── security/
│   ├── JwtAuthenticationFilter.java   ← NOVO
│   ├── SecurityConfig.java            ← NOVO (config Spring Security)
│   ├── TokenProvider.java             ← NOVO (geração/validação JWT)
│   └── UserDetailsServiceImpl.java    ← NOVO (loadUserByUsername)
│
└── service/
    ├── BotService.java                ← Existente (interface)
    ├── ChatService.java               ← Existente (interface)
    ├── ConversationService.java       ← Existente (interface)
    ├── FileService.java               ← Existente (interface)
    │
    ├── chat/
    │   ├── BotServiceImpl.java        ← NOVO (substitui MockBotServiceImpl)
    │   └── ChatServiceImpl.java       ← Alterado (injeta BotServiceImpl real)
    │
    ├── chunking/
    │   └── TextChunker.java           ← NOVO
    │
    ├── embedding/
    │   ├── EmbeddingService.java      ← NOVO (interface)
    │   └── EmbeddingServiceImpl.java  ← NOVO (implementação)
    │
    ├── impl/
    │   ├── ChatServiceImpl.java       ← MOVIDO para chat/
    │   ├── ConversationServiceImpl.java ← Existente
    │   ├── FileServiceImpl.java       ← Existente
    │   └── MockBotServiceImpl.java    ← REMOVIDO
    │
    ├── integration/
    │   └── N8nWebhookNotifier.java    ← NOVO
    │
    ├── parser/
    │   ├── DocumentParser.java        ← NOVO (interface)
    │   ├── PdfParser.java             ← NOVO
    │   ├── DocxParser.java            ← NOVO
    │   └── TxtParser.java             ← NOVO
    │
    └── rag/
        └── RagService.java           ← NOVO
```

```
src/main/resources/
├── application.yml                   ← Alterado (propriedades OpenRouter, pgvector, chunking)
├── db/migration/
│   ├── V1__create_conversations.sql  ← Existente
│   ├── V2__create_messages.sql       ← Existente
│   ├── V3__create_attachments.sql    ← Existente
│   ├── V4__create_users.sql          ← NOVO
│   ├── V5__create_documents.sql      ← NOVO
│   └── V6__create_document_chunks.sql ← NOVO (com vector(1536))
├── static/
└── templates/
```

---

## 7. Flyway Migrations — pgvector

### V4__create_users.sql

```sql
CREATE TABLE users (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### V5__create_documents.sql

```sql
CREATE TABLE documents (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    original_text TEXT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### V6__create_document_chunks.sql

```sql
-- Habilitar extensão pgvector (executar como superuser)
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE document_chunks (
    id UUID PRIMARY KEY,
    document_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536) NOT NULL,
    chunk_index INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chunks_document FOREIGN KEY (document_id)
        REFERENCES documents(id) ON DELETE CASCADE
);

CREATE INDEX idx_chunks_document_id ON document_chunks(document_id);

-- Índice IVF para busca aproximada de similaridade (performance em grandes volumes)
CREATE INDEX idx_chunks_embedding_ivf
    ON document_chunks
    USING ivf (embedding vector_cosine_ops)
    WITH (lists = 100);
```

> **Nota arquitetural sobre índices:** O IVF (Inverted File Index) acelera a busca por similaridade em datasets > 100k linhas. Para datasets menores, um índice bruto (exato) via `vector_cosine_ops` sem IVF é suficiente. O parâmetro `lists` deve ser `sqrt(n_rows)` aproximado.

---

## 8. Pipeline de Ingestão de Documentos

### 8.1 Fluxo Completo

```
Upload via DocumentController
    ↓
1. Validação do arquivo (extensão + content-type + tamanho)
    ↓
2. Seleção do DocumentParser:
    ├── .pdf  → PdfParser  (Apache PDFBox)
    ├── .docx → DocxParser (Apache POI)
    └── .txt  → TxtParser  (BufferedReader)
    ↓
3. Extração de texto limpo → String
    ↓
4. TextChunker.chunk(text) → List<String>
    (parâmetros: chunkSize, overlap, delimiters)
    ↓
5. Para cada chunk:
    ├── EmbeddingService.generateEmbedding(chunkText) → List<Float>
    └── Monta DocumentChunk (UUID, texto, vetor, índice)
    ↓
6. DocumentRepository.save(document com chunks)
    ↓
7. N8nWebhookNotifier.notify(documentId) — assíncrono (@Async)
    ↓
Response 201 → DocumentResponse
```

### 8.2 Validação de Arquivos (FileService)

| Validação           | Regra                                       |
| ------------------- | ------------------------------------------- |
| Extensão permitida  | `.txt`, `.pdf`, `.docx`                     |
| Content-Type        | `text/plain`, `application/pdf`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document` |
| Tamanho máximo      | 50 MB (configurável via `spring.servlet.multipart.max-file-size`) |

### 8.3 DocumentParser — Interface

```java
public interface DocumentParser {
    String parse(InputStream inputStream);
}
```

### 8.4 Estratégia de Chunking (TextChunker)

| Parâmetro    | Default | Descrição                                    |
| ------------ | ------- | -------------------------------------------- |
| chunkSize    | 1000    | Número máximo de caracteres por chunk        |
| overlap      | 200     | Sobreposição entre chunks consecutivos       |
| delimiters   | `\n\n`  | Prioridade de quebra: parágrafo > linha > frase |

**Algoritmo:**

1. Limpar texto (espaços duplicados, tabs, caracteres de controle)
2. Dividir por delimitadores em ordem de prioridade
3. Recompor segmentos respeitando `chunkSize`
4. Aplicar `overlap` entre chunks adjacentes (preservar contexto semântico)
5. Retornar `List<String>` — texto puro, sem metadados

### 8.5 EmbeddingService — Interface

```java
public interface EmbeddingService {
    List<Float> generateEmbedding(String text);
}
```

**Implementação:** `EmbeddingServiceImpl` utiliza `EmbeddingModel` do Spring AI (OpenAI) apontando para Open Router com o modelo `openai/text-embedding-3-small` (1536 dimensões).

### 8.6 Notificação Assíncrona (N8nWebhookNotifier)

```java
@Service
public class N8nWebhookNotifier {

    @Value("${app.n8n.webhook-url}")
    private String webhookUrl;

    @Async
    public void notify(Long documentId) {
        // POST para n8n com documentId
        // Tratamento de falha silenciosa (log de erro)
    }
}
```

---

## 9. Pipeline de Retrieval-Augmented Generation (RAG)

### 9.1 Arquitetura do RAG

```
                    ┌─────────────────┐
                    │  User Question  │
                    └────────┬────────┘
                             ↓
                    ┌─────────────────┐
                    │  EmbeddingService│
                    │  (question→vec) │
                    └────────┬────────┘
                             ↓ (1536d vector)
                    ┌──────────────────────────────┐
                    │ DocumentChunkRepository       │
                    │ findSimilarChunks(vec, topK,  │
                    │   minSimilarity)              │
                    └────────┬─────────────────────┘
                             ↓ (List<ChunkResult>)
                    ┌─────────────────┐
                    │   RagService    │
                    │  - Filtra por   │
                    │    minSimilarity│
                    │  - Monta prompt │
                    │    context      │
                    └────────┬────────┘
                             ↓ (String context)
                    ┌─────────────────┐
                    │   ChatModel     │
                    │  (Meta Llama)   │
                    └────────┬────────┘
                             ↓ (String answer)
                    ┌─────────────────┐
                    │  ChatService    │
                    │  - Salva BOT msg│
                    │  - Monta DTO    │
                    └────────┬────────┘
                             ↓
                    ┌──────────────────────────┐
                    │ ChatResponseV2 (JSON)    │
                    │ answer, sources, metrics │
                    └──────────────────────────┘
```

### 9.2 RagService — Orquestrador

```java
@Service
@RequiredArgsConstructor
public class RagService {

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository chunkRepository;

    public RagResult retrieveContext(String question, int topK, double minSimilarity, int maxContextSize) {
        var startTime = System.currentTimeMillis();

        // 1. Vetorizar pergunta
        var queryVector = embeddingService.generateEmbedding(question);

        // 2. Buscar chunks similares no pgvector
        var similarChunks = chunkRepository.findSimilarChunks(
            new PGvector(queryVector.stream().mapToDouble(Float::doubleValue).toArray()),
            topK,
            minSimilarity
        );

        // 3. Montar contexto truncado
        var contextBuilder = new StringBuilder();
        var sources = new ArrayList<SourceReference>();
        for (var chunk : similarChunks) {
            if (contextBuilder.length() + chunk.getContent().length() > maxContextSize) break;
            contextBuilder.append(chunk.getContent()).append("\n\n");
            sources.add(new SourceReference(
                chunk.getDocumentId(),
                chunk.getDocumentName(),
                chunk.getContent().substring(0, Math.min(200, chunk.getContent().length()))
            ));
        }

        var elapsed = System.currentTimeMillis() - startTime;

        return new RagResult(
            contextBuilder.toString(),
            sources,
            elapsed,
            sources.size()
        );
    }
}
```

### 9.3 Parâmetros Dinâmicos (application.yml)

```yaml
app:
  rag:
    top-k: 5                    # Limite de chunks recuperados
    min-similarity: 0.75        # Corte semântico (score 1 - distância cosseno)
    max-context-size: 4000      # Caracteres máximos do contexto
  chunking:
    chunk-size: 1000
    overlap: 200
    delimiters: "\n\n|\n|\\.|\\?"
  openrouter:
    base-url: https://openrouter.ai/api/v1
    api-key: ${OPEN_ROUTER_API_KEY}
    chat-model: meta-llama/llama-3.1-70b-instruct
    embedding-model: openai/text-embedding-3-small
  n8n:
    webhook-url: ${N8N_WEBHOOK_URL:http://localhost:5678/webhook/chatbot-ingest}
```

---

## 10. Integração com Open Router via Spring AI

### 10.1 Arquitetura de Integração

```
Chatbot API
    ↓
Spring AI (OpenAiChatModel / OpenAiEmbeddingModel)
    ↓
Open Router API (https://openrouter.ai/api/v1)
    ├── /chat/completions → Chat (Meta Llama 3.1 70B)
    └── /embeddings       → Embedding (text-embedding-3-small)
```

### 10.2 Princípios

1. **Provedor Único:** Todo tráfego de IA passa exclusivamente pelo Open Router
2. **Compatibilidade OpenAI:** Spring AI se comunica usando o formato OpenAI — Open Router é 100% compatível
3. **Sem SDKs específicos:** Não há dependência de SDK da Meta, OpenAI ou Google — apenas `spring-ai-openai`
4. **Chave única:** Apenas `OPEN_ROUTER_API_KEY` como variável de ambiente sensível

### 10.3 OpenAiConfig — Beans

```java
@Configuration
public class OpenAiConfig {

    @Bean
    @ConditionalOnMissingBean
    public OpenAiChatModel openAiChatModel(
            @Value("${app.openrouter.base-url}") String baseUrl,
            @Value("${app.openrouter.api-key}") String apiKey,
            @Value("${app.openrouter.chat-model}") String model) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .defaultOptions(OpenAiChatOptions.builder()
                        .withModel(model)
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenAiEmbeddingModel openAiEmbeddingModel(
            @Value("${app.openrouter.base-url}") String baseUrl,
            @Value("${app.openrouter.api-key}") String apiKey,
            @Value("${app.openrouter.embedding-model}") String model) {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .defaultOptions(OpenAiEmbeddingOptions.builder()
                        .withModel(model)
                        .build())
                .build();
    }
}
```

### 10.4 Variáveis de Ambiente (.env)

```
OPEN_ROUTER_API_KEY=sk-or-v1-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
N8N_WEBHOOK_URL=http://localhost:5678/webhook/chatbot-ingest
JWT_SECRET=base64-encoded-256-bit-secret-for-hs256
```

---

## 11. Segurança — Spring Security Stateless + JWT

### 11.1 Arquitetura de Segurança

```
Request
    ↓
JwtAuthenticationFilter (OncePerRequestFilter)
    ├── Extrai token do header "Authorization: Bearer <token>"
    ├── Valida token via TokenProvider
    │   ├── Válido   → Seta SecurityContextHolder (Authentication)
    │   └── Inválido → Prossegue sem autenticação (401 será retornado)
    ↓
SecurityFilterChain
    ├── /health                  → PERMIT ALL
    ├── /api/auth/**             → PERMIT ALL
    ├── /api/users (POST)        → PERMIT ALL
    ├── /swagger-ui/**           → PERMIT ALL
    ├── /v3/api-docs/**          → PERMIT ALL
    └── /api/**                  → AUTHENTICATED
```

### 11.2 TokenProvider

```java
@Component
public class TokenProvider {
    private final SecretKey secretKey;
    private final long tokenValidityMs;

    public TokenProvider(@Value("${app.jwt.secret}") String secret,
                         @Value("${app.jwt.token-validity:86400000}") long validityMs) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.tokenValidityMs = validityMs;
    }

    public String createToken(String email) {
        var now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(tokenValidityMs)))
                .signWith(secretKey)
                .compact();
    }

    public Optional<String> getEmailFromToken(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return Optional.of(claims.getPayload().getSubject());
        } catch (JwtException e) {
            return Optional.empty();
        }
    }
}
```

### 11.3 CORS — Suporte a React Native

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    var config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of("*"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    config.setExposedHeaders(List.of("Authorization", "Content-Type"));
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

> **Nota:** `setAllowedOriginPatterns("*")` com `setAllowCredentials(true)` é a combinação correta para aceitar requisições de dispositivos móveis (React Native) que não têm uma origem HTTP tradicional.

---

## 12. Tratamento de Exceções

### 12.1 Mapa de Exceções

| Exceção                              | HTTP Status | Causa                                              |
| ------------------------------------ | ----------- | -------------------------------------------------- |
| `ResourceNotFoundException`         | 404         | Entidade não encontrada no banco                   |
| `InvalidFileTypeException`           | 400         | Tipo de arquivo não suportado                      |
| `DocumentProcessingException`        | 422         | Falha no parse ou embedding do documento           |
| `MethodArgumentNotValidException`    | 400         | Falha de validação de DTO                          |
| `HttpMessageNotReadableException`    | 400         | JSON malformado                                    |
| `MaxUploadSizeExceededException`     | 413         | Arquivo excede limite de tamanho                   |
| `AuthenticationException`            | 401         | Credenciais inválidas ou token expirado            |
| `DuplicateResourceException`         | 409         | Email duplicado, chunk duplicado                   |
| `AccessDeniedException`              | 403         | Usuário não autorizado para o recurso              |
| `Exception` (genérica)               | 500         | Erro interno não categorizado                      |

### 12.2 Formato de erro padronizado (mantido do sistema existente)

```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Falha ao processar documento: PDF corrompido",
  "path": "/api/documents/upload",
  "timestamp": "2026-06-27T10:30:00.123"
}
```

### 12.3 GlobalExceptionHandler — Novos Handlers

```java
@ExceptionHandler(DocumentProcessingException.class)
public ResponseEntity<ErrorResponse> handleDocumentProcessing(
        DocumentProcessingException ex, HttpServletRequest request) {
    var response = new ErrorResponse(
            HttpStatus.UNPROCESSABLE_ENTITY.value(), "Unprocessable Entity",
            ex.getMessage(), request.getRequestURI(), LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
}

@ExceptionHandler(DuplicateResourceException.class)
public ResponseEntity<ErrorResponse> handleDuplicate(
        DuplicateResourceException ex, HttpServletRequest request) {
    var response = new ErrorResponse(
            HttpStatus.CONFLICT.value(), "Conflict",
            ex.getMessage(), request.getRequestURI(), LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
}

@ExceptionHandler(AuthenticationException.class)
public ResponseEntity<ErrorResponse> handleAuthentication(
        AuthenticationException ex, HttpServletRequest request) {
    var response = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(), "Unauthorized",
            ex.getMessage(), request.getRequestURI(), LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
}

@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<ErrorResponse> handleAccessDenied(
        AccessDeniedException ex, HttpServletRequest request) {
    var response = new ErrorResponse(
            HttpStatus.FORBIDDEN.value(), "Forbidden",
            "Acesso negado a este recurso",
            request.getRequestURI(), LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
}
```

---

## 13. Estratégia de Testes

### 13.1 Framework e Ferramentas

| Ferramenta        | Uso                                             |
| ----------------- | ----------------------------------------------- |
| JUnit 5           | Framework de testes                             |
| Mockito           | Mock de dependências                            |
| Testcontainers    | PostgreSQL + pgvector em container Docker       |
| Spring Boot Test  | Testes de integração com `@SpringBootTest`      |

### 13.2 Escopo de Testes

#### TextChunkerTest

| Cenário                        | Teste                                               |
| ------------------------------ | --------------------------------------------------- |
| Texto menor que chunkSize      | `chunk_SmallText_ReturnsSingleChunk()`              |
| Texto com múltiplos parágrafos | `chunk_MultipleParagraphs_ReturnsMultipleChunks()`  |
| Texto com overlap              | `chunk_WithOverlap_LastChunkOverlapsPrevious()`     |
| Texto vazio                    | `chunk_EmptyText_ReturnsEmptyList()`                |

#### EmbeddingServiceImplTest

| Cenário                        | Teste                                               |
| ------------------------------ | --------------------------------------------------- |
| Geração de embedding           | `generateEmbedding_ValidText_ReturnsVector1536()`   |
| Lista de floats não nula       | `generateEmbedding_ValidText_ReturnsNonNull()`      |
| Dimensão correta               | `generateEmbedding_ValidText_DimensionIs1536()`     |

#### RagServiceTest

| Cenário                        | Teste                                               |
| ------------------------------ | --------------------------------------------------- |
| Recuperação de contexto        | `retrieveContext_ValidQuestion_ReturnsRagResult()`  |
| Filtro por minSimilarity       | `retrieveContext_LowSimilarity_FiltersChunks()`     |
| Limite de maxContextSize       | `retrieveContext_ExceedsMaxSize_Truncates()`        |

#### BotServiceImplTest (RAG real)

| Cenário                        | Teste                                               |
| ------------------------------ | --------------------------------------------------- |
| Geração de resposta com RAG    | `responseGenerate_ValidMessage_ReturnsAnswer()`     |
| Resposta contém fontes         | `responseGenerate_ReturnsAnswerWithSources()`       |

#### DocumentControllerTest (integração)

| Cenário                        | Teste                                               |
| ------------------------------ | --------------------------------------------------- |
| Upload PDF válido              | `upload_ValidPdf_Returns201WithChunks()`            |
| Upload DOCX válido             | `upload_ValidDocx_Returns201WithChunks()`           |
| Upload tipo inválido           | `upload_InvalidType_Returns400()`                   |
| Upload sem autenticação        | `upload_WithoutToken_Returns401()`                  |

#### Security Tests

| Cenário                        | Teste                                               |
| ------------------------------ | --------------------------------------------------- |
| Acesso sem token               | `getDocumentEndpoint_WithoutToken_Returns401()`     |
| Acesso com token válido        | `getDocumentEndpoint_WithValidToken_Returns200()`   |
| Acesso com token expirado      | `getDocumentEndpoint_ExpiredToken_Returns401()`     |
| Login com credenciais válidas  | `login_ValidCredentials_ReturnsToken()`             |
| Login com senha inválida       | `login_InvalidPassword_Returns401()`                |

### 13.3 Testcontainers — pgvector

```java
@SpringBootTest
@Testcontainers
class RagServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private RagService ragService;

    @Test
    void retrieveContext_ValidQuestion_ReturnsRagResult() {
        var result = ragService.retrieveContext(
            "Qual o teor do documento?", 5, 0.75, 4000);
        assertNotNull(result);
        assertNotNull(result.context());
    }
}
```

---

## 14. Ordem de Implementação

| Fase | Etapa                           | Descrição                                                       |
| ---- | ------------------------------- | --------------------------------------------------------------- |
| 1    | pom.xml                         | Adicionar dependências: PostgreSQL, Spring AI, MapStruct, JWT, PDFBox, POI |
| 2    | application.yml                 | Configurar datasource PostgreSQL, pgvector, Open Router, chunking, RAG |
| 3    | .env                            | Adicionar OPEN_ROUTER_API_KEY, JWT_SECRET, N8N_WEBHOOK_URL      |
| 4    | Flyway migrations               | V4 (users), V5 (documents), V6 (document_chunks com pgvector)   |
| 5    | Novas entidades                 | Document, DocumentChunk, User                                   |
| 6    | Novos repositórios              | DocumentRepository, DocumentChunkRepository (busca vetorial), UserRepository |
| 7    | Mappers (MapStruct)             | DocumentMapper, UserMapper, atualizar mappers existentes        |
| 8    | DTOs                            | LoginRequest, UserCreateRequest, DocumentResponse, ChatResponseV2, SourceReference |
| 9    | Security                        | TokenProvider, JwtAuthenticationFilter, SecurityConfig, UserDetailsServiceImpl |
| 10   | OpenAiConfig                    | Beans ChatModel + EmbeddingModel apontando para Open Router     |
| 11   | Parsers                         | DocumentParser interface, PdfParser, DocxParser, TxtParser      |
| 12   | TextChunker                     | Algoritmo de fragmentação com chunkSize/overlap/delimiters      |
| 13   | EmbeddingService                | Interface + EmbeddingServiceImpl                                |
| 14   | RagService                      | Orquestrador do fluxo RAG                                       |
| 15   | BotServiceImpl                  | Substitui MockBotServiceImpl — usa ChatModel + RagService       |
| 16   | ChatServiceImpl                 | Atualizar para usar BotServiceImpl real e retornar ChatResponseV2 |
| 17   | DocumentController              | Upload com pipeline completo (parse + chunk + embed + persist)  |
| 18   | AuthController + UserController | Login, criação de usuário                                       |
| 19   | N8nWebhookNotifier              | Notificação assíncrona                                          |
| 20   | Atualizar GlobalExceptionHandler| Novos handlers para DocumentProcessingException, DuplicateResourceException, AuthenticationException |
| 21   | Testes unitários                | TextChunkerTest, EmbeddingServiceImplTest, RagServiceTest, TokenProviderTest |
| 22   | Testes de integração            | Testcontainers com pgvector, DocumentControllerTest, SecurityTests |
| 23   | Swagger/OpenAPI                 | Atualizar documentação com novos endpoints                      |

---

## 15. Regras Arquiteturais Inegociáveis

| #  | Regra                                                                                                 | Violação exemplar                                                                        |
| -- | ----------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| 1  | **Controllers não contêm lógica de negócio** — apenas delegam para services e retornam DTOs           | `controller` chamar `embeddingService.generateEmbedding()` diretamente                   |
| 2  | **Services concentram toda lógica** — nunca expõem entidades JPA                                      | `service` retornar `Document` (entity) em vez de `DocumentResponse`                      |
| 3  | **Repositories só fazem acesso a dados** — sem regras de negócio                                      | `repository` ter método que aplica filtro semântico antes de retornar                    |
| 4  | **Entities jamais são retornadas em controllers** — mapper obrigatório                                | `controller` receber ou devolver `@Entity` como parâmetro                                |
| 5  | **DTOs de request usam Bean Validation** (`@NotBlank`, `@NotNull`, `@Size`)                           | DTO sem validação aceitando campos nulos                                                 |
| 6  | **Tratamento de erros é centralizado** em um único `@RestControllerAdvice`                            | Cada controller ter seu próprio try-catch                                                |
| 7  | **BotService é injetado por interface** — troca de provider sem alterar controllers                   | `ChatServiceImpl` instanciar `new BotServiceImpl()` diretamente                          |
| 8  | **Flyway migrations são a única fonte de verdade do schema** — não usar `ddl-auto=update` em produção | `spring.jpa.hibernate.ddl-auto=update` em produção                                       |
| 9  | **TextChunker não conhece banco de dados** — serviço puro de manipulação de string                    | `TextChunker` injetar `DocumentChunkRepository`                                          |
| 10 | **EmbeddingService não conhece entidades** — recebe String, retorna List<Float>                       | `EmbeddingService` depender de `Document` ou `DocumentChunk`                            |
| 11 | **Spring AI é o único middleware de IA** — sem chamadas HTTP diretas a APIs de LLM                    | `BotServiceImpl` fazer `RestTemplate.exchange()` para Open Router                       |
| 12 | **Parâmetros de RAG e chunking são configurados via application.yml** — sem hardcoded                 | `topK = 5` literal no código em vez de `@Value("${app.rag.top-k}")`                      |
| 13 | **ChatResponseV2 retorna metadados completos** — não apenas a string da resposta                      | `sendMessage` retornar apenas `String answer` sem `sources`, `executionTimeMs`, `chunksConsumed` |
| 14 | **Segurança stateless** — sem sessões HTTP, sem JSESSIONID                                            | `SecurityConfig` com `sessionManagement().sessionCreationPolicy(IF_REQUIRED)`            |
| 15 | **MockBotServiceImpl é eliminado** — produção usa BotServiceImpl real com RAG                         | Manter `MockBotServiceImpl` ativo com `@Primary` ou `@Profile("dev")`                    |

---

> Este documento é o guia oficial de implementação da Camada de Inteligência Artificial do Chatbot API.  
> Deve ser lido em conjunto com o `SYSTEM_SPECIFICATION.md` (sistema base).  
> Qualquer desvio arquitetural deve ser aprovado pela equipe antes de ser incorporado ao código.

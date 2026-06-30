# CODE REVIEW 3 — Novas Camadas de Inteligência (RAG, Embeddings, Chunking, Document Pipeline)

## Contexto
Adição de suporte a documentos (upload, parsing, chunking, embeddings), busca semântica (RAG) e notificação via n8n. Inclui novos endpoints, serviços, entidades e mappers.

---

## Resumo

| Prioridade | Total |
|-----------|-------|
| 🔴 Crítico | 1 |
| 🟡 Médio | 7 |
| 🔵 Leve | 5 |
| ⚪ Sugestão | 3 |
| **Total** | **16** |

---

## 🔴 Crítico

### 1. `DocumentService.java` — `originalText` comentado mas builder ainda chama `.originalText(text)`

**Arquivo:** `service/DocumentService.java:62`

**Problema:** O campo `originalText` foi comentado na entity `Document.java`, mas o builder em `DocumentService.java` ainda chama `.originalText(text)`, causando erro de compilação.

**Solução:** Comentar a chamada `.originalText(text)` no builder.

```java
// .originalText(text) // COMENTADO: evita OOM — revisar com front
```

---

## 🟡 Médio

### 2. `pom.xml` — Dependências faltando (spring-boot-starter-security, Spring AI, jjwt, MapStruct, PDFBox, POI)

**Arquivo:** `pom.xml`

**Problema:** Várias dependências usadas no código não estão declaradas.

**Solução:** Adicionar:
- `spring-boot-starter-security`
- Spring AI BOM + `spring-ai-openai-spring-boot-starter`
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (0.12.6)
- `mapstruct` + `mapstruct-processor` (1.6.3)
- `pdfbox` (3.0.4)
- `poi-ooxml` (5.4.0)

### 3. `N8nWebhookNotifier` — RestTemplate instanciado diretamente (viola DIP)

**Arquivo:** `service/integration/N8nWebhookNotifier.java:18`

**Problema:** `RestTemplate` é criado com `new RestTemplate()` em vez de ser injetado.

**Solução:** Injetar via construtor. O `RestTemplate` bean já existe em `AsyncConfig`.

### 4. `DocumentChunkRepository` — Query nativa sem JOIN com `documents`

**Arquivo:** `repository/DocumentChunkRepository.java:16`

**Problema:** A query não traz o `document_name`, necessário para exibir a fonte no front-end.

**Solução (Opção B — JOIN):** Alterar a query para JOIN com `documents` e incluir `d.file_name AS documentName`.

### 5. `ChunkSimilarityProjection` — `getDocumentName()` ausente

**Arquivo:** `repository/ChunkSimilarityProjection.java`

**Problema:** A projeção não declara `getDocumentName()`, então o JOIN não pode entregar o nome do documento.

**Solução:** Adicionar `String getDocumentName()`.

### 6. `RagService` — Sem validação de parâmetros

**Arquivo:** `service/rag/RagService.java:19`

**Problema:** Nenhuma validação de `question` (null/blank), `topK` (>0), `minSimilarity` (0-1), `maxContextSize` (>0).

**Solução:** Adicionar validação com `IllegalArgumentException`.

### 7. `RagService` — `SourceReference` criado com `null` no lugar do `documentName`

**Arquivo:** `service/rag/RagService.java:33-34`

**Problema:** `chunk.getDocumentName()` não é chamado; passa `null` no lugar do fileName.

**Solução:** Substituir `null` por `chunk.getDocumentName()`.

### 8. `TextChunker` — Ausência de `@PostConstruct init()` para validar regex dos delimiters

**Arquivo:** `service/chunking/TextChunker.java`

**Problema:** Regex mal formatada no `application.yml` só seria descoberta em runtime.

**Solução:** Adicionar método `@PostConstruct init()` que compila o regex antecipadamente.

---

## 🔵 Leve

### 9. `TextChunker.getOverlap()` — Pode quebrar palavra no meio

**Arquivo:** `service/chunking/TextChunker.java:61-66`

**Problema:** `text.substring(text.length() - overlap)` pode cortar no meio de uma palavra.

**Solução:** Buscar o último espaço dentro da substring e cortar a partir dele.

### 10. `BotServiceImpl.responseGenerate()` — Código duplicado com `responseGenerateWithMetadata()`

**Arquivo:** `service/chat/BotServiceImpl.java:33-60`

**Problema:** Ambos os métodos têm a mesma lógica (RAG + LLM), duplicada.

**Solução:** `responseGenerate()` deve delegar para `responseGenerateWithMetadata()` e extrair `.context()`.

### 11. `ChatController` — Ausência do endpoint `/send-v2`

**Arquivo:** `controller/ChatController.java`

**Problema:** O endpoint atual `/send` já retorna `ChatResponseV2`, mas deveria haver um `/send-v2` separado que expõe metadados.

**Solução:** Criar `POST /api/chat/send-v2` que retorna `ChatResponseV2`.

### 12. `DocumentService.selectParser()` — O(n) por reflexão, deveria ser O(1) com mapa

**Arquivo:** `service/DocumentService.java:106-113`

**Problema:** Itera a lista de parsers comparando nomes de classe.

**Solução:** Usar `Map<String, DocumentParser>` estático (PARSER_MAP) com chave = extensão.

### 13. `GlobalExceptionHandler` — Faltam handlers para exceções específicas

**Arquivo:** `exception/GlobalExceptionHandler.java`

**Problema:** `DocumentProcessingException`, `DuplicateResourceException` e `AuthenticationException` existem mas não têm handlers.

**Solução:** Adicionar 3 handlers com HTTP status apropriados (422, 409, 401).

---

## ⚪ Sugestão

### 14. `ChatResponse` — Deprecatar em favor de `ChatResponseV2`

**Arquivo:** `dto/response/ChatResponse.java`

**Problema:** `ChatResponse` é um record simples sem metadados; `ChatResponseV2` é superior.

**Solução:** Anotar com `@Deprecated(since = "2.0", forRemoval = true)`.

### 15. `OpenAiConfig` / `EmbeddingServiceImpl` — Compatibilidade com Spring AI auto-configuration

**Arquivo:** `config/OpenAiConfig.java`, `service/embedding/EmbeddingServiceImpl.java`

**Problema:** Spring AI auto-configura `OpenAiChatModel` e `OpenAiEmbeddingModel` a partir do `application.yml`. Beans manuais podem conflitar.

**Solução:** Usar `@ConditionalOnMissingBean` nos beans manuais.

### 16. Testes unitários — Cobrir novas camadas

**Arquivo:** `src/test/java/.../`

**Problema:** Novas classes sem cobertura de testes.

**Solução:** Criar testes para `TextChunker`, `RagService`, `BotServiceImpl`, `TokenProvider`.

---

## Itens Implementados

- [x] AsyncConfig com @EnableAsync + RestTemplate timeout 5s
- [x] N8nWebhookNotifier com RestTemplate injetado via construtor
- [x] DocumentChunkRepository com JOIN e documentName
- [x] ChunkSimilarityProjection com getDocumentName()
- [x] RagService com validação e chunk.getDocumentName()
- [x] TextChunker com @PostConstruct init() e overlap sem quebra de palavra
- [x] BotServiceImpl.responseGenerate() delegando para responseGenerateWithMetadata()
- [x] ChatController com POST /api/chat/send-v2
- [x] DocumentService com PARSER_MAP
- [x] GlobalExceptionHandler com DocumentProcessing (422), DuplicateResource (409), Authentication (401)
- [x] ChatResponse com @Deprecated
- [x] OpenAiConfig com @ConditionalOnMissingBean
- [x] EmbeddingServiceImpl com EmbeddingRequest
- [x] PGvectorUserType (Hibernate UserType)
- [x] Document.java com originalText comentado
- [x] Testes: TextChunkerTest, RagServiceTest, BotServiceImplTest, TokenProviderTest
- [x] pom.xml com security, spring-ai, jjwt, mapstruct, pdfbox, poi

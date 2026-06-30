# Code Review 3 — Intelligence Layers (RAG, Embeddings, Chunking, Document Pipeline)

> **Data:** 29/06/2026  
> **Revisor:** Arquiteto de Software  
> **Escopo:** Novas camadas de inteligência implementadas — `service/rag/`, `service/embedding/`, `service/chunking/`, `service/chat/`, `service/parser/`, `service/integration/`, `entity/Document*`, `entity/PGvector`, `entity/User`, `repository/Document*`, `repository/ChunkSimilarityProjection`, `security/`, `config/OpenAiConfig`, `controller/DocumentController`, `dto/response/*V2*`, `dto/response/SourceReference`

---

## Sumário

| Gravidade                                  | Qtd |
| ------------------------------------------ | --- |
| 🔴 Crítico (impede build ou funcionamento) | 1   |
| 🟡 Alto (comportamento incorreto)          | 7   |
| 🔵 Médio (violação de boas práticas)       | 5   |
| ⚪ Baixo (cosmético/sugestão)              | 3   |

---

## 1. 🔴 Críticos

### 1.1 `@Async` sem `@EnableAsync` — notificação n8n nunca dispara

**Arquivo:** `service/integration/N8nWebhookNotifier.java:20`  
**Problema:** O método `notify()` está anotado com `@Async`, mas não há `@EnableAsync` em nenhuma classe de configuração (`config/OpenAiConfig.java`, `ChatbotApiApplication.java`, etc.).  
**Impacto:** O Spring ignora a anotação — a chamada a `n8nNotifier.notify()` executa **de forma síncrona** no mesmo thread do upload, bloqueando a resposta até o timeout do n8n. Se o n8n estiver offline, o upload falha.  
**Correção:** Adicionar `@EnableAsync` em uma classe `@Configuration`:

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    // Configuração opcional de TaskExecutor
}
```

Ou diretamente na aplicação:

```java
@SpringBootApplication
@EnableAsync
public class ChatbotApiApplication { ... }
```

---

## 2. 🟡 Alta Gravidade

### 2.1 TextChunker: Regex injetado via `@Value` sem sanitização — `PatternSyntaxException` em runtime

**Arquivo:** `service/chunking/TextChunker.java:18-19,50`  
**Problema:** A propriedade `${app.chunking.delimiters}` é interpolada diretamente em `String.split("(?<=" + delimiters + ")")` sem validação. Se alguém configurar um delimiter inválido (ex: `(unclosed`), o split lança `PatternSyntaxException` em runtime.  
**Impacto:** Um erro de configuração quebra todo o pipeline de chunking, impedindo upload de documentos.  
**Correção:** Validar o regex no construtor ou `@PostConstruct`:

```java
@PostConstruct
public void init() {
    try {
        Pattern.compile(delimiters);
    } catch (PatternSyntaxException e) {
        throw new IllegalArgumentException("Delimitador inválido: " + delimiters, e);
    }
}
```

---

### 2.2 TextChunker: Overlap quebra palavras no meio — perda de contexto semântico

**Arquivo:** `service/chunking/TextChunker.java:61-66`  
**Problema:** `getOverlap()` retorna os últimos N caracteres sem considerar fronteiras de palavra. Ex: texto `"...importante revisar"` com overlap=7 retorna `" revisar"` (ok), mas se overlap=4 retorna `"sar"` — fragmento sem sentido semântico que será prepended ao próximo chunk.  
**Impacto:** O overlap, que deveria preservar contexto, na verdade insere ruído nos chunks, degradando a qualidade da busca vetorial e da resposta do RAG.  
**Correção:** Ajustar o overlap para a última fronteira de palavra/pontuação dentro do limite:

```java
private String getOverlap(String text) {
    if (text.length() <= overlap) return "";
    var candidate = text.substring(text.length() - overlap);
    // Ajustar para não quebrar palavras
    var firstSpace = candidate.indexOf(' ');
    return firstSpace > 0 ? candidate.substring(firstSpace + 1) : candidate;
}
```

---

### 2.3 RagService: `SourceReference` sempre com `fileName = null`

**Arquivo:** `service/rag/RagService.java:33`  
**Problema:** O `SourceReference` é criado com `fileName = null` mesmo que o `ChunkSimilarityProjection` contenha `documentId`, que poderia ser usado para buscar o nome do documento via `DocumentRepository`.  
**Impacto:** O front-end nunca exibe o nome do documento de origem da resposta — o campo `fileName` vem sempre nulo no `ChatResponseV2`, violando a regra arquitetural #13 (metadados completos).  
**Correção:** Adicionar consulta ao `DocumentRepository` para obter o `fileName`:

```java
// Opção A: Injeta DocumentRepository e busca nome
private final DocumentRepository documentRepository;

...

var fileName = documentRepository.findById(chunk.getDocumentId())
        .map(Document::getFileName)
        .orElse(null);
sources.add(new SourceReference(chunk.getDocumentId(), fileName, excerpt));

// Opção B (recomendada): Adicionar document_name na native query
// SELECT d.file_name AS document_name ...
// Alterar ChunkSimilarityProjection para incluir getDocumentName()
```

---

### 2.4 ChatController não expõe endpoint V2 — `sendMessageV2` órfão

**Arquivo:** `controller/ChatController.java`, `service/chat/ChatServiceImpl.java:42-57`  
**Problema:** O método `sendMessageV2()` foi implementado em `ChatServiceImpl` mas **não há endpoint no controller** para chamá-lo. O controller expõe apenas `send()` que chama `sendMessage()` (versão antiga sem RAG).  
**Impacto:** Toda a camada de RAG com metadados (sources, executionTimeMs, chunksConsumed) nunca é acessível via API REST. A especificação (seção 5.3) determina que o chat deve retornar `ChatResponseV2` com metadados completos.  
**Correção:** Alterar o endpoint existente ou criar um novo:

```java
// Opção A: Substituir o endpoint existente (breaking change)
@PostMapping("/send")
public ResponseEntity<ChatResponseV2> send(@Valid @RequestBody ChatRequest request) {
    var response = chatService.sendMessageV2(request);
    return ResponseEntity.ok(response);
}

// Opção B (recomendada): Criar novo endpoint /api/chat/send-v2
@PostMapping("/send-v2")
public ResponseEntity<ChatResponseV2> sendV2(@Valid @RequestBody ChatRequest request) {
    var response = chatService.sendMessageV2(request);
    return ResponseEntity.ok(response);
}
```

---

### 2.5 BotServiceImpl: Duplicação de chamada RAG — `responseGenerate()` vs `responseGenerateWithMetadata()`

**Arquivo:** `service/chat/BotServiceImpl.java:33-59`  
**Problema:** Ambos os métodos chamam `ragService.retrieveContext()` de forma independente. Quando `ChatServiceImpl.sendMessageV2()` (que retorna metadados) for integrado ao controller, cada requisição executará o **mesmo** `retrieveContext` duas vezes (uma em `responseGenerate` e outra em `responseGenerateWithMetadata`).  
**Impacto:** Dobro de chamadas ao embedding model (Open Router) para cada mensagem — latência e custo dobrados.  
**Correção:** Unificar em um único método que retorne `RagResult`:

```java
@Override
public RagResult responseGenerateWithMetadata(String userMessage) {
    var ragResult = ragService.retrieveContext(userMessage, topK, minSimilarity, maxContextSize);
    var prompt = buildPrompt(ragResult.context(), userMessage);
    var response = chatModel.call(
            new Prompt(prompt,
                    OpenAiChatOptions.builder().withModel(chatModelId).build())
    );
    return new RagResult(
            response.getResult().getOutput().getContent(),
            ragResult.sources(),
            ragResult.executionTimeMs(),
            ragResult.chunksConsumed()
    );
}
```

Eliminar `responseGenerate()` ou fazê-lo delegar para `responseGenerateWithMetadata()`.

---

### 2.6 DocumentService.selectParser: Seleção frágil por nome de classe

**Arquivo:** `service/DocumentService.java:106-113`  
**Problema:** `parser.getClass().getSimpleName().toLowerCase().contains(extension)` é um heurística frágil. Ex: um parser chamado `SuperTxtParser` matchearia `"txt"` mas também `"superpdfparser"` conteria `"pdf"`. Inversamente, se a extensão for "docx" e o nome da classe for `DocxParser`, funciona, mas a lógica quebra com qualquer convenção de nomenclatura diferente.  
**Impacto:** Risco de selecionar o parser errado se alguém adicionar novos parsers com nomes que contenham extensões incidentalmente.  
**Correção:** Usar `@Component("pdf")` ou um map explícito:

```java
private static final Map<String, Class<?>> PARSER_MAP = Map.of(
        "txt", TxtParser.class,
        "pdf", PdfParser.class,
        "docx", DocxParser.class
);

private DocumentParser selectParser(String extension) {
    var parserClass = PARSER_MAP.get(extension);
    if (parserClass == null) {
        throw new DocumentProcessingException("Parser não encontrado para: " + extension);
    }
    return parsers.stream()
            .filter(p -> p.getClass().equals(parserClass))
            .findFirst()
            .orElseThrow(() -> new DocumentProcessingException("Parser não disponível para: " + extension));
}
```

---

### 2.7 PGvector: Sem registro de tipo Hibernate para `vector(1536)`

**Arquivo:** `entity/DocumentChunk.java:42`, `entity/PGvector.java`  
**Problema:** O campo `embedding` usa `columnDefinition = "vector(1536)"` com tipo Java `PGvector`, mas não há um `UserType` ou `Dialect` registrado para o tipo `vector` do pgvector.  
**Impacto:** O Hibernate não sabe como mapear `PGvector` ↔ `vector(1536)`. Em tempo de execução, lança `MappingException` ou `SQLException` ao tentar persisted ou ler embeddings.  
**Correção:** Uma das seguintes abordagens:

1. **Registrar um `UserType` para PGvector** (mais correto arquiteturalmente)
2. **Usar `@Convert` com um `AttributeConverter`** que serialize/deserialize o vetor como string
3. **Manter o vetor como `float[]` ou `String`** e converter manualmente nos repositories

Exemplo com `AttributeConverter`:

```java
@Converter
public class PGvectorConverter implements AttributeConverter<PGvector, String> {
    @Override
    public String convertToDatabaseColumn(PGvector attribute) {
        return Arrays.toString(attribute.getVector()); // "[0.1, 0.2, ...]"
    }

    @Override
    public PGvector convertToEntityAttribute(String dbData) {
        // Parse "[0.1, 0.2, ...]" → float[]
        var values = dbData.replaceAll("[\\[\\]]", "").split(", ");
        var vector = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            vector[i] = Float.parseFloat(values[i]);
        }
        return new PGvector(vector);
    }
}
```

---

## 3. 🔵 Média Gravidade

### 3.1 RagService: Sem validação de parâmetros de entrada

**Arquivo:** `service/rag/RagService.java:19-47`  
**Problema:** Nenhum dos parâmetros (`question`, `topK`, `minSimilarity`, `maxContextSize`) é validado. `question` pode ser null/blank (causando erro no embedding), `topK` pode ser negativo (query SQL inválida), `maxContextSize` pode ser zero (nunca adiciona chunks).  
**Impacto:** Erros obscuros vindos do modelo de embedding ou do banco em vez de mensagens claras.  
**Correção:**

```java
public RagResult retrieveContext(String question, int topK, double minSimilarity, int maxContextSize) {
    if (question == null || question.isBlank()) {
        throw new IllegalArgumentException("Pergunta não pode ser vazia");
    }
    if (topK <= 0) {
        throw new IllegalArgumentException("topK deve ser positivo");
    }
    if (minSimilarity < 0 || minSimilarity > 1) {
        throw new IllegalArgumentException("minSimilarity deve estar entre 0 e 1");
    }
    if (maxContextSize <= 0) {
        throw new IllegalArgumentException("maxContextSize deve ser positivo");
    }
    // ...
}
```

---

### 3.2 DocumentService: `originalText` armazenado em memória — risco de OOM

**Arquivo:** `entity/Document.java:44`, `service/DocumentService.java:62`  
**Problema:** O campo `originalText` armazena o texto completo do documento original no banco (TEXT) e é carregado integralmente em memória pela JPA. Para documentos grandes (>100MB), isso pode causar `OutOfMemoryError`.  
**Impacto:** Upload de PDFs extensos pode derrubar a aplicação.  
**Correção:** Avaliar se `originalText` é realmente necessário. Se for, considerar:

1. Armazenar em um bucket S3/local e salvar apenas o path no banco
2. Usar `@Basic(fetch = FetchType.LAZY)` com bytecode enhancement
3. Remover o campo se não for usado por nenhuma funcionalidade

---

### 3.3 `DocumentProcessingException` não tratado no `GlobalExceptionHandler`

**Arquivo:** `exception/GlobalExceptionHandler.java`  
**Problema:** A especificação (seção 12.3) define handlers para `DocumentProcessingException`, `AuthenticationException` e `DuplicateResourceException`, mas eles **não foram implementados** no `GlobalExceptionHandler`.  
**Impacto:** Erros como "Falha ao processar PDF" caem no handler genérico (`Exception.class`) e retornam 500 Internal Server Error em vez de 422 Unprocessable Entity.  
**Correção:** Adicionar os handlers conforme especificação:

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
```

---

### 3.4 DocumentChunkRepository: Projeção nativa sem `document_name` — SourceReference incompleto

**Arquivo:** `repository/DocumentChunkRepository.java:15-22`  
**Problema:** A query nativa não seleciona `d.file_name AS documentName`. O `ChunkSimilarityProjection` não tem `getDocumentName()`. Isso força o `RagService` a deixar `fileName = null` em `SourceReference`.  
**Impacto:** Relacionado ao item 2.3 — sem o nome do documento na query, nunca teremos `fileName` preenchido na resposta.  
**Correção:** Alterar a query para JOIN com `documents`:

```sql
SELECT c.id, c.document_id AS documentId, d.file_name AS documentName,
       c.content, c.chunk_index AS chunkIndex, c.created_at AS createdAt,
       1 - (c.embedding <=> :queryVector) AS similarity
FROM document_chunks c
JOIN documents d ON d.id = c.document_id
WHERE 1 - (c.embedding <=> :queryVector) >= :minSimilarity
ORDER BY c.embedding <=> :queryVector
LIMIT :topK
```

E adicionar `getDocumentName()` em `ChunkSimilarityProjection`.

---

### 3.5 N8nWebhookNotifier: RestTemplate sem configuração de timeout

**Arquivo:** `service/integration/N8nWebhookNotifier.java:18`  
**Problema:** `new RestTemplate()` é criado sem timeouts. Se o n8n estiver lento ou offline, o thread (mesmo com `@Async`) pode ficar bloqueado por minutos até o timeout default do HTTP connection.  
**Impacto:** Acúmulo de threads bloqueados em cenário de n8n instável.  
**Correção:** Configurar timeouts no `RestTemplate` ou (melhor) injetar um bean configurado:

```java
@Bean
public RestTemplate restTemplate() {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5000);
    factory.setReadTimeout(5000);
    return new RestTemplate(factory);
}
```

---

## 4. ⚪ Baixa Gravidade (Sugestões)

### 4.1 Nenhum teste unitário para as novas camadas de inteligência

**Arquivos:** `src/test/java/br/com/chatbot/chatbot_api/`  
**Problema:** Não existem testes para `TextChunker`, `EmbeddingServiceImpl`, `RagService`, `BotServiceImpl`, `DocumentService`, `DocumentController`, `TokenProvider`, nem `SecurityConfig`. A especificação (seção 13) define cenários de teste detalhados para cada um.  
**Sugestão:** Implementar ao menos os testes unitários prioritários:

- **TextChunkerTest:** texto pequeno, texto grande com overlap, texto vazio, delimitadores personalizados
- **RagServiceTest:** contexto válido, filtro por similaridade, limite de tamanho
- **BotServiceImplTest:** resposta com RAG, resposta com fontes
- **DocumentServiceTest:** upload de txt/pdf/docx válido, tipo inválido, parsing de arquivo corrompido
- **TokenProviderTest:** geração e validação de token, token inválido

---

### 4.2 `OpenAiConfig` usa `@ConditionalOnMissingBean` — pode mascarar beans customizados

**Arquivo:** `config/OpenAiConfig.java:16,31`  
**Problema:** `@ConditionalOnMissingBean` nos beans `openAiChatModel` e `openAiEmbeddingModel` faz com que, se o Spring Boot autoconfiguration já criar esses beans (via `spring.ai.openai.api-key` no application.yml), os beans definidos aqui são ignorados silenciosamente.  
**Sugestão:** Remover `@ConditionalOnMissingBean` e deixar os beans explícitos — ou remover os beans e configurar tudo via `application.yml`, já que o `spring-ai-openai-spring-boot-starter` suporta autoconfiguração completa.

---

### 4.3 `ChatResponseV2` e `ChatResponse` coexistem — pode causar confusão

**Arquivos:** `dto/response/ChatResponse.java`, `dto/response/ChatResponseV2.java`  
**Problema:** Dois DTOs de resposta de chat com propósitos sobrepostos. `ChatResponse` (legado) não tem metadados RAG. `ChatResponseV2` tem todos os metadados. Manter ambos aumenta a superfície de manutenção.  
**Sugestão:** Após validar que o endpoint V2 está funcional, marcar `ChatResponse` como `@Deprecated` e removê-lo no próximo release.

---

## 5. Resumo por Arquivo

| Arquivo                                               | Achados                                                                                     |
| ----------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| `service/chunking/TextChunker.java`                   | 🟡 regex sem validação; 🟡 overlap quebra palavras                                          |
| `service/rag/RagService.java`                         | 🟡 SourceReference sem fileName; 🔵 sem validação de parâmetros                             |
| `service/embedding/EmbeddingServiceImpl.java`         | ✅ OK                                                                                       |
| `service/chat/BotServiceImpl.java`                    | 🟡 duplicação de chamada RAG entre responseGenerate e responseGenerateWithMetadata          |
| `service/chat/ChatServiceImpl.java`                   | 🟡 sendMessageV2 órfão — controller não expõe                                               |
| `service/DocumentService.java`                        | 🟡 selectParser frágil; 🔵 originalText pode causar OOM                                     |
| `service/integration/N8nWebhookNotifier.java`         | 🔴 @Async sem @EnableAsync; 🔵 RestTemplate sem timeout                                     |
| `controller/ChatController.java`                      | 🟡 não expõe V2                                                                             |
| `controller/DocumentController.java`                  | ✅ OK (depende de DocumentService)                                                          |
| `entity/DocumentChunk.java`                           | 🟡 PGvector sem Hibernate type registration                                                 |
| `entity/PGvector.java`                                | 🟡 sem suporte a Hibernate                                                                  |
| `repository/DocumentChunkRepository.java`             | 🔵 query sem JOIN com documents → fileName sempre null                                      |
| `repository/ChunkSimilarityProjection.java`           | 🔵 falta getDocumentName()                                                                  |
| `config/OpenAiConfig.java`                            | ⚪ @ConditionalOnMissingBean pode mascarar beans                                             |
| `exception/GlobalExceptionHandler.java`               | 🔵 faltam handlers para DocumentProcessingException, AuthenticationException, DuplicateResourceException |
| `dto/response/ChatResponseV2.java`                    | ⚪ ChatResponse legado deveria ser depreciado                                                |
| `dto/response/SourceReference.java`                   | ✅ OK (mas nunca preenchido corretamente)                                                    |
| `test/`                                               | ⚪ sem testes para as novas camadas                                                          |

---

## 6. Checklist de Correções Prioritárias

- [ ] 🔴 1. **Adicionar `@EnableAsync`** em alguma `@Configuration` ou na aplicação principal
- [ ] 🟡 2. **Validar regex** dos delimiters no `TextChunker` via `@PostConstruct`
- [ ] 🟡 3. **Corrigir overlap** do `TextChunker` para não quebrar palavras
- [ ] 🟡 4. **Preencher `fileName`** no `SourceReference` (JOIN na query ou lookup via DocumentRepository)
- [ ] 🟡 5. **Expor endpoint V2** no `ChatController` ou substituir o existente
- [ ] 🟡 6. **Eliminar duplicação** de `ragService.retrieveContext()` em `BotServiceImpl`
- [ ] 🟡 7. **Corrigir `selectParser`** no `DocumentService` usando Map<extensão, Class>
- [ ] 🟡 8. **Registrar tipo Hibernate** para `PGvector` ou usar `AttributeConverter`
- [ ] 🔵 9. **Adicionar validação** de parâmetros no `RagService`
- [ ] 🔵 10. **Adicionar handlers** para `DocumentProcessingException`, `AuthenticationException`, `DuplicateResourceException` no `GlobalExceptionHandler`
- [ ] 🔵 11. **Adicionar `documentName`** na query nativa e na `ChunkSimilarityProjection`
- [ ] 🔵 12. **Configurar timeout** no `RestTemplate` do `N8nWebhookNotifier`
- [ ] ⚪ 13. **Implementar testes unitários** para TextChunker, RagService, EmbeddingServiceImpl, BotServiceImpl, DocumentService, TokenProvider
- [ ] ⚪ 14. **Avaliar remoção** de `@ConditionalOnMissingBean` no `OpenAiConfig`
- [ ] ⚪ 15. **Depreciar `ChatResponse`** em favor de `ChatResponseV2`

---

## 7. Regras Arquiteturais Violadas

| #  | Regra                                                                       | Violação                                               |
| -- | --------------------------------------------------------------------------- | ------------------------------------------------------ |
| 8  | Flyway migrations são a única fonte de verdade do schema                    | PGvector sem type mapping pode exigir ddl-auto         |
| 12 | Parâmetros de RAG e chunking são configurados via application.yml           | Delimiters sem validação (ok config, mas frágil)       |
| 13 | ChatResponseV2 retorna metadados completos                                  | fileName = null sempre                                 |
| 7  | BotService é injetado por interface                                         | ✅ OK (cumprido)                                       |
| 9  | TextChunker não conhece banco de dados                                      | ✅ OK                                                  |
| 10 | EmbeddingService não conhece entidades                                      | ✅ OK                                                  |
| 11 | Spring AI é o único middleware de IA                                        | ✅ OK                                                  |

---

*Este review considera apenas as novas camadas de inteligência implementadas entre os CODE_REVIEW.md e CODE_REVIEW_2.md. Issues já reportados anteriormente (pom.xml, entities base, exception handler base) devem estar corrigidos conforme os checklists anteriores.*

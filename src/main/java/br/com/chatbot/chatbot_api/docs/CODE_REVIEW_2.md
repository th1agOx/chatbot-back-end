# Code Review — Chatbot API (Novos Packages)

> **Data:** 25/06/2026  
> **Revisor:** Arquiteto de Software  
> **Escopo:** `repository/`, `mapper/`, `service/` (interfaces + impl), `test/service/impl/`

---

## Arquivos Revisados

| Package              | Arquivos                                                                    |
| -------------------- | --------------------------------------------------------------------------- |
| `repository/`        | `ConversationRepository`, `MessageRepository`, `AttachmentRepository`       |
| `mapper/`            | `EntityMapper`                                                              |
| `service/`           | `ConversationService`, `ChatService`, `FileService` (interfaces)            |
| `service/impl/`      | `ConversationServiceImpl`, `ChatServiceImpl`, `FileServiceImpl`             |
| `test/service/impl/` | `ConversationServiceImplTest`, `ChatServiceImplTest`, `FileServiceImplTest` |

---

## 1. 🔴 Críticos

Nenhum — o código compila e funciona após as correções aplicadas no CODE_REVIEW.md.

---

## 2. 🟡 Alta Gravidade

### 2.1 ChatServiceImpl e FileServiceImpl dependem de implementação concreta

//Feito por humano

**Arquivos:**

- `service/impl/ChatServiceImpl.java:22` — `ConversationServiceImpl conversationService`
- `service/impl/FileServiceImpl.java:25` — `ConversationServiceImpl conversationService`

**Problema:** Ambos os services injetam a classe concreta `ConversationServiceImpl` em vez da interface `ConversationService`.

**Impacto:** Viola o **Dependency Inversion Principle (DIP)**. Se alguém criar uma segunda implementação de `ConversationService` (ex: `ConversationServiceCacheImpl`), o Spring não consegue substituir sem alterar o código desses services. Também dificulta testes unitários (obriga mock de classe concreta).

**Causa raiz:** O método `findConversationOrThrow()` em `ConversationServiceImpl` é `public`. Como não existe na interface `ConversationService`, os outros services são forçados a depender da implementação concreta.

**Correção:**

1. Adicionar o método na interface `ConversationService`:

```java
public interface ConversationService {
    ConversationResponse create(ConversationRequest request);
    List<ConversationResponse> findAll();
    ConversationResponse findById(Long id);
    void deleteById(Long id);
    Conversation findConversationOrThrow(Long id);  // ← adicionar
}
```

2. Alterar as injeções nos services para usar a interface:

```java
// ChatServiceImpl.java
private final ConversationService conversationService;

// FileServiceImpl.java
private final ConversationService conversationService;
```

3. Manter `findConversationOrThrow` como `public` em `ConversationServiceImpl`.

---

### 2.2 ChatServiceImpl: Lógica de resposta do BOT inline — sem BotService

//Feito por humano

**Arquivo:** `service/impl/ChatServiceImpl.java:58-60`

**Problema:** O método `generateBotResponse()` está embutido no `ChatServiceImpl`. A especificação exige que a geração de resposta seja feita por um `BotService` injetável, substituível futuramente por OpenAI, Gemini, etc. sem alterar `ChatServiceImpl`.

**Impacto:** Viola a **regra arquitetural #7** (BotService injetado por interface). Quando o colaborador de IA implementar `OpenAiBotService`, vai precisar refatorar `ChatServiceImpl`.

**Correção:** Extrair para interface + implementação mock:

```java
// service/BotService.java
public interface BotService {
    String generateResponse(String userMessage);
}

// service/impl/MockBotServiceImpl.java
@Service
public class MockBotServiceImpl implements BotService {
    @Override
    public String generateResponse(String userMessage) {
        return "Você disse: " + userMessage;
    }
}

// ChatServiceImpl.java — injetar BotService
private final BotService botService;
```

---

### 2.3 FileServiceImpl: `file.getOriginalFilename()` pode retornar null

// Feito por humano

**Arquivo:** `service/impl/FileServiceImpl.java:36`

**Problema:** `MultipartFile.getOriginalFilename()` pode retornar `null` segundo o contrato da interface. O valor é passado diretamente para `Attachment.builder().fileName(...)` sem validação.

**Impacto:** `NullPointerException` em runtime se o upload for feito sem nome de arquivo.

**Correção:**

```java
var fileName = file.getOriginalFilename();
if (fileName == null || fileName.isBlank()) {
    throw new InvalidFileTypeException("File name is required");
}
```

---

## 3. 🔵 Média Gravidade

### 3.1 `@Repository` redundante nos repositories (3 ocorrências)

// Feito por humano

**Arquivos:** `repository/ConversationRepository.java:7`, `MessageRepository.java:9`, `AttachmentRepository.java:7`

**Problema:** `JpaRepository` já possui `@Repository` herdado via `SimpleJpaRepository`. A anotação é redundante.

**Impacto:** Nenhum funcional, mas código ruidoso. Pode remover.

---

### 3.2 `ConversationServiceImpl.findConversationOrThrow()` público — expõe entity

/\* Não entendi, eu instancio o metodo findById no repositoy

- Porque o metodo findConversationOrThrow já localiza o id
  \*\

**Arquivo:** `service/impl/ConversationServiceImpl.java:54`

**Problema:** Método retorna `Conversation` (entity JPA) publicamente. Isso quebra o isolamento — services externos recebem uma entidade gerenciada pelo JPA.

**Impacto:** Se o método for chamado fora de uma transação, pode lançar `LazyInitializationException`. Também acopla outros services ao modelo JPA.

**Correção:** Manter como `public` na interface (conforme 2.1) mas ciente de que retorna entity. Idealmente criar um método separado que retorne apenas o ID ou usar projeção.

---

### 3.3 `FileServiceImpl` valida content-type mas não extensão do arquivo

// feito por humano

**Arquivo:** `service/impl/FileServiceImpl.java:50-53`

**Problema:** A validação confia apenas em `file.getContentType()`, que é enviado pelo cliente e pode ser falsificado. Uma requisição com `file.txt` mas `Content-Type: application/pdf` passa na validação.

**Correção:** Adicionar validação de extensão como fallback:

```java
private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".txt", ".pdf");

private void validateFile(MultipartFile file) {
    var originalName = file.getOriginalFilename();
    if (originalName != null) {
        var ext = originalName.toLowerCase();
        if (ALLOWED_EXTENSIONS.stream().noneMatch(ext::endsWith)) {
            throw new InvalidFileTypeException("Invalid file extension");
        }
    }
    // ... validação existing de content-type e tamanho
}
```

---

### 3.4 Testes mockam `ConversationServiceImpl` concreto

// opencode Faça

**Arquivos:**

- `test/service/impl/ChatServiceImplTest.java:29`
- `test/service/impl/FileServiceImplTest.java:26`

**Problema:** Os testes usam `@Mock ConversationServiceImpl conversationService`. Se algum método for adicionado/removido da implementação concreta, os testes quebram. Deveriam mockar a interface `ConversationService`.

**Impacto:** Testes frágeis e acoplados a detalhes de implementação.

**Correção:** Após a correção 2.1, trocar para `@Mock ConversationService conversationService`.

---

### 3.5 `application.yml` e código duplicam validação de tamanho de arquivo

// Feito por humano

**Arquivo:** `application.yml:29` + `service/impl/FileServiceImpl.java:21`

**Problema:** O limite de 10MB está configurado em dois lugares:

- `spring.servlet.multipart.max-file-size: 10MB`
- `FileServiceImpl.MAX_FILE_SIZE = 10 * 1024 * 1024L`

**Impacto:** Se um dia alterarem um e esquecerem o outro, o comportamento fica inconsistente.

**Correção:** Remover a constante do código e confiar na validação do Spring, ou usar `@Value` para injetar o valor do config:

```java
@Value("${spring.servlet.multipart.max-file-size}")
private DataSize maxFileSize;
```

---

## 4. ⚪ Baixa Gravidade

### 4.1 EntityMapper único — pode virar god class

**Arquivo:** `mapper/EntityMapper.java`

**Sugestão:** Separar em `ConversationMapper`, `MessageMapper`, `AttachmentMapper` para respeitar SRP (Single Responsibility Principle). Não urgente.

### 4.2 Cobertura de testes baixa

**Arquivos:**

- `ConversationServiceImplTest` — apenas 2 cenários (create, findById not found). Falta: `findAll`, `deleteById`, `deleteById not found`.
- `FileServiceImplTest` — apenas 2 cenários (txt válido, tipo inválido). Falta: `PDF válido`, `conversation not found`, `arquivo vazio`.

**Sugestão:** Adicionar cenários faltantes.

### 4.3 Nomenclatura: service interfaces sem prefixo "I"

**Arquivos:** `service/ConversationService.java`, `service/ChatService.java`, `service/FileService.java`

Nota: A convenção adotada (sem prefixo `I`) é boa e segue o padrão Spring. Apenas registro.

---

## 5. Resumo por Arquivo

| Arquivo                                     | Achados                                                                     |
| ------------------------------------------- | --------------------------------------------------------------------------- |
| `repository/ConversationRepository.java`    | 🔵 @Repository redundante                                                   |
| `repository/MessageRepository.java`         | 🔵 @Repository redundante; ✅ query method                                  |
| `repository/AttachmentRepository.java`      | 🔵 @Repository redundante                                                   |
| `mapper/EntityMapper.java`                  | ⚪ God class em potencial                                                   |
| `service/ConversationService.java`          | ✅ OK                                                                       |
| `service/ChatService.java`                  | ✅ OK                                                                       |
| `service/FileService.java`                  | ✅ OK                                                                       |
| `service/impl/ConversationServiceImpl.java` | 🟡 findConversationOrThrow público (causa 2.1)                              |
| `service/impl/ChatServiceImpl.java`         | 🟡 DIP violation; 🟡 sem BotService                                         |
| `service/impl/FileServiceImpl.java`         | 🟡 DIP violation; 🟡 getOriginalFilename null; 🔵 sem validação de extensão |
| `test/.../ConversationServiceImplTest.java` | 🟡 mock concreto; ⚪ baixa cobertura                                        |
| `test/.../ChatServiceImplTest.java`         | 🟡 mock concreto                                                            |
| `test/.../FileServiceImplTest.java`         | 🟡 mock concreto; ⚪ baixa cobertura                                        |

---

## 6. Checklist de Correções Recomendadas

- [ ] 1. **Adicionar** `findConversationOrThrow` na interface `ConversationService`
- [ ] 2. **Trocar** injeção de `ConversationServiceImpl` → `ConversationService` em `ChatServiceImpl` e `FileServiceImpl`
- [ ] 3. **Criar** interface `BotService` + implementação `MockBotServiceImpl`
- [ ] 4. **Injetar** `BotService` em `ChatServiceImpl` (remover `generateBotResponse` inline)
- [ ] 5. **Adicionar** validação de `null` para `file.getOriginalFilename()` em `FileServiceImpl`
- [ ] 6. **Adicionar** validação de extensão de arquivo em `FileServiceImpl`
- [ ] 7. **Atualizar** testes para mockarem interfaces em vez de classes concretas
- [ ] 8. **Adicionar** cenários de teste faltantes (delete, pdf, conversa não encontrada)
- [ ] 9. **Remover** `@Repository` redundante dos 3 repositories
- [ ] 10. **Avaliar** desduplicação do limite de tamanho (yml vs constante Java)

---

_Este review considera apenas os 4 novos packages. Issues já reportados no CODE_REVIEW.md (pom.xml, entities, exception handler, etc.) foram corrigidos separadamente._

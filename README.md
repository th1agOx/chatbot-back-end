## Grupo 7:
Douglas Costa,
João Pedro Mazzotti,
Matheus da Silveira Santos,
Thiago Rocha.

# Chatbot API - Backend

Backend REST de um chatbot com **RAG (Retrieval-Augmented Generation)** desenvolvido com **Java 21**, **Spring Boot 3**, **PostgreSQL + pgvector** e **Ollama** (modelos locais gratuitos).

---

## Sumário

- [Arquitetura](#arquitetura)
- [Pré-requisitos](#pré-requisitos)
- [Como Executar](#como-executar)
- [Endpoints da API](#endpoints-da-api)
- [Base de Conhecimento (RAG)](#base-de-conhecimento-rag)
- [O que perguntar ao Chatbot](#o-que-perguntar-ao-chatbot)
- [Fluxo Completo para Testar](#fluxo-completo-para-testar)
- [Swagger](#swagger)
- [Tecnologias](#tecnologias)
- [Estrutura do Projeto](#estrutura-do-projeto)

---

## Arquitetura

```
                    +-----------+
                    |  Frontend  |
                    | (Vite 5173)|
                    +-----+-----+
                          |
                     CORS OK
                          |
          +---------------v----------------+
          |       REST API (8080)           |
          |  Spring Boot 3 + Java 21        |
          +---+-----------+-----------+----+
              |           |           |
              v           v           v
        +----------+ +----------+ +----------+
        |   Chat   | |   RAG    | |  Docs/   |
        | Service  | | Service  | | Knowledge|
        +----+-----+ +----+-----+ +----+-----+
             |             |             |
             v             v             v
        +----------+ +----------+ +----------+
        |  Message | | pgvector | | Postgres |
        |  Repository| | (768d)   | |   SQL    |
        +----------+ +----------+ +----------+
                          |
                     +----v----+
                     |  Ollama  |
                     | (local)  |
                     +----+----+
                          |
              +-----------+-----------+
              |                       |
         +----v----+           +------v------+
         | llama3.2|           | nomic-embed |
         |  (chat) |           |   (embed)   |
         +---------+           +-------------+
```

## Pré-requisitos

- **Java 21+**
- **Maven** (ou use o `mvnw` incluso)
- **PostgreSQL 15+** com extensão **pgvector**
- **Ollama** rodando localmente com os modelos:
  - `llama3.2:1b` (chat)
  - `nomic-embed-text` (embeddings)

### Instalar Ollama e modelos

```bash
# Instalar Ollama: https://ollama.com/download

# Baixar os modelos
ollama pull llama3.2:1b
ollama pull nomic-embed-text
```

### Configurar PostgreSQL com pgvector

```bash
# Criar banco
createdb postgres

# Ativar extensão pgvector
psql -U postgres -d postgres -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

---

## Como Executar

```bash
# 1. Subir banco com Docker (opcional, se tiver docker-compose.yml)
docker compose up -d

# 2. Executar a aplicação
./mvnw spring-boot:run

# 3. A base de conhecimento de tecnologia carrega automaticamente no startup
#    (16 arquivos sobre Java, Spring, SQL, Docker, etc.)

# 4. Acessar: http://localhost:8080
#    Swagger:  http://localhost:8080/swagger-ui.html
```

---

## Endpoints da API

### Health

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/health` | Status da aplicação |

### Conversas

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/api/conversations` | Criar conversa (body: `{ "title": "..." }`) |
| GET | `/api/conversations` | Listar todas as conversas |
| GET | `/api/conversations/{id}` | Buscar conversa por ID |
| PUT | `/api/conversations/{id}` | Renomear conversa (body: `{ "title": "..." }`) |
| DELETE | `/api/conversations/{id}` | Excluir conversa |

**Exemplo PUT:**
```bash
curl -X PUT http://localhost:8080/api/conversations/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"Dúvidas sobre Java"}'
```
Resposta: `{ "id": 1, "title": "Dúvidas sobre Java", "createdAt": "...", "updatedAt": "..." }`

### Chat

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/api/chat/send-v2` | Enviar mensagem com resposta RAG |
| GET | `/api/chat/history/{conversationId}` | Histórico de mensagens da conversa |

**Exemplo de conversa (recomendado para testar):**

```bash
# 1. Enviar mensagem (se conversationId for null, cria conversa automaticamente)
curl -X POST http://localhost:8080/api/chat/send-v2 \
  -H "Content-Type: application/json" \
  -d '{"message":"O que é polimorfismo em Java?"}'

# Resposta: { "conversationId": 1, "userMessage": {...}, "botMessage": {...},
#             "answer": "Polimorfismo é...", "sources": [...], ... }

# 2. Continuar a conversa (usando o conversationId retornado)
curl -X POST http://localhost:8080/api/chat/send-v2 \
  -H "Content-Type: application/json" \
  -d '{"conversationId":1,"message":"Explique a diferença entre ArrayList e LinkedList"}'

# 3. Ver histórico
curl http://localhost:8080/api/chat/history/1

# Resposta: [ { "id":1, "role":"USER", "content":"O que é polimorfismo...", "createdAt":"..." },
#             { "id":2, "role":"BOT", "content":"Polimorfismo é...", "createdAt":"..." }, ... ]
```

### Documentos (Upload de Arquivos)

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/api/documents/upload` | Upload de arquivo (PDF, DOCX, TXT) para alimentar o RAG |
| GET | `/api/documents/{id}/status` | Ver status do processamento |

**Exemplo:**
```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@manual.pdf"
```

### Conhecimento Direto (sem arquivo)

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/api/knowledge/add` | Adicionar texto puro como conhecimento |

**Exemplo:**
```bash
curl -X POST http://localhost:8080/api/knowledge/add \
  -H "Content-Type: application/json" \
  -d '{"content":"Kubernetes é uma plataforma de orquestração de containers...","source":"DevOps"}'
```

---

## Base de Conhecimento (RAG)

O chatbot vem com **16 módulos de conhecimento** carregados automaticamente no startup sobre tecnologia, do básico ao avançado:

| Módulo | Tópicos |
|--------|---------|
| Java Fundamentos | JVM, tipos primitivos, Garbage Collector, modificadores de acesso |
| Java POO | Encapsulamento, herança, polimorfismo, interfaces, composição |
| Java Collections | List, Set, Map, HashMap interno, equals/hashCode, Comparable/Comparator |
| Java Streams | Streams API, lambdas, interfaces funcionais, Optional |
| Java Exceptions | Checked/unchecked, try-with-resources, boas práticas |
| Spring Boot | Auto-configuration, starters, profiles, Actuator |
| Spring DI/IoC | Injeção por construtor, escopos, ciclo de vida, AOP |
| Spring Data JPA | Repositories, query methods, N+1, locking, auditoria |
| Spring REST APIs | REST, códigos HTTP, validação, HATEOAS |
| SQL | SELECT, JOINs, índices, normalização, transações ACID |
| PostgreSQL + pgvector | pgvector, HNSW, IVFFlat, busca por similaridade |
| Docker | Dockerfile, Compose, volumes, redes, multi-stage |
| Design Patterns | Singleton, Factory, Builder, Strategy, Observer, Template Method |
| Estruturas de Dados | Arrays, listas, árvores, hash, grafos, Big O |
| Spring Security + JWT | Filter Chain, JWT, CORS, BCrypt |
| SOLID + Clean Code | SRP, OCP, LSP, ISP, DIP, nomes, funções, testes |

---

## O que perguntar ao Chatbot

Abaixo estão exemplos de perguntas que o chatbot consegue responder com base no conhecimento carregado:

### Java
- "O que é a JVM e como ela funciona?"
- "Explique os 4 pilares da orientação a objetos"
- "Qual a diferença entre ArrayList e LinkedList?"
- "Como funciona o HashMap internamente?"
- "O que é uma expressão lambda?"
- "Qual a diferença entre checked e unchecked exception?"
- "O que é o Garbage Collector e como ele funciona?"
- "Explique o polimorfismo com exemplos"
- "O que é a diferença entre Comparable e Comparator?"
- "Como funciona o try-with-resources?"

### Spring Boot
- "O que é injeção de dependência?"
- "Explique o que é um bean no Spring"
- "Qual a diferença entre @Component, @Service e @Repository?"
- "O que é o Spring Data JPA?"
- "Como resolver o problema N+1 no Hibernate?"
- "Explique o padrão REST e seus métodos HTTP"
- "O que são perfis (profiles) no Spring Boot?"
- "Como tratar exceções globalmente em uma API REST?"

### Banco de Dados
- "Qual a diferença entre LEFT JOIN e INNER JOIN?"
- "O que é normalização de banco de dados?"
- "Explique os níveis de isolamento de transação"
- "O que é pgvector e para que serve?"

### Docker
- "Qual a diferença entre imagem e container?"
- "O que é Docker Compose?"
- "Explique o que é multi-stage build"

### Design e Arquitetura
- "Explique o padrão Singleton"
- "Qual a diferença entre Factory Method e Abstract Factory?"
- "Explique os princípios SOLID"
- "O que é Clean Code?"

### Segurança
- "Como funciona o JWT?"
- "O que é CORS?"
- "Por que usar BCrypt para senhas?"

---

## Fluxo Completo para Testar

```bash
# 1. Health check
curl http://localhost:8080/health

# 2. Criar uma conversa com título
curl -X POST http://localhost:8080/api/conversations \
  -H "Content-Type: application/json" \
  -d '{"title":"Dúvidas de Tecnologia"}'

# 3. Enviar mensagem (cria conversa automaticamente se não existir)
curl -X POST http://localhost:8080/api/chat/send-v2 \
  -H "Content-Type: application/json" \
  -d '{"message":"O que é polimorfismo em Java?"}'

# 4. Listar conversas
curl http://localhost:8080/api/conversations

# 5. Ver histórico de mensagens
curl http://localhost:8080/api/chat/history/1

# 6. Renomear conversa
curl -X PUT http://localhost:8080/api/conversations/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"Estudando Java Avançado"}'

# 7. Adicionar conhecimento novo
curl -X POST http://localhost:8080/api/knowledge/add \
  -H "Content-Type: application/json" \
  -d '{"content":"Microsserviços são um estilo arquitetural onde a aplicação é composta por serviços pequenos e independentes...","source":"Microservicos"}'

# 8. Upload de documento
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@documento.pdf"

# 9. Verificar status do documento
curl http://localhost:8080/api/documents/1/status
```

---

## Swagger

A documentação interativa da API está disponível em:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON: `http://localhost:8080/api-docs`

---

## Tecnologias

- **Java 21**
- **Spring Boot 3.5**
- **Spring AI** (Ollama)
- **PostgreSQL + pgvector** (banco vetorial)
- **Flyway** (migrações)
- **MapStruct** (mapeamento DTO)
- **Lombok**
- **Apache PDFBox** (parse de PDF)
- **Apache POI** (parse de DOCX)
- **OpenAPI / Swagger UI**
- **JUnit 5 + Mockito**

---

## Estrutura do Projeto

```
src/main/java/br/com/chatbot/chatbot_api
 ├── config/              # Configurações (CORS, Swagger, Async)
 ├── controller/          # Endpoints REST
 │   ├── ChatController.java
 │   ├── ConversationController.java
 │   ├── DocumentController.java
 │   ├── FileController.java
 │   ├── HealthController.java
 │   └── KnowledgeController.java
 ├── dto/
 │   ├── request/         # DTOs de entrada
 │   └── response/        # DTOs de saída
 ├── entity/              # Entidades JPA
 ├── enums/               # Enumerações (MessageRole, DocumentStatus)
 ├── exception/           # Exceções + GlobalExceptionHandler
 ├── mapper/              # MapStruct mappers
 ├── repository/          # Spring Data JPA
 ├── service/             # Lógica de negócio
 │   ├── chat/            # Chat + BotService
 │   ├── chunking/        # TextChunker
 │   ├── embedding/       # EmbeddingService (nomic-embed-text)
 │   ├── impl/            # Implementações
 │   ├── integration/     # N8n webhook
 │   ├── parser/          # PDF, DOCX, TXT parsers
 │   └── rag/             # RAG Service + RagResult
 └── ChatbotApiApplication.java

src/main/resources/
 ├── knowledge/           # Base de conhecimento (16 arquivos .txt)
 ├── db/migration/        # Flyway migrations (V1 a V9)
 └── application.yml      # Configuração principal
```

# Chatbot API

Backend REST de um chatbot desenvolvido com **Java 21**, **Spring Boot 3** e **Clean Architecture**.

## Arquitetura

Clean Architecture com separação clara de responsabilidades:

```
src/main/java/br/com/chatbot/chatbot_api
 ├── config/         # Configurações (Swagger/OpenAPI)
 ├── controller/     # Endpoints REST (apenas recebem requisições HTTP)
 ├── dto/
 │   ├── request/    # DTOs de entrada
 │   └── response/   # DTOs de saída
 ├── entity/         # Entidades JPA
 ├── enums/          # Enumerações
 ├── exception/      # Exceções e GlobalExceptionHandler
 ├── mapper/         # Conversão Entity → DTO
 ├── repository/     # Acesso a dados (Spring Data JPA)
 ├── service/        # Interfaces de serviço
 │   └── impl/       # Implementações com lógica de negócio
 ├── validation/     # Validações customizadas
 └── util/           # Utilitários
```

## Princípios

- **SOLID**: Interfaces segregadas, responsabilidade única, inversão de dependência.
- **DTOs**: Entidades nunca são expostas nas respostas da API.
- **Spec-Driven**: Código orientado pela especificação.

## Endpoints

### Health

| Método | Rota       | Descrição              |
|--------|------------|------------------------|
| GET    | `/health`  | Status da aplicação    |

### Conversas

| Método | Rota                     | Descrição              |
|--------|--------------------------|------------------------|
| POST   | `/api/conversations`     | Criar conversa         |
| GET    | `/api/conversations`     | Listar conversas       |
| GET    | `/api/conversations/{id}`| Buscar conversa por ID |
| DELETE | `/api/conversations/{id}`| Excluir conversa       |

### Chat

| Método | Rota                            | Descrição                |
|--------|---------------------------------|--------------------------|
| POST   | `/api/chat/send`                | Enviar mensagem          |
| GET    | `/api/chat/history/{id}`        | Histórico da conversa    |

### Arquivos

| Método | Rota                     | Descrição                      |
|--------|--------------------------|--------------------------------|
| POST   | `/api/files/upload`      | Upload de TXT ou PDF           |

## Tecnologias

- Java 21
- Spring Boot 3.4
- Maven
- Spring Web
- Spring Data JPA
- Spring Validation
- Lombok
- H2 Database (em memória)
- Flyway (migrations)
- Swagger/OpenAPI (Springdoc)
- JUnit 5 + Mockito

## Banco de Dados

Banco H2 em memória.

- **Console H2**: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:chatbotdb`
  - User: `sa`
  - Password: *(vazio)*

### Migrations (Flyway)

As tabelas são criadas automaticamente via Flyway:

- `V1__create_tables.sql` — Cria as tabelas `conversation`, `message` e `attachment`.

## Swagger

Acessar: `http://localhost:8080/swagger-ui.html`

OpenAPI JSON: `http://localhost:8080/api-docs`

## Como Executar

```bash
# Com Maven Wrapper
./mvnw spring-boot:run

# Ou gerar JAR e executar
./mvnw clean package -DskipTests
java -jar target/chatbot-api-0.0.1-SNAPSHOT.jar
```

## Como Testar

```bash
./mvnw test
```

## Estrutura para n8n

A API REST pode ser consumida pelo n8n utilizando os nós de **HTTP Request**:

1. **Criar conversa**: `POST /api/conversations` com body `{"title": "..."}`
2. **Enviar mensagem**: `POST /api/chat/send` com body `{"conversationId": 1, "message": "..."}`
3. **Histórico**: `GET /api/chat/history/1`
4. **Upload**: `POST /api/files/upload` com `FormData` (conversationId + file)

Todos os endpoints retornam JSON padronizado e erros seguem o formato `ErrorResponse`.

# Debuging — Migrations Flyway (V1–V6)

## Erro Atual — V2 (`ERRO: relação "conversations" não existe`)

**Causa**: O arquivo `V2__create_messages.sql` cria uma FK (`fk_messages_conversation`) que referencia `conversations(id)`, mas a tabela `conversations` não existe no banco alvo.

**Possíveis causas raiz**:
1. **Flyway baselineado em banco diferente** — a migration `V1__create_conversations.sql` pode ter sido aplicada em outro banco/schema, e o `spring.flyway.baseline-on-migrate: true` fez o baseline ignorando o V1 no banco atual.
2. **`DB_NAME` apontando para banco errado** — a URL usa `${DB_NAME:postgres}`; se a variável de ambiente `DB_NAME` não estiver setada, Flyway roda contra o banco `postgres` (default do PostgreSQL), que pode não ter as migrations anteriores.
3. **Tabela `conversations` dropada manualmente** após a V1 já ter sido aplicada.
4. **Schema search path incorreto** — o schema configurado no datasource não é o mesmo onde a V1 foi executada.

**Solução sugerida**:
- Verificar em qual banco/schema as migrations estão sendo aplicadas:
  ```sql
  SELECT * FROM flyway_schema_history ORDER BY installed_rank;
  ```
- Confirmar o valor efetivo de `DB_NAME` / `DB_USERNAME` sendo usado.
- Se necessário, resetar o Flyway com `flyway repair` ou dropar a tabela `flyway_schema_history` e reaplicar do zero (apenas em dev).

---

## Mapeamento de possíveis erros — V2

| # | Problema | Local | Gravidade |
|---|----------|-------|-----------|
| 1 | `conversations` não existe | `REFERENCES conversations(id)` | 🔴 **Bloqueante** (erro atual) |
| 2 | `role VARCHAR(10)` pode ser curto para valores futuros | `role VARCHAR(10) NOT NULL` | 🟡 Valores atuais (`USER`, `BOT`) cabem; atentar se novos roles forem adicionados |
| 3 | Sem FK para `users` — não é possível rastrear quem enviou a mensagem | Mensagem não tem `user_id` | 🟡 GAP de modelo — se houver necessidade de auditoria por usuário, será preciso adicionar coluna e FK |

---

## Mapeamento de possíveis erros — V3

| # | Problema | Local | Gravidade |
|---|----------|-------|-----------|
| 1 | Mesma dependência da V2 — se `conversations` não existir, falha igual | `REFERENCES conversations(id)` | 🔴 Bloqueante se V1 não estiver no banco |
| 2 | Nenhuma FK para `messages` — attachment vinculado apenas à conversa, não à mensagem específica | Modelo não relaciona `attachments` com `messages` | 🟡 GAP de modelo — pode ser intencional, mas usualmente attachment pertence a uma mensagem |
| 3 | `file_size BIGINT` mapeado como `Long size` na entidade | `@Column(name = "file_size")` na entidade | ✅ OK — anotação explícita resolve a diferença de nome |

---

## Mapeamento de possíveis erros — V4

| # | Problema | Local | Gravidade |
|---|----------|-------|-----------|
| 1 | Nenhum — tabela autossuficiente, sem FKs | `V4__create_users.sql` | ✅ Deve executar sem erros |
| 2 | Nenhuma FK entre `users` e `conversations` — conversa não tem dono | `conversations` não tem `user_id` | 🟡 GAP de modelo — sem `user_id` na conversa, qualquer usuário pode acessar qualquer conversa |
| 3 | `email VARCHAR(100)` pode ser curto para emails muito longos | `email VARCHAR(100) NOT NULL UNIQUE` | 🟡 Raro, mas emails com mais de 100 chars existem; `RFC 5321` permite até 254 |

---

## Mapeamento de possíveis erros — V5

| # | Problema | Local | Gravidade |
|---|----------|-------|-----------|
| 1 | **`original_text TEXT NOT NULL` na migration, mas campo comentado na entidade Java** | `V5` cria coluna NOT NULL; `Document.java` linha 44-45 tem o campo `originalText` comentado | 🔴 **Crítico** — qualquer `save()` em `Document` via JPA gerará `INSERT` sem `original_text`, violando NOT NULL → `PSQLException` |
| 2 | Comentário no código: `// COMENTADO: evita OOM no servidor` | `Document.java:44` | 🟠 A decisão de comentar o campo precisa ser refletida na migration (tornar a coluna nullable ou remover a coluna) |

**Ação necessária na V5**: ou (a) remove `original_text` da migration, ou (b) altera para `TEXT` (nullable), ou (c) reativa o campo na entidade.

---

## Mapeamento de possíveis erros — V6

| # | Problema | Local | Gravidade |
|---|----------|-------|-----------|
| 1 | **pgvector extension não instalada no PostgreSQL** | `CREATE EXTENSION IF NOT EXISTS vector` | 🔴 **Bloqueante** — o banco precisa ter a extensão instalada (`CREATE EXTENSION vector` requer o pacote `pgvector` no servidor) |
| 2 | **`USING ivf` — índice vetorial depende do pgvector** | `USING ivf (embedding vector_cosine_ops)` | 🔴 Bloqueante — sem a extensão, o comando `CREATE INDEX` falha |
| 3 | **Dependência de V5** | `REFERENCES documents(id)` | 🔴 Bloqueante se V5 não executou |
| 4 | `vector_cosine_ops` pode não existir em versões antigas do pgvector | `vector_cosine_ops` | 🟡 Verificar versão mínima do pgvector (≥ 0.4.0) |
| 5 | `lists = 100` — tamanho da lista IVF inadequado para poucos registros | `WITH (lists = 100)` | 🟡 IVF com `lists = 100` é adequado para ~100k registros; para datasets menores, listas menores são mais eficientes |

---

## Status atual das correções

| Migration | Status | Observação |
|-----------|--------|------------|
| V1 | ✅ OK | Tabela `conversations` criada |
| V2 | ✅ OK | Tabela `messages` com FK para `conversations` |
| V3 | ✅ OK | Tabela `attachments` com FK para `conversations` |
| V4 | ✅ OK | Tabela `users` criada |
| V5 | ✅ **CORRIGIDO** | Coluna `original_text` removida (campo comentado na entity) |
| V6 | ❌ **PENDENTE** | Requer extensão `vector` (pgvector) no PostgreSQL |

## Resumo de dependências entre migrations

```
V1 → V2 → V3
(V1 → V3)
V4 (independente)
V5 → V6
```

- **V4** executa em qualquer ordem (sem FK).
- **V5** e **V6** são independentes de V1–V4, mas V6 depende de V5.
- Toda a cadeia V1→V2→V3 está quebrada se V1 não estiver no banco alvo.

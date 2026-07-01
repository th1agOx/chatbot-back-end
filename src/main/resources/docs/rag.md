# RAG — Retrieval-Augmented Generation

RAG é uma arquitetura que combina sistemas de busca (retrieval) com modelos de linguagem (generation) para responder perguntas com base em conhecimento específico.

## Arquitetura

O pipeline RAG possui três estágios principais:

### 1. Indexação (Offline)

Documentos são processados antes das consultas:
- **Parsing**: Extração de texto bruto de PDFs, DOCX, TXT, MD.
- **Chunking**: Divisão do texto em pedaços menores (chunks) de tamanho controlado (ex: 1000 caracteres com overlap de 200).
- **Embedding**: Cada chunk é convertido em um vetor numérico (embedding) por um modelo de linguagem especializado (ex: nomic-embed-text, 768 dimensões).
- **Armazenamento**: Os vetores são armazenados em um banco de dados vetorial (ex: PostgreSQL com extensão pgvector).

### 2. Recuperação (Online)

Quando o usuário faz uma pergunta:
- O texto da pergunta é convertido no mesmo modelo de embedding.
- Uma busca por similaridade cosseno ou distância euclidiana é feita no banco vetorial.
- Os chunks mais similares (top-K) são recuperados como contexto.

### 3. Geração (Online)

- O contexto recuperado é concatenado com a pergunta original em um prompt.
- O prompt é enviado para um LLM (ex: llama3.2:1b via Ollama).
- O LLM gera a resposta baseada APENAS no contexto fornecido.

## pgvector

Extensão do PostgreSQL que adiciona suporte a vetores e operações de相似idade. Suporta:
- Distância Euclidiana (L2): `<->`
- Produto Interno: `<#>`
- Similaridade Cosseno: `<=>`

Índice HNSW para busca aproximada em alta performance.

## Embedding vs Chat Model

- **Embedding Model** (nomic-embed-text): Converte texto em vetores. Leve e rápido.
- **Chat Model** (llama3.2:1b): Gera respostas em linguagem natural. Mais pesado.

## Chunking Estratégico

- Chunks muito pequenos perdem contexto semântico.
- Chunks muito grandes aumentam o custo do LLM e diluem informação relevante.
- Overlap entre chunks preserva contexto em fronteiras de divisão.
- Delimitadores naturais (parágrafos, frases) produzem chunks mais coerentes.

# AI Document Assistant (RAG) — V1 (MVP)

A Retrieval Augmented Generation service: upload PDFs, and ask questions answered
**only** from their contents using Claude, with source references. See
[`docs/specification.md`](docs/specification.md) and
[`docs/architecture.md`](docs/architecture.md) for requirements and design, and
[`docs/implementation-plan.md`](docs/implementation-plan.md) for the build increments.

> **Scope:** V1 is single-user. Authentication, authorization, and multi-user
> isolation are planned for V2 and are **not** implemented here.

## How it works

- **Ingestion:** PDF → text (PDFBox) → 800-token chunks with 100-token overlap →
  embeddings → pgvector. Runs asynchronously so the upload responds immediately.
- **Embeddings:** `all-MiniLM-L6-v2` (384-dim) run **locally in-process** via ONNX
  Runtime (Spring AI `TransformersEmbeddingModel`). No API key; the model downloads
  on first startup and then runs offline. Swappable via Spring AI's `EmbeddingModel`.
- **Query:** question embedded with the same local model → Top-5 similarity search →
  grounded prompt → **Claude (`claude-sonnet-5`)** generates the answer with citations.
- **Claude is used only for generation** (Anthropic has no embeddings API).

## Prerequisites

- JDK 21
- Docker (for the pgvector database)
- An Anthropic API key for the query endpoint (ingestion works without one)

## Running

1. Start PostgreSQL + pgvector:

   ```bash
   docker compose up -d
   ```

2. Provide your Anthropic API key (required only for `POST /api/query`):

   ```bash
   export ANTHROPIC_API_KEY=sk-ant-...
   ```

3. Run the app (first start downloads the embedding model, ~90 MB):

   ```bash
   ./gradlew bootRun
   ```

The API is served at `http://localhost:8080`.

## API

### Upload a PDF — `POST /api/documents`

Returns `202 Accepted` immediately; ingestion runs in the background.

```bash
curl -i -F "file=@yourfile.pdf;type=application/pdf" http://localhost:8080/api/documents
```

### Check ingestion status — `GET /api/documents/{id}` · list — `GET /api/documents`

```bash
curl http://localhost:8080/api/documents/<id>
```

Status moves `PENDING → PROCESSING → COMPLETED` (or `FAILED`, with `errorMessage` and
`retryCount`). `chunkCount` is set on completion.

### Ask a question — `POST /api/query`

```bash
curl -H "Content-Type: application/json" \
     -d '{"question":"What does the document say about X?"}' \
     http://localhost:8080/api/query
```

Response:

```json
{
  "answer": "…grounded answer citing filenames…",
  "sources": [
    { "documentId": "…", "filename": "yourfile.pdf", "chunkIndex": 0, "excerpt": "…", "score": 0.83 }
  ]
}
```

If nothing relevant is found (or the context is insufficient), the answer is
`"I don't know based on the provided documents."`.

## Configuration

Set in `src/main/resources/application.properties` (or override via env/CLI):

| Property | Default | Meaning |
|---|---|---|
| `rag.chunk-size` | `800` | Chunk size in tokens |
| `rag.chunk-overlap` | `100` | Overlap between chunks in tokens |
| `rag.top-k` | `5` | Chunks retrieved per query |
| `rag.generation-model` | `claude-sonnet-5` | Claude model for answers (configurable, not hardcoded) |
| `rag.answer-max-tokens` | `1024` | Max output tokens for an answer |
| `rag.max-ingestion-attempts` | `3` | Attempts before a document is marked `FAILED` |
| `spring.ai.vectorstore.pgvector.dimensions` | `384` | Must match the embedding model |

## Testing

```bash
./gradlew test
```

Unit tests cover the token chunker, prompt builder, ingestion pipeline (success /
no-text / retry-to-failure), the query service, and the web layer (validation, 404,
and error mapping). They require neither a database nor network access.

## Error responses

All errors return a consistent JSON body:

```json
{ "timestamp": "…", "status": 400, "error": "Bad Request", "message": "Only PDF files are supported", "path": "/api/documents" }
```

Common cases: `400` (non-PDF upload, blank question), `404` (unknown document id),
`413` (file too large), `503` (answer generation unavailable — e.g. missing API key).

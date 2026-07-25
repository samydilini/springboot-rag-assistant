# Implementation Plan

Scope: **Version 1 (MVP), single-user, no auth.** Authentication, authorization,
and multi-user isolation are **deferred entirely to V2** — V1 stores no owner/user
information on any row. Built in increments with a review gate between each.
Increment 1 (build + config) is complete.

---

## Increment 2 — Document ingestion

**Goal:** Accept a PDF upload, store its metadata, and asynchronously extract →
chunk → embed → store its content in pgvector, tracking status and handling
failures with retry. No querying yet (that is Increment 3).

Satisfies these spec requirements: *Allow users to upload PDF files · Store
document metadata · Extract text · Split into chunks (800/100) · Generate
embeddings · Store embeddings for retrieval · Upload API response < 5s · Failed
document processing should retry · Processing failures should be recorded.*

### 2.1 Embedding model execution (confirmed)

`all-MiniLM-L6-v2` runs **in-process via Microsoft ONNX Runtime** — not Ollama, no
external server. Spring AI's `TransformersEmbeddingModel` loads the model's ONNX export
and runs inference locally; tokenization is done by DJL's HuggingFace tokenizer. The
model + tokenizer are downloaded from HuggingFace on first boot and cached, then run
fully offline (produces 384-dim vectors, matching the pgvector `dimensions=384` set in
Increment 1).

These are already on the classpath transitively from `spring-ai-starter-model-transformers`
(added in Increment 1) — **no new embedding dependency is required**:

- `com.microsoft.onnxruntime:onnxruntime:1.19.2` — runs the model
- `ai.djl.huggingface:tokenizers:0.32.0` — tokenization
- `org.springframework.ai:spring-ai-transformers:1.0.1` — `TransformersEmbeddingModel`

Embedding stays behind Spring AI's `EmbeddingModel` interface, which remains the
provider swap seam for V2 (swapping to Ollama/Voyage/OpenAI is a dependency + config
change with no service-code change).

### 2.2 New dependency (only one)

- `org.springframework.ai:spring-ai-pdf-document-reader` — Spring AI's PDFBox-based
  reader (`PagePdfDocumentReader`). The transformers/pgvector starters do **not**
  bundle a PDF reader, so one must be added. PDF-focused (V1 is PDF-only) and stays
  within the Spring AI stack.
- Chunking needs no new dependency — **JTokkit 1.1.0** is already transitive via Spring
  AI and provides `cl100k_base` tokenization for the overlap splitter.

### 2.3 Two design decisions (with recommendations)

1. **Token overlap.** Spring AI's built-in `TokenTextSplitter` has **no overlap
   parameter**, but the spec mandates a 100-token overlap. **Recommendation:** a small
   custom `OverlappingTokenChunker` using JTokkit — tokenize with `cl100k_base`, emit
   windows of 800 tokens stepping by 700 (→ 100-token overlap), decode each window back
   to text. This honors the spec exactly.

2. **Retry mechanism.** **Recommendation:** a manual, bounded retry loop inside the
   async ingestion method (configurable max attempts, default 3), recording
   `retryCount` and `errorMessage`. Avoids mixing `@Async` and `@Retryable` AOP on the
   same bean and needs no new dependency.

### 2.4 Ingestion flow

```
POST /api/documents (multipart "file")
   │  validate (non-empty, PDF content-type/extension)
   │  read bytes into memory (≤ 25 MB per Increment 1 limits)
   │  persist Document{status=PENDING}
   │  trigger IngestionService.ingest(id, bytes, filename)  ← @Async (cross-bean)
   └► 202 Accepted  { id, filename, status }   (Location: /api/documents/{id})

IngestionService.ingest  (async, off the request thread → keeps upload < 5s)
   status → PROCESSING
   attempt loop (≤ maxAttempts):
     1. extract text   (PagePdfDocumentReader over a ByteArrayResource)
     2. chunk          (OverlappingTokenChunker, 800/100)
     3. wrap chunks as Spring AI Documents with metadata
            { document_id, filename, chunk_index }
     4. vectorStore.add(chunks)   ← embeds locally (ONNX) + stores
     on success → status=COMPLETED, chunkCount=N ; stop
     on failure → retryCount++, save errorMessage ; retry
   exhausted → status=FAILED (errorMessage retained)
```

Notes:
- Bytes are read into memory and passed to the async method (a `MultipartFile` is not
  valid on another thread). Within the 25 MB cap this is safe.
- Embedding is **implicit** in `vectorStore.add(...)` — `PgVectorStore` uses the
  `EmbeddingModel` bean. No separate embedding call is written for ingestion.
- Status transitions are each persisted immediately (`repository.save`) so `GET`
  reflects live progress.
- A PDF with no extractable text (e.g. scanned/needs OCR — out of scope until V2) is a
  **terminal, non-retried** failure with a clear message.

### 2.5 Files to add

```
config/
  AsyncConfig.java            @EnableAsync + a small bounded ThreadPoolTaskExecutor
document/
  DocumentController.java     POST /api/documents · GET /api/documents/{id} · GET /api/documents
  DocumentService.java        validate, persist metadata, trigger async ingestion
  IngestionService.java       @Async pipeline: extract → chunk → embed/store; retry; status
  ingestion/
    PdfTextExtractor.java     PagePdfDocumentReader(ByteArrayResource) → plain text
    OverlappingTokenChunker.java   JTokkit cl100k_base, 800/100 windows
    ChunkMetadata.java        metadata key constants (document_id, filename, chunk_index)
  model/
    Document.java             JPA entity (fields below)
    DocumentStatus.java       PENDING · PROCESSING · COMPLETED · FAILED
  DocumentRepository.java     JpaRepository<Document, UUID>
  dto/
    DocumentResponse.java     API view of a Document
```

`RagProperties` gains one field: `maxIngestionAttempts` (default 3), bound from
`rag.max-ingestion-attempts`.

No `owner_id` column, no `CurrentUserProvider` — deferred to V2.

### 2.6 `Document` entity fields

| Field         | Type            | Notes                                             |
|---------------|-----------------|---------------------------------------------------|
| `id`          | `UUID`          | generated; external-facing id                     |
| `filename`    | `String`        | original upload name                              |
| `contentType` | `String`        | e.g. `application/pdf`                             |
| `sizeBytes`   | `long`          |                                                   |
| `status`      | `DocumentStatus`| stored as STRING                                  |
| `chunkCount`  | `Integer`       | null until COMPLETED                              |
| `errorMessage`| `String` (TEXT) | null unless failed                                |
| `retryCount`  | `int`           | attempts consumed                                 |
| `createdAt`   | `Instant`       | `@CreationTimestamp`                              |
| `updatedAt`   | `Instant`       | `@UpdateTimestamp`                               |

Hibernate manages this table (`ddl-auto=update`, set in Increment 1). Chunks +
embeddings continue to live in Spring AI's `vector_store` table.

### 2.7 API contract

- `POST /api/documents` — `multipart/form-data`, part `file`.
  - `202 Accepted` + `DocumentResponse` (status `PENDING`), `Location` header.
  - `400` for empty/non-PDF input.
- `GET /api/documents/{id}` — `DocumentResponse` (200) or `404`.
- `GET /api/documents` — list all documents (convenience for testing/review).

A `GlobalExceptionHandler` is deferred to Increment 4 (polish) unless you want it now.

### 2.8 Verification

- `./gradlew compileJava` and a boot against the docker-compose pgvector.
- Manual: `curl -F file=@sample.pdf` → poll `GET /api/documents/{id}` until `COMPLETED`;
  confirm `chunkCount > 0` and rows present in the `vector_store` table.
- One focused unit test for `OverlappingTokenChunker` (verifies 800-token windows and the
  100-token overlap on a known input). Broader tests stay in Increment 4.

### 2.9 Explicitly out of scope for Increment 2

Querying/retrieval, Claude generation, source references (Increment 3); OCR for scanned
PDFs (V2); authentication, authorization, multi-user isolation (V2); a scheduled
re-drive of `FAILED` documents (optional, Increment 4).

---

## Later increments

- **Increment 3 — Query + generation:** retriever (Top-K=5), Claude answer generation
  with the three prompt rules (answer only from context; say "I don't know" when
  unsupported; include document references), `QueryResponse` with source references.
- **Increment 4 — Polish:** global exception handling, validation messages, README run
  notes, broader tests, optional `FAILED` re-drive sweep.

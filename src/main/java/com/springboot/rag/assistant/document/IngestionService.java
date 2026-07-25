package com.springboot.rag.assistant.document;

import com.springboot.rag.assistant.config.RagProperties;
import com.springboot.rag.assistant.document.ingestion.ChunkMetadata;
import com.springboot.rag.assistant.document.ingestion.OverlappingTokenChunker;
import com.springboot.rag.assistant.document.ingestion.PdfTextExtractor;
import com.springboot.rag.assistant.document.model.Document;
import com.springboot.rag.assistant.document.model.DocumentStatus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Runs the ingestion pipeline off the request thread: extract text → chunk →
 * embed + store, with a bounded manual retry loop. Persists each status
 * transition immediately so the document's status can be polled while it runs.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final DocumentRepository repository;
    private final PdfTextExtractor extractor;
    private final OverlappingTokenChunker chunker;
    private final VectorStore vectorStore;
    private final RagProperties properties;

    public IngestionService(
            DocumentRepository repository,
            PdfTextExtractor extractor,
            OverlappingTokenChunker chunker,
            VectorStore vectorStore,
            RagProperties properties) {
        this.repository = repository;
        this.extractor = extractor;
        this.chunker = chunker;
        this.vectorStore = vectorStore;
        this.properties = properties;
    }

    @Async("ingestionExecutor")
    public void ingest(UUID documentId, byte[] pdfBytes, String filename) {
        markStatus(documentId, DocumentStatus.PROCESSING);
        int maxAttempts = Math.max(1, properties.getMaxIngestionAttempts());
        String lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String text = extractor.extract(pdfBytes, filename);
                if (text == null || text.isBlank()) {
                    // Terminal, non-retryable: nothing to embed (e.g. a scanned PDF
                    // needing OCR, which is out of scope until V2).
                    fail(documentId, attempt - 1,
                            "No extractable text found (scanned PDFs require OCR, not supported in V1)");
                    return;
                }

                List<String> chunkTexts = chunker.chunk(text);
                if (chunkTexts.isEmpty()) {
                    fail(documentId, attempt - 1, "Document produced no chunks");
                    return;
                }

                vectorStore.add(toVectorDocuments(documentId, filename, chunkTexts));
                complete(documentId, chunkTexts.size());
                log.info("Ingested document {} ({} chunks)", documentId, chunkTexts.size());
                return;
            } catch (Exception e) {
                lastError = e.getMessage();
                log.warn("Ingestion attempt {}/{} failed for document {}: {}",
                        attempt, maxAttempts, documentId, lastError);
                recordAttempt(documentId, attempt, lastError);
            }
        }

        fail(documentId, maxAttempts, lastError != null ? lastError : "Ingestion failed");
        log.error("Ingestion failed for document {} after {} attempts", documentId, maxAttempts);
    }

    private List<org.springframework.ai.document.Document> toVectorDocuments(
            UUID documentId, String filename, List<String> chunkTexts) {
        List<org.springframework.ai.document.Document> docs = new ArrayList<>(chunkTexts.size());
        for (int i = 0; i < chunkTexts.size(); i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put(ChunkMetadata.DOCUMENT_ID, documentId.toString());
            metadata.put(ChunkMetadata.FILENAME, filename);
            metadata.put(ChunkMetadata.CHUNK_INDEX, i);
            docs.add(org.springframework.ai.document.Document.builder()
                    .text(chunkTexts.get(i))
                    .metadata(metadata)
                    .build());
        }
        return docs;
    }

    private void markStatus(UUID documentId, DocumentStatus status) {
        repository.findById(documentId).ifPresent(doc -> {
            doc.setStatus(status);
            repository.save(doc);
        });
    }

    private void recordAttempt(UUID documentId, int attempt, String error) {
        repository.findById(documentId).ifPresent(doc -> {
            doc.setRetryCount(attempt);
            doc.setErrorMessage(error);
            repository.save(doc);
        });
    }

    private void complete(UUID documentId, int chunkCount) {
        repository.findById(documentId).ifPresent(doc -> {
            doc.setStatus(DocumentStatus.COMPLETED);
            doc.setChunkCount(chunkCount);
            doc.setErrorMessage(null);
            repository.save(doc);
        });
    }

    private void fail(UUID documentId, int retryCount, String error) {
        repository.findById(documentId).ifPresent(doc -> {
            doc.setStatus(DocumentStatus.FAILED);
            doc.setRetryCount(retryCount);
            doc.setErrorMessage(error);
            repository.save(doc);
        });
    }
}

package com.springboot.rag.assistant.query;

import com.springboot.rag.assistant.config.RagProperties;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/**
 * Retrieves the most relevant chunks for a question via vector similarity search.
 *
 * <p>The query is embedded by the same local model used during ingestion —
 * {@code PgVectorStore} calls the configured {@code EmbeddingModel} internally — so
 * query-side and document-side vectors are always produced the same way.
 */
@Component
public class Retriever {

    private final VectorStore vectorStore;
    private final RagProperties properties;

    public Retriever(VectorStore vectorStore, RagProperties properties) {
        this.vectorStore = vectorStore;
        this.properties = properties;
    }

    public List<Document> retrieve(String question) {
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(properties.getTopK())
                .build();
        List<Document> results = vectorStore.similaritySearch(request);
        return results != null ? results : List.of();
    }
}

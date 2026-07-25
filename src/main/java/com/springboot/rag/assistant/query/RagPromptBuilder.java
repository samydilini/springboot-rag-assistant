package com.springboot.rag.assistant.query;

import com.springboot.rag.assistant.document.ingestion.ChunkMetadata;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

/**
 * Builds the system and user prompts for answer generation. Pure string
 * assembly (no I/O) so it is unit-testable without the API or a database.
 *
 * <p>The system prompt encodes the specification's three rules: answer only from
 * the provided context, say "I don't know" when the context is insufficient, and
 * cite the source documents.
 */
@Component
public class RagPromptBuilder {

    static final String NO_ANSWER = "I don't know based on the provided documents.";

    static final String SYSTEM_PROMPT = """
            You are a document assistant. Answer the user's question using ONLY the context \
            provided below. If the context does not contain enough information to answer, reply \
            exactly: "%s" Do not use any outside knowledge. When you answer, cite the source \
            documents you used by their filename.""".formatted(NO_ANSWER);

    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String userMessage(String question, List<Document> chunks) {
        StringBuilder sb = new StringBuilder("Context:\n");
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            Object filename = chunk.getMetadata().getOrDefault(ChunkMetadata.FILENAME, "unknown");
            Object chunkIndex = chunk.getMetadata().get(ChunkMetadata.CHUNK_INDEX);
            sb.append('[').append(i + 1).append("] (filename=").append(filename)
                    .append(", chunk=").append(chunkIndex).append(")\n");
            sb.append(chunk.getText()).append("\n\n");
        }
        sb.append("Question: ").append(question);
        return sb.toString();
    }
}

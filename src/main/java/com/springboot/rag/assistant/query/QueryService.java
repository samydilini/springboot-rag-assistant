package com.springboot.rag.assistant.query;

import com.springboot.rag.assistant.document.ingestion.ChunkMetadata;
import com.springboot.rag.assistant.query.dto.QueryResponse;
import com.springboot.rag.assistant.query.dto.SourceReference;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

/**
 * Orchestrates a query: retrieve relevant chunks, then either short-circuit (no
 * matches) or build a grounded prompt and generate an answer with Claude.
 */
@Service
public class QueryService {

    private static final int EXCERPT_LENGTH = 200;

    private final Retriever retriever;
    private final RagPromptBuilder promptBuilder;
    private final AnswerGenerator answerGenerator;

    public QueryService(Retriever retriever, RagPromptBuilder promptBuilder, AnswerGenerator answerGenerator) {
        this.retriever = retriever;
        this.promptBuilder = promptBuilder;
        this.answerGenerator = answerGenerator;
    }

    public QueryResponse answer(String question) {
        List<Document> chunks = retriever.retrieve(question);
        if (chunks.isEmpty()) {
            return new QueryResponse(RagPromptBuilder.NO_ANSWER, List.of());
        }

        String answer = answerGenerator.generate(
                promptBuilder.systemPrompt(),
                promptBuilder.userMessage(question, chunks));

        List<SourceReference> sources = chunks.stream().map(this::toSource).toList();
        return new QueryResponse(answer, sources);
    }

    private SourceReference toSource(Document chunk) {
        Object documentId = chunk.getMetadata().get(ChunkMetadata.DOCUMENT_ID);
        Object filename = chunk.getMetadata().get(ChunkMetadata.FILENAME);
        return new SourceReference(
                documentId != null ? documentId.toString() : null,
                filename != null ? filename.toString() : null,
                parseChunkIndex(chunk.getMetadata().get(ChunkMetadata.CHUNK_INDEX)),
                excerpt(chunk.getText()),
                chunk.getScore());
    }

    private Integer parseChunkIndex(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String excerpt(String text) {
        if (text == null) {
            return null;
        }
        String stripped = text.strip();
        return stripped.length() <= EXCERPT_LENGTH ? stripped : stripped.substring(0, EXCERPT_LENGTH) + "…";
    }
}

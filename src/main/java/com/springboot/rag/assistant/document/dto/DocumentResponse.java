package com.springboot.rag.assistant.document.dto;

import com.springboot.rag.assistant.document.model.Document;
import com.springboot.rag.assistant.document.model.DocumentStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * API view of a {@link Document}.
 */
public record DocumentResponse(
        UUID id,
        String filename,
        String contentType,
        long sizeBytes,
        DocumentStatus status,
        Integer chunkCount,
        String errorMessage,
        int retryCount,
        Instant createdAt,
        Instant updatedAt) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getFilename(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getStatus(),
                document.getChunkCount(),
                document.getErrorMessage(),
                document.getRetryCount(),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }
}

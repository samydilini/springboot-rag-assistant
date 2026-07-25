package com.springboot.rag.assistant.query.dto;

/**
 * A single retrieved chunk cited as a source for an answer.
 */
public record SourceReference(
        String documentId,
        String filename,
        Integer chunkIndex,
        String excerpt,
        Double score) {
}

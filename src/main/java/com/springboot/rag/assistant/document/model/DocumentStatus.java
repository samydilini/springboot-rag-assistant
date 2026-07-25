package com.springboot.rag.assistant.document.model;

/**
 * Lifecycle of a document as it moves through ingestion.
 */
public enum DocumentStatus {

    /** Uploaded and persisted; ingestion not yet started. */
    PENDING,

    /** Ingestion in progress (extraction / chunking / embedding). */
    PROCESSING,

    /** Ingestion finished successfully; chunks are stored and searchable. */
    COMPLETED,

    /** Ingestion failed after exhausting retries (see {@code errorMessage}). */
    FAILED
}

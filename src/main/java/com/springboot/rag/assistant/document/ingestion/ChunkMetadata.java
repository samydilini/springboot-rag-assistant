package com.springboot.rag.assistant.document.ingestion;

/**
 * Metadata keys stored on each chunk in the vector store. These link a stored
 * chunk back to its source {@code documents} row and are used to build source
 * references during query (Increment 3).
 */
public final class ChunkMetadata {

    /** UUID (as String) of the owning {@code Document}. */
    public static final String DOCUMENT_ID = "document_id";

    /** Original filename of the source document. */
    public static final String FILENAME = "filename";

    /** Zero-based index of the chunk within the document. */
    public static final String CHUNK_INDEX = "chunk_index";

    private ChunkMetadata() {
    }
}

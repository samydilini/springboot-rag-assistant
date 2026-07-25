package com.springboot.rag.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunable RAG parameters, bound from the {@code rag.*} properties.
 *
 * <p>Defaults match the specification: 800-token chunks, 100-token overlap,
 * Top-K of 5, and Claude Sonnet for answer generation.
 */
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    /** Target chunk size in tokens. */
    private int chunkSize = 800;

    /** Overlap between consecutive chunks in tokens. */
    private int chunkOverlap = 100;

    /** Number of chunks retrieved for each query. */
    private int topK = 5;

    /** Claude model used for answer generation. */
    private String generationModel = "claude-sonnet-5";

    /** Maximum attempts (initial + retries) for ingesting a document before it is marked FAILED. */
    private int maxIngestionAttempts = 3;

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(int chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public String getGenerationModel() {
        return generationModel;
    }

    public void setGenerationModel(String generationModel) {
        this.generationModel = generationModel;
    }

    public int getMaxIngestionAttempts() {
        return maxIngestionAttempts;
    }

    public void setMaxIngestionAttempts(int maxIngestionAttempts) {
        this.maxIngestionAttempts = maxIngestionAttempts;
    }
}

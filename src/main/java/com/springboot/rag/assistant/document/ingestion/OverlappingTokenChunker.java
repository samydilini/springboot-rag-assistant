package com.springboot.rag.assistant.document.ingestion;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import com.springboot.rag.assistant.config.RagProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Splits text into overlapping token windows.
 *
 * <p>Spring AI's built-in {@code TokenTextSplitter} has no overlap parameter, but
 * the specification requires an overlap (default 800-token chunks with 100-token
 * overlap). This chunker tokenizes with the {@code cl100k_base} encoding, emits
 * windows of {@code chunkSize} tokens stepping by {@code chunkSize - overlap}, and
 * decodes each window back to text.
 */
@Component
public class OverlappingTokenChunker {

    private static final EncodingRegistry REGISTRY = Encodings.newLazyEncodingRegistry();

    private final int chunkSize;
    private final int overlap;
    private final Encoding encoding;

    @Autowired
    public OverlappingTokenChunker(RagProperties properties) {
        this(properties.getChunkSize(), properties.getChunkOverlap());
    }

    /** Package-private constructor for deterministic unit testing with custom sizes. */
    OverlappingTokenChunker(int chunkSize, int overlap) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be > 0");
        }
        if (overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("overlap must be >= 0 and < chunkSize");
        }
        this.chunkSize = chunkSize;
        this.overlap = overlap;
        this.encoding = REGISTRY.getEncoding(EncodingType.CL100K_BASE);
    }

    /**
     * Splits the given text into overlapping chunks. Returns an empty list for
     * null/blank input.
     */
    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        IntArrayList tokens = encoding.encode(text);
        for (int[] bounds : windowBounds(tokens.size())) {
            IntArrayList window = new IntArrayList(bounds[1] - bounds[0]);
            for (int i = bounds[0]; i < bounds[1]; i++) {
                window.add(tokens.get(i));
            }
            String chunkText = encoding.decode(window).strip();
            if (!chunkText.isEmpty()) {
                chunks.add(chunkText);
            }
        }
        return chunks;
    }

    /**
     * Computes the [start, end) token-index windows for a document of
     * {@code totalTokens} tokens. Pure integer math — no tokenization — so the
     * overlap behavior is unit-testable independent of any text.
     */
    List<int[]> windowBounds(int totalTokens) {
        List<int[]> bounds = new ArrayList<>();
        if (totalTokens <= 0) {
            return bounds;
        }
        int step = chunkSize - overlap;
        for (int start = 0; start < totalTokens; start += step) {
            int end = Math.min(start + chunkSize, totalTokens);
            bounds.add(new int[] {start, end});
            if (end == totalTokens) {
                break;
            }
        }
        return bounds;
    }
}

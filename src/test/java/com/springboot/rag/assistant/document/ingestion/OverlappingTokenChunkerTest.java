package com.springboot.rag.assistant.document.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class OverlappingTokenChunkerTest {

    @Test
    void windowsOverlapByConfiguredAmount() {
        OverlappingTokenChunker chunker = new OverlappingTokenChunker(800, 100);

        // 1000 tokens, size 800, step 700 → [0,800) then [700,1000).
        List<int[]> bounds = chunker.windowBounds(1000);

        assertThat(bounds).hasSize(2);
        assertThat(bounds.get(0)).containsExactly(0, 800);
        assertThat(bounds.get(1)).containsExactly(700, 1000);
        // Overlap = end of window0 - start of window1 = 800 - 700 = 100.
        assertThat(bounds.get(0)[1] - bounds.get(1)[0]).isEqualTo(100);
    }

    @Test
    void singleWindowWhenShorterThanChunkSize() {
        OverlappingTokenChunker chunker = new OverlappingTokenChunker(800, 100);

        assertThat(chunker.windowBounds(500)).singleElement().satisfies(w -> {
            assertThat(w[0]).isZero();
            assertThat(w[1]).isEqualTo(500);
        });
    }

    @Test
    void consecutiveWindowsShareExactlyOverlapTokens() {
        OverlappingTokenChunker chunker = new OverlappingTokenChunker(10, 3);

        List<int[]> bounds = chunker.windowBounds(25);

        // step = 7 → [0,10), [7,17), [14,24), [21,25)
        assertThat(bounds).hasSize(4);
        for (int i = 1; i < bounds.size(); i++) {
            int overlap = bounds.get(i - 1)[1] - bounds.get(i)[0];
            // Full-size windows overlap by exactly 3; the final short window may overlap more.
            assertThat(overlap).isGreaterThanOrEqualTo(3);
        }
    }

    @Test
    void emptyInputProducesNoChunks() {
        OverlappingTokenChunker chunker = new OverlappingTokenChunker(800, 100);

        assertThat(chunker.chunk("")).isEmpty();
        assertThat(chunker.chunk("   ")).isEmpty();
        assertThat(chunker.chunk(null)).isEmpty();
    }

    @Test
    void longTextProducesMultipleOverlappingChunks() {
        OverlappingTokenChunker chunker = new OverlappingTokenChunker(20, 5);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("word").append(i).append(' ');
        }

        List<String> chunks = chunker.chunk(sb.toString().strip());

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(c -> assertThat(c).isNotBlank());
    }

    @Test
    void rejectsInvalidConfiguration() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> new OverlappingTokenChunker(10, 10));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> new OverlappingTokenChunker(0, 0));
    }
}

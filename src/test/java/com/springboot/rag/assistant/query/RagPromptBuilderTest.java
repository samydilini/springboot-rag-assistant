package com.springboot.rag.assistant.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.rag.assistant.document.ingestion.ChunkMetadata;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class RagPromptBuilderTest {

    private final RagPromptBuilder builder = new RagPromptBuilder();

    @Test
    void systemPromptEncodesTheThreeRules() {
        String system = builder.systemPrompt();

        assertThat(system)
                .contains("ONLY the context")                 // answer only from context
                .contains(RagPromptBuilder.NO_ANSWER)          // say "I don't know"
                .contains("cite the source documents");        // include references
    }

    @Test
    void userMessageFormatsNumberedContextAndQuestion() {
        Document c0 = Document.builder()
                .text("Alpha content")
                .metadata(Map.of(ChunkMetadata.FILENAME, "a.pdf", ChunkMetadata.CHUNK_INDEX, 0))
                .build();
        Document c1 = Document.builder()
                .text("Beta content")
                .metadata(Map.of(ChunkMetadata.FILENAME, "b.pdf", ChunkMetadata.CHUNK_INDEX, 3))
                .build();

        String user = builder.userMessage("What is alpha?", List.of(c0, c1));

        assertThat(user)
                .contains("[1] (filename=a.pdf, chunk=0)")
                .contains("Alpha content")
                .contains("[2] (filename=b.pdf, chunk=3)")
                .contains("Beta content")
                .contains("Question: What is alpha?");
    }

    @Test
    void userMessageFallsBackToUnknownFilename() {
        Document c0 = Document.builder()
                .text("No filename here")
                .metadata(Map.of(ChunkMetadata.CHUNK_INDEX, 0))
                .build();

        String user = builder.userMessage("q", List.of(c0));

        assertThat(user).contains("filename=unknown");
    }
}

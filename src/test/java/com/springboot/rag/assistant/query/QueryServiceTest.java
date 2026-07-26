package com.springboot.rag.assistant.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.springboot.rag.assistant.document.ingestion.ChunkMetadata;
import com.springboot.rag.assistant.query.dto.QueryResponse;
import com.springboot.rag.assistant.query.dto.SourceReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

    @Mock
    Retriever retriever;

    @Mock
    AnswerGenerator answerGenerator;

    QueryService service;

    @BeforeEach
    void setUp() {
        service = new QueryService(retriever, new RagPromptBuilder(), answerGenerator);
    }

    @Test
    void emptyRetrievalShortCircuitsWithoutCallingClaude() {
        when(retriever.retrieve("q")).thenReturn(List.of());

        QueryResponse response = service.answer("q");

        assertThat(response.answer()).isEqualTo(RagPromptBuilder.NO_ANSWER);
        assertThat(response.sources()).isEmpty();
        verifyNoInteractions(answerGenerator);
    }

    @Test
    void generatesAnswerAndMapsSources() {
        Document chunk = Document.builder()
                .text("Alpha content")
                .metadata(Map.of(
                        ChunkMetadata.DOCUMENT_ID, "doc-1",
                        ChunkMetadata.FILENAME, "a.pdf",
                        ChunkMetadata.CHUNK_INDEX, 0))
                .build();
        when(retriever.retrieve("q")).thenReturn(List.of(chunk));
        when(answerGenerator.generate(anyString(), anyString())).thenReturn("the answer");

        QueryResponse response = service.answer("q");

        assertThat(response.answer()).isEqualTo("the answer");
        assertThat(response.sources()).hasSize(1);
        SourceReference source = response.sources().get(0);
        assertThat(source.documentId()).isEqualTo("doc-1");
        assertThat(source.filename()).isEqualTo("a.pdf");
        assertThat(source.chunkIndex()).isEqualTo(0);
        assertThat(source.excerpt()).isEqualTo("Alpha content");
    }
}

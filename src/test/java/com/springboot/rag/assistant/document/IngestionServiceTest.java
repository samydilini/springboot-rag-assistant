package com.springboot.rag.assistant.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.springboot.rag.assistant.config.RagProperties;
import com.springboot.rag.assistant.document.ingestion.OverlappingTokenChunker;
import com.springboot.rag.assistant.document.ingestion.PdfTextExtractor;
import com.springboot.rag.assistant.document.model.Document;
import com.springboot.rag.assistant.document.model.DocumentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.vectorstore.VectorStore;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IngestionServiceTest {

    @Mock
    DocumentRepository repository;

    @Mock
    PdfTextExtractor extractor;

    @Mock
    OverlappingTokenChunker chunker;

    @Mock
    VectorStore vectorStore;

    @Mock
    RagProperties properties;

    @InjectMocks
    IngestionService service;

    private final UUID id = UUID.randomUUID();
    private Document doc;

    @BeforeEach
    void setUp() {
        doc = new Document("f.pdf", "application/pdf", 10);
        when(repository.findById(id)).thenReturn(Optional.of(doc));
        when(properties.getMaxIngestionAttempts()).thenReturn(3);
    }

    @Test
    void successStoresChunksAndMarksCompleted() {
        when(extractor.extract(any(), eq("f.pdf"))).thenReturn("some extracted text");
        when(chunker.chunk("some extracted text")).thenReturn(List.of("c0", "c1"));

        service.ingest(id, new byte[] {1, 2, 3}, "f.pdf");

        verify(vectorStore).add(anyList());
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.COMPLETED);
        assertThat(doc.getChunkCount()).isEqualTo(2);
        assertThat(doc.getErrorMessage()).isNull();
    }

    @Test
    void noExtractableTextFailsWithoutRetryOrStore() {
        when(extractor.extract(any(), any())).thenReturn("   ");

        service.ingest(id, new byte[] {1}, "f.pdf");

        verify(vectorStore, never()).add(anyList());
        verify(extractor, times(1)).extract(any(), any());
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(doc.getErrorMessage()).contains("No extractable text");
    }

    @Test
    void retriesThenMarksFailed() {
        when(extractor.extract(any(), any())).thenThrow(new RuntimeException("boom"));

        service.ingest(id, new byte[] {1}, "f.pdf");

        verify(extractor, times(3)).extract(any(), any());
        verify(vectorStore, never()).add(anyList());
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(doc.getRetryCount()).isEqualTo(3);
        assertThat(doc.getErrorMessage()).contains("boom");
    }
}

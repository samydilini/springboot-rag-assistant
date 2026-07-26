package com.springboot.rag.assistant.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.springboot.rag.assistant.document.model.Document;
import com.springboot.rag.assistant.document.model.DocumentStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    DocumentRepository repository;

    @Mock
    IngestionService ingestionService;

    @InjectMocks
    DocumentService service;

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(400));

        verifyNoInteractions(ingestionService);
    }

    @Test
    void rejectsNonPdf() {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(400));

        verifyNoInteractions(ingestionService);
    }

    @Test
    void acceptsPdfPersistsPendingAndTriggersIngestion() {
        byte[] content = "%PDF-1.4 ...".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", content);
        when(repository.save(any(Document.class))).thenAnswer(invocation -> {
            Document d = invocation.getArgument(0);
            ReflectionTestUtils.setField(d, "id", UUID.randomUUID());
            return d;
        });

        Document result = service.upload(file);

        assertThat(result.getStatus()).isEqualTo(DocumentStatus.PENDING);
        assertThat(result.getFilename()).isEqualTo("a.pdf");
        verify(ingestionService).ingest(eq(result.getId()), any(byte[].class), eq("a.pdf"));
    }
}

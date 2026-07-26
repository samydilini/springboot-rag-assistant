package com.springboot.rag.assistant.document;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.springboot.rag.assistant.document.model.Document;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    DocumentService documentService;

    @Test
    void uploadReturns202WithLocationAndBody() throws Exception {
        Document doc = new Document("a.pdf", "application/pdf", 3);
        UUID id = UUID.randomUUID();
        ReflectionTestUtils.setField(doc, "id", id);
        when(documentService.upload(any())).thenReturn(doc);

        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "abc".getBytes());

        mvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/api/documents/" + id))
                .andExpect(jsonPath("$.filename").value("a.pdf"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getUnknownDocumentReturns404Json() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentService.get(id))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + id));

        mvc.perform(get("/api/documents/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Document not found: " + id))
                .andExpect(jsonPath("$.path").value("/api/documents/" + id));
    }
}

package com.springboot.rag.assistant.document;

import com.springboot.rag.assistant.document.dto.DocumentResponse;
import com.springboot.rag.assistant.document.model.Document;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Uploads a PDF. Returns 202 Accepted immediately; ingestion runs
     * asynchronously — poll {@code GET /api/documents/{id}} for status.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(@RequestParam("file") MultipartFile file) {
        Document document = documentService.upload(file);
        URI location = URI.create("/api/documents/" + document.getId());
        return ResponseEntity.accepted().location(location).body(DocumentResponse.from(document));
    }

    @GetMapping("/{id}")
    public DocumentResponse get(@PathVariable UUID id) {
        return DocumentResponse.from(documentService.get(id));
    }

    @GetMapping
    public List<DocumentResponse> list() {
        return documentService.list().stream().map(DocumentResponse::from).toList();
    }
}

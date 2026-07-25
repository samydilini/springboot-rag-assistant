package com.springboot.rag.assistant.document;

import com.springboot.rag.assistant.document.model.Document;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles the synchronous part of an upload: validate the file, persist its
 * metadata, and hand the bytes to {@link IngestionService} for asynchronous
 * processing. Returns quickly so the upload response stays well under 5s.
 */
@Service
public class DocumentService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final DocumentRepository repository;
    private final IngestionService ingestionService;

    public DocumentService(DocumentRepository repository, IngestionService ingestionService) {
        this.repository = repository;
        this.ingestionService = ingestionService;
    }

    public Document upload(MultipartFile file) {
        validate(file);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read uploaded file", e);
        }

        Document document = new Document(
                file.getOriginalFilename(),
                file.getContentType() != null ? file.getContentType() : PDF_CONTENT_TYPE,
                file.getSize());
        document = repository.save(document);

        // Cross-bean call so the @Async proxy applies; the row is already committed.
        ingestionService.ingest(document.getId(), bytes, document.getFilename());
        return document;
    }

    public Document get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + id));
    }

    public List<Document> list() {
        return repository.findAll();
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        String name = file.getOriginalFilename();
        String contentType = file.getContentType();
        boolean pdfByName = name != null && name.toLowerCase().endsWith(".pdf");
        boolean pdfByType = contentType != null && contentType.equalsIgnoreCase(PDF_CONTENT_TYPE);
        if (!pdfByName && !pdfByType) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF files are supported");
        }
    }
}

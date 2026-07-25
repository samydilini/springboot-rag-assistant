package com.springboot.rag.assistant.document;

import com.springboot.rag.assistant.document.model.Document;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
}

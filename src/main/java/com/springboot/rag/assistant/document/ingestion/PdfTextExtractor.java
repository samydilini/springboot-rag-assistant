package com.springboot.rag.assistant.document.ingestion;

import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

/**
 * Extracts plain text from a PDF using Spring AI's PDFBox-based
 * {@link PagePdfDocumentReader}. Page texts are concatenated into a single string
 * for downstream chunking.
 */
@Component
public class PdfTextExtractor {

    public String extract(byte[] pdfBytes, String filename) {
        ByteArrayResource resource = new ByteArrayResource(pdfBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
        return reader.get().stream()
                .map(org.springframework.ai.document.Document::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
    }
}

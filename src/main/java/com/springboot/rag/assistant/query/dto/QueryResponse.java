package com.springboot.rag.assistant.query.dto;

import java.util.List;

public record QueryResponse(String answer, List<SourceReference> sources) {
}

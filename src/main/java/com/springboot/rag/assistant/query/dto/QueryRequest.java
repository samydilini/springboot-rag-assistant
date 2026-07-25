package com.springboot.rag.assistant.query.dto;

import jakarta.validation.constraints.NotBlank;

public record QueryRequest(@NotBlank String question) {
}

package com.tsy.oa.intelligence.ai.model;

public record AiPrompt(String requestType, String businessReferenceId, String content) {

    public AiPrompt {
        requireText(requestType, "requestType");
        requireText(businessReferenceId, "businessReferenceId");
        requireText(content, "content");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}

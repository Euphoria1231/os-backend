package com.tsy.oa.intelligence.ai.service;

public record AiAnalysisRequest(String requestType, String businessReferenceId, long initiatorEmployeeId, String prompt) {

    public AiAnalysisRequest(String requestType, String businessReferenceId, String prompt) {
        this(requestType, businessReferenceId, 0L, prompt);
    }

    public AiAnalysisRequest {
        requireText(requestType, "requestType");
        requireText(businessReferenceId, "businessReferenceId");
        requireText(prompt, "prompt");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}

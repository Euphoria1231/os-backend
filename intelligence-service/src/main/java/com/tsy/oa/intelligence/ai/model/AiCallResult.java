package com.tsy.oa.intelligence.ai.model;

import java.util.Objects;

public record AiCallResult(AiCallStatus status, String displayText) {

    private static final String REFERENCE_ONLY_PREFIX = "仅供参考：";

    public AiCallResult {
        Objects.requireNonNull(status, "status must not be null");
        displayText = referenceOnly(displayText);
    }

    private static String referenceOnly(String value) {
        String safeValue = value == null || value.isBlank() ? "AI analysis is unavailable." : value.trim();
        return safeValue.startsWith(REFERENCE_ONLY_PREFIX) ? safeValue : REFERENCE_ONLY_PREFIX + safeValue;
    }
}

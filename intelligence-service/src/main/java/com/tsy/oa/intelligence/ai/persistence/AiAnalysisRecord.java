package com.tsy.oa.intelligence.ai.persistence;

import java.time.LocalDateTime;

public record AiAnalysisRecord(
        String requestType,
        String businessReferenceId,
        String status,
        long durationMs,
        String resultSummary,
        LocalDateTime auditedAt
) {
}

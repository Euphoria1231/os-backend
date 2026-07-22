package com.tsy.oa.intelligence.search.dto;

import java.time.LocalDateTime;

public record ApplicationSearchResponse(
        long applicationId,
        long applicantId,
        String type,
        String status,
        String reasonSummary,
        String reasonHighlight,
        LocalDateTime submittedAt,
        LocalDateTime updatedAt
) {
}

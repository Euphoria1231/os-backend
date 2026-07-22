package com.tsy.oa.intelligence.search.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ApplicationSearchDocument(
        long applicationId,
        long applicantId,
        String type,
        String status,
        String reasonSummary,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime submittedAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime updatedAt
) {
}

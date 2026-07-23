package com.tsy.oa.intelligence.search.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public record ApplicationSearchDocument(
        long applicationId,
        long applicantId,
        long approverId,
        List<Long> approverIds,
        String type,
        String status,
        String reasonSummary,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime submittedAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime updatedAt,
        long sourceVersion
) {

    public ApplicationSearchDocument(
            long applicationId,
            long applicantId,
            long approverId,
            String type,
            String status,
            String reasonSummary,
            LocalDateTime submittedAt,
            LocalDateTime updatedAt
    ) {
        this(
                applicationId, applicantId, approverId, List.of(approverId), type, status,
                reasonSummary, submittedAt, updatedAt, 1L
        );
    }
}

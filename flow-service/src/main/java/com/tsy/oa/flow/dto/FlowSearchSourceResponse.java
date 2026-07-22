package com.tsy.oa.flow.dto;

import com.tsy.oa.flow.model.FlowApplication;

import java.time.LocalDateTime;

public record FlowSearchSourceResponse(
        Long id,
        String applicationNo,
        Long applicantId,
        String applicationType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String reason,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FlowSearchSourceResponse from(FlowApplication application) {
        return new FlowSearchSourceResponse(
                application.getId(),
                application.getApplicationNo(),
                application.getApplicantId(),
                application.getApplicationType(),
                application.getStartTime(),
                application.getEndTime(),
                application.getReason(),
                application.getStatus(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}

package com.tsy.oa.flow.dto;

import com.tsy.oa.flow.model.FlowApplication;

import java.time.LocalDateTime;

public record FlowApplicationResponse(
        Long id,
        String applicationNo,
        Long applicantId,
        Long approverId,
        String applicationType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String reason,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FlowApplicationResponse from(FlowApplication application) {
        return new FlowApplicationResponse(
                application.getId(), application.getApplicationNo(), application.getApplicantId(),
                application.getApproverId(), application.getApplicationType(), application.getStartTime(),
                application.getEndTime(), application.getReason(), application.getStatus(),
                application.getCreatedAt(), application.getUpdatedAt()
        );
    }
}

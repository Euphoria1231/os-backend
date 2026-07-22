package com.tsy.oa.flow.dto;

import com.tsy.oa.flow.model.FlowApplication;

import java.time.LocalDateTime;

public record ApplicationSearchSourceResponse(
        Long id,
        Long applicantId,
        Long approverId,
        String applicationType,
        String status,
        String reason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ApplicationSearchSourceResponse from(FlowApplication application) {
        return new ApplicationSearchSourceResponse(
                application.getId(), application.getApplicantId(), application.getApproverId(),
                application.getApplicationType(), application.getStatus(), application.getReason(),
                application.getCreatedAt(), application.getUpdatedAt()
        );
    }
}

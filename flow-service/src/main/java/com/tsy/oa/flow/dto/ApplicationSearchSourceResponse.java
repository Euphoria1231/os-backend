package com.tsy.oa.flow.dto;

import com.tsy.oa.flow.model.FlowApplication;

import java.time.LocalDateTime;
import java.util.List;

public record ApplicationSearchSourceResponse(
        Long id,
        Long applicantId,
        Long approverId,
        List<Long> approverIds,
        String applicationType,
        String status,
        String reason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long searchVersion
) {
    public static ApplicationSearchSourceResponse from(
            FlowApplication application,
            List<Long> approverIds
    ) {
        return new ApplicationSearchSourceResponse(
                application.getId(), application.getApplicantId(), application.getApproverId(),
                List.copyOf(approverIds),
                application.getApplicationType(), application.getStatus(), application.getReason(),
                application.getCreatedAt(), application.getUpdatedAt(), application.getSearchVersion()
        );
    }
}

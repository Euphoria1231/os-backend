package com.tsy.oa.flow.dto;

import com.tsy.oa.flow.model.ApprovalTaskRecord;

import java.time.LocalDateTime;

public record ApprovalTaskResponse(
        Long applicationId,
        String applicationNo,
        Long applicantId,
        String applicationType,
        String action,
        String comment,
        LocalDateTime processedAt
) {
    public static ApprovalTaskResponse from(ApprovalTaskRecord record) {
        return new ApprovalTaskResponse(
                record.getApplicationId(), record.getApplicationNo(), record.getApplicantId(),
                record.getApplicationType(), record.getAction(), record.getComment(), record.getProcessedAt()
        );
    }
}

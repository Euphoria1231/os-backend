package com.tsy.oa.flow.dto;

import com.tsy.oa.flow.model.ApprovalTaskRecord;

import java.time.LocalDateTime;

public record ApprovalTaskResponse(
        Long taskId,
        Long applicationId,
        String applicationNo,
        Long applicantId,
        String applicationType,
        Integer approvalLevel,
        Long approverId,
        String approverName,
        String status,
        String action,
        String comment,
        LocalDateTime activatedAt,
        LocalDateTime processedAt
) {
    public static ApprovalTaskResponse from(ApprovalTaskRecord record) {
        return new ApprovalTaskResponse(
                record.getId(), record.getApplicationId(), record.getApplicationNo(),
                record.getApplicantId(), record.getApplicationType(), record.getApprovalLevel(),
                record.getApproverId(), record.getApproverName(), record.getStatus(),
                record.getAction(), record.getComment(), record.getActivatedAt(), record.getProcessedAt()
        );
    }
}

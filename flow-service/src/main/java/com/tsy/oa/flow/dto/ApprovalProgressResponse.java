package com.tsy.oa.flow.dto;

import com.tsy.oa.flow.model.ApprovalTaskRecord;

import java.time.LocalDateTime;

public record ApprovalProgressResponse(
        Long taskId,
        Integer approvalLevel,
        Long approverId,
        String approverName,
        String status,
        String action,
        String comment,
        LocalDateTime activatedAt,
        LocalDateTime processedAt
) {
    public static ApprovalProgressResponse from(ApprovalTaskRecord task) {
        return new ApprovalProgressResponse(
                task.getId(), task.getApprovalLevel(), task.getApproverId(),
                task.getApproverName(), task.getStatus(), task.getAction(), task.getComment(),
                task.getActivatedAt(), task.getProcessedAt()
        );
    }
}

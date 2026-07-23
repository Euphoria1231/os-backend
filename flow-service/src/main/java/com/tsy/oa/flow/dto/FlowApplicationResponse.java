package com.tsy.oa.flow.dto;

import com.tsy.oa.flow.model.FlowApplication;

import java.time.LocalDateTime;
import java.util.List;

public record FlowApplicationResponse(
        Long id,
        String applicationNo,
        Long applicantId,
        Long approverId,
        String applicationType,
        Long attendanceRecordId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String reason,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ApprovalProgressResponse> approvalProgress
) {
    public static FlowApplicationResponse from(
            FlowApplication application,
            List<ApprovalProgressResponse> approvalProgress
    ) {
        return new FlowApplicationResponse(
                application.getId(), application.getApplicationNo(), application.getApplicantId(),
                application.getApproverId(), application.getApplicationType(),
                application.getAttendanceRecordId(), application.getStartTime(),
                application.getEndTime(), application.getReason(), application.getStatus(),
                application.getCreatedAt(), application.getUpdatedAt(), approvalProgress
        );
    }
}

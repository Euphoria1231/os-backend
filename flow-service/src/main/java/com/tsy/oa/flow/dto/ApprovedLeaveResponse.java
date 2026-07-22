package com.tsy.oa.flow.dto;

import com.tsy.oa.flow.model.FlowApplication;

import java.time.LocalDate;

public record ApprovedLeaveResponse(
        Long employeeId,
        LocalDate startDate,
        LocalDate endDate,
        String status
) {
    public static ApprovedLeaveResponse from(FlowApplication application) {
        return new ApprovedLeaveResponse(
                application.getApplicantId(),
                application.getStartTime().toLocalDate(),
                application.getEndTime().toLocalDate(),
                application.getStatus()
        );
    }
}

package com.tsy.oa.attendance.dto;

import jakarta.validation.constraints.NotNull;

public record MakeupCompletionRequest(
        @NotNull(message = "员工ID不能为空") Long employeeId,
        @NotNull(message = "补签申请ID不能为空") Long applicationId
) {
}

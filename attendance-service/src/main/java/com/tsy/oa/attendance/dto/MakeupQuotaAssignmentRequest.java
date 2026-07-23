package com.tsy.oa.attendance.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.YearMonth;

public record MakeupQuotaAssignmentRequest(
        @NotNull(message = "补签额度月份不能为空") YearMonth quotaMonth,
        @Min(value = 1, message = "补签总次数必须大于0") int totalCount
) {
}

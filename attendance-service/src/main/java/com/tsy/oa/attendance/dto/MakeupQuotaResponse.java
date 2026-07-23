package com.tsy.oa.attendance.dto;

import com.tsy.oa.attendance.model.AttendanceMakeupQuota;

import java.time.YearMonth;

public record MakeupQuotaResponse(
        Long employeeId,
        YearMonth quotaMonth,
        int totalCount,
        int usedCount,
        int remainingCount,
        Long assignedBy
) {
    public static MakeupQuotaResponse from(AttendanceMakeupQuota quota) {
        return new MakeupQuotaResponse(
                quota.getEmployeeId(), YearMonth.from(quota.getQuotaMonth()),
                quota.getTotalCount(), quota.getUsedCount(),
                quota.getTotalCount() - quota.getUsedCount(), quota.getAssignedBy()
        );
    }
}

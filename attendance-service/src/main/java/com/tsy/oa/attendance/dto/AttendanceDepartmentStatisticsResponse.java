package com.tsy.oa.attendance.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record AttendanceDepartmentStatisticsResponse(
        Long departmentId,
        YearMonth month,
        int employeeCount,
        int expectedAttendanceDays,
        int actualAttendanceDays,
        int lateCount,
        int earlyLeaveCount,
        int absenceCount,
        int leaveDays,
        BigDecimal totalWorkHours
) {
}

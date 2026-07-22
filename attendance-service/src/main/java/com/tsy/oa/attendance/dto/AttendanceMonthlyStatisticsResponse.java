package com.tsy.oa.attendance.dto;

import com.tsy.oa.attendance.model.AttendanceMonthlySummary;

import java.math.BigDecimal;
import java.time.YearMonth;

public record AttendanceMonthlyStatisticsResponse(
        Long employeeId,
        YearMonth month,
        int expectedAttendanceDays,
        int actualAttendanceDays,
        int lateCount,
        int earlyLeaveCount,
        int absenceCount,
        int leaveDays,
        BigDecimal totalWorkHours
) {

    public static AttendanceMonthlyStatisticsResponse from(AttendanceMonthlySummary summary) {
        return new AttendanceMonthlyStatisticsResponse(
                summary.getEmployeeId(),
                YearMonth.from(summary.getSummaryMonth()),
                summary.getExpectedAttendanceDays(),
                summary.getActualAttendanceDays(),
                summary.getLateCount(),
                summary.getEarlyLeaveCount(),
                summary.getAbsenceCount(),
                summary.getLeaveDays(),
                summary.getTotalWorkHours()
        );
    }
}

package com.tsy.oa.attendance.dashboard.dto;

import java.time.LocalDate;

public record AttendanceDailyTrendResponse(
        LocalDate date,
        Long totalCount,
        Long normalCount,
        Long lateCount,
        Long earlyLeaveCount,
        Long absentCount
) {
}

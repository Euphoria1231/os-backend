package com.tsy.oa.intelligence.dashboard;

import java.time.LocalDate;

public record AttendanceDailyTrendResponse(
        LocalDate date,
        long totalCount,
        long normalCount,
        long lateCount,
        long earlyLeaveCount,
        long absentCount
) {
}

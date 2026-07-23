package com.tsy.oa.attendance.dashboard.dto;

import java.util.List;

public record AttendanceStatisticsResponse(
        String month,
        long normalCount,
        long lateCount,
        long earlyLeaveCount,
        long absentCount,
        List<AttendanceDailyTrendResponse> dailyTrend
) {
    public AttendanceStatisticsResponse {
        dailyTrend = List.copyOf(dailyTrend);
    }
}

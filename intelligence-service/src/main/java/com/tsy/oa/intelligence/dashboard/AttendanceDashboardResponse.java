package com.tsy.oa.intelligence.dashboard;

import java.util.List;

public record AttendanceDashboardResponse(
        String month,
        long normalCount,
        long lateCount,
        long earlyLeaveCount,
        long absentCount,
        List<AttendanceDailyTrendResponse> dailyTrend
) {
    public AttendanceDashboardResponse {
        dailyTrend = List.copyOf(dailyTrend);
    }
}

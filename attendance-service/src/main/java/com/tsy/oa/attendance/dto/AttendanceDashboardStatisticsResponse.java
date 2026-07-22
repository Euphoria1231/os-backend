package com.tsy.oa.attendance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record AttendanceDashboardStatisticsResponse(
        YearMonth month,
        List<DepartmentAverageWorkHours> departmentAverageWorkHours,
        List<DailyAbnormalTrend> dailyAbnormalTrends,
        List<ClockInHeatmapPoint> clockInHeatmap
) {

    public AttendanceDashboardStatisticsResponse {
        departmentAverageWorkHours = List.copyOf(departmentAverageWorkHours);
        dailyAbnormalTrends = List.copyOf(dailyAbnormalTrends);
        clockInHeatmap = List.copyOf(clockInHeatmap);
    }

    public record DepartmentAverageWorkHours(
            Long departmentId,
            int employeeCount,
            BigDecimal averageWorkHours
    ) {
    }

    public record DailyAbnormalTrend(
            LocalDate workDate,
            int lateCount,
            int earlyLeaveCount,
            int absenceCount
    ) {
    }

    public record ClockInHeatmapPoint(
            LocalDate workDate,
            int hour,
            int clockInCount
    ) {
    }
}

package com.tsy.oa.intelligence.dashboard;

import com.tsy.oa.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "attendance-service",
        contextId = "attendanceDashboardClient",
        path = "/internal/attendance/dashboard"
)
public interface AttendanceDashboardClient {

    @GetMapping("/statistics")
    ApiResponse<AttendanceStatisticsResponse> statistics(@RequestParam String month);

    record AttendanceStatisticsResponse(
            String month,
            long normalCount,
            long lateCount,
            long earlyLeaveCount,
            long absentCount,
            List<AttendanceDailyTrendResponse> dailyTrend
    ) {
    }
}

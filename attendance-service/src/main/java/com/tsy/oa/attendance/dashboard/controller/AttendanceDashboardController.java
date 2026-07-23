package com.tsy.oa.attendance.dashboard.controller;

import com.tsy.oa.attendance.dashboard.dto.AttendanceStatisticsResponse;
import com.tsy.oa.attendance.dashboard.service.AttendanceDashboardService;
import com.tsy.oa.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/attendance/dashboard")
public class AttendanceDashboardController {

    private final AttendanceDashboardService dashboardService;

    public AttendanceDashboardController(AttendanceDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/statistics")
    public ApiResponse<AttendanceStatisticsResponse> statistics(@RequestParam String month) {
        return ApiResponse.success(dashboardService.getStatistics(month));
    }
}

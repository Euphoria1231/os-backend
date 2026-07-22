package com.tsy.oa.attendance.controller;

import com.tsy.oa.attendance.dto.AttendanceDashboardStatisticsResponse;
import com.tsy.oa.attendance.dto.AttendanceDepartmentStatisticsResponse;
import com.tsy.oa.attendance.dto.AttendanceMonthlyStatisticsResponse;
import com.tsy.oa.attendance.service.AttendanceDashboardStatisticsService;
import com.tsy.oa.attendance.service.AttendanceMonthlyStatisticsService;
import com.tsy.oa.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/attendance/statistics")
public class AttendanceStatisticsController {

    private static final String EMPLOYEE_HEADER = "X-Employee-Id";

    private final AttendanceMonthlyStatisticsService statisticsService;
    private final AttendanceDashboardStatisticsService dashboardService;

    public AttendanceStatisticsController(
            AttendanceMonthlyStatisticsService statisticsService,
            AttendanceDashboardStatisticsService dashboardService
    ) {
        this.statisticsService = statisticsService;
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "查询员工月度考勤统计")
    @GetMapping("/monthly")
    public ApiResponse<AttendanceMonthlyStatisticsResponse> getMonthlyStatistics(
            @Parameter(hidden = true) @RequestHeader(EMPLOYEE_HEADER) Long operatorEmployeeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam Long employeeId
    ) {
        return ApiResponse.success(
                statisticsService.getEmployeeStatistics(operatorEmployeeId, month, employeeId)
        );
    }

    @Operation(summary = "查询部门月度考勤统计")
    @GetMapping("/departments")
    public ApiResponse<AttendanceDepartmentStatisticsResponse> getDepartmentStatistics(
            @Parameter(hidden = true) @RequestHeader(EMPLOYEE_HEADER) Long operatorEmployeeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam Long departmentId
    ) {
        return ApiResponse.success(
                statisticsService.getDepartmentStatistics(operatorEmployeeId, month, departmentId)
        );
    }

    @Operation(summary = "查询考勤大屏统计")
    @GetMapping("/dashboard")
    public ApiResponse<AttendanceDashboardStatisticsResponse> getDashboardStatistics(
            @Parameter(hidden = true) @RequestHeader(EMPLOYEE_HEADER) Long operatorEmployeeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        return ApiResponse.success(dashboardService.getDashboard(operatorEmployeeId, month));
    }
}

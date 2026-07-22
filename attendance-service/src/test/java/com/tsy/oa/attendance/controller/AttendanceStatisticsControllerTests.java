package com.tsy.oa.attendance.controller;

import com.tsy.oa.attendance.AttendanceServiceApplication;
import com.tsy.oa.attendance.dto.AttendanceDashboardStatisticsResponse;
import com.tsy.oa.attendance.dto.AttendanceDepartmentStatisticsResponse;
import com.tsy.oa.attendance.dto.AttendanceMonthlyStatisticsResponse;
import com.tsy.oa.attendance.service.AttendanceDashboardStatisticsService;
import com.tsy.oa.attendance.service.AttendanceMonthlyStatisticsService;
import com.tsy.oa.attendance.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendanceStatisticsController.class)
@ContextConfiguration(classes = AttendanceServiceApplication.class)
@Import(GlobalExceptionHandler.class)
class AttendanceStatisticsControllerTests {

    private static final String EMPLOYEE_HEADER = "X-Employee-Id";
    private static final YearMonth MONTH = YearMonth.of(2026, 7);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttendanceMonthlyStatisticsService statisticsService;

    @MockitoBean
    private AttendanceDashboardStatisticsService dashboardService;

    @Test
    void returnsEmployeeMonthlyStatistics() throws Exception {
        when(statisticsService.getEmployeeStatistics(10L, MONTH, 10L)).thenReturn(
                new AttendanceMonthlyStatisticsResponse(
                        10L, MONTH, 23, 20, 2, 1, 1, 1, new BigDecimal("160.00")
                )
        );

        mockMvc.perform(get("/api/attendance/statistics/monthly")
                        .header(EMPLOYEE_HEADER, "10")
                        .param("month", "2026-07")
                        .param("employeeId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.employeeId").value(10))
                .andExpect(jsonPath("$.data.month").value("2026-07"))
                .andExpect(jsonPath("$.data.totalWorkHours").value(160.00));
    }

    @Test
    void returnsDepartmentMonthlyStatistics() throws Exception {
        when(statisticsService.getDepartmentStatistics(2L, MONTH, 2L)).thenReturn(
                new AttendanceDepartmentStatisticsResponse(
                        2L, MONTH, 3, 69, 58, 3, 2, 2, 1, new BigDecimal("460.50")
                )
        );

        mockMvc.perform(get("/api/attendance/statistics/departments")
                        .header(EMPLOYEE_HEADER, "2")
                        .param("month", "2026-07")
                        .param("departmentId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.departmentId").value(2))
                .andExpect(jsonPath("$.data.employeeCount").value(3));
    }

    @Test
    void rejectsInvalidMonthParameter() throws Exception {
        mockMvc.perform(get("/api/attendance/statistics/monthly")
                        .header(EMPLOYEE_HEADER, "10")
                        .param("month", "2026-13")
                        .param("employeeId", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        verifyNoInteractions(statisticsService);
    }

    @Test
    void returnsDashboardStatistics() throws Exception {
        when(dashboardService.getDashboard(1L, MONTH)).thenReturn(
                new AttendanceDashboardStatisticsResponse(
                        MONTH,
                        List.of(new AttendanceDashboardStatisticsResponse.DepartmentAverageWorkHours(
                                2L, 3, new BigDecimal("152.50")
                        )),
                        List.of(new AttendanceDashboardStatisticsResponse.DailyAbnormalTrend(
                                LocalDate.of(2026, 7, 1), 2, 1, 0
                        )),
                        List.of(new AttendanceDashboardStatisticsResponse.ClockInHeatmapPoint(
                                LocalDate.of(2026, 7, 1), 9, 3
                        ))
                )
        );

        mockMvc.perform(get("/api/attendance/statistics/dashboard")
                        .header(EMPLOYEE_HEADER, "1")
                        .param("month", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.month").value("2026-07"))
                .andExpect(jsonPath("$.data.departmentAverageWorkHours[0].departmentId").value(2))
                .andExpect(jsonPath("$.data.dailyAbnormalTrends[0].lateCount").value(2))
                .andExpect(jsonPath("$.data.clockInHeatmap[0].clockInCount").value(3));
    }

    @Test
    void rejectsInvalidDashboardMonth() throws Exception {
        mockMvc.perform(get("/api/attendance/statistics/dashboard")
                        .header(EMPLOYEE_HEADER, "1")
                        .param("month", "2026-13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        verifyNoInteractions(dashboardService);
    }
}

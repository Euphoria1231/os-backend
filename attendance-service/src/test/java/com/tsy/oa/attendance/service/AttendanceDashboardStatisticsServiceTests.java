package com.tsy.oa.attendance.service;

import com.tsy.oa.attendance.AttendanceServiceApplication;
import com.tsy.oa.attendance.calculation.UserAttendanceClient;
import com.tsy.oa.attendance.dto.AttendanceDashboardStatisticsResponse;
import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AttendanceServiceApplication.class)
class AttendanceDashboardStatisticsServiceTests {

    private static final YearMonth MONTH = YearMonth.of(2026, 7);

    @Autowired
    private AttendanceDashboardStatisticsService dashboardService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private UserAttendanceClient userAttendanceClient;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM attendance_daily_summary");
    }

    @Test
    void aggregatesDepartmentAverageHoursDailyAbnormalTrendAndClockInHeatmap() {
        mockAuthorization(1L, "SUPER_ADMIN");
        when(userAttendanceClient.findEmployees()).thenReturn(ApiResponse.success(List.of(
                new UserAttendanceClient.EmployeeSummary(10L, 2L, 1),
                new UserAttendanceClient.EmployeeSummary(11L, 2L, 1),
                new UserAttendanceClient.EmployeeSummary(12L, 3L, 1),
                new UserAttendanceClient.EmployeeSummary(13L, 2L, 0)
        )));
        insertDailySummary(10L, LocalDate.of(2026, 7, 1), 9, "LATE", "8.00");
        insertDailySummary(10L, LocalDate.of(2026, 7, 2), null, "ABSENT", "0.00");
        insertDailySummary(11L, LocalDate.of(2026, 7, 1), 9, "EARLY", "8.50");
        insertDailySummary(12L, LocalDate.of(2026, 7, 1), 8, "NORMAL", "6.00");
        insertDailySummary(13L, LocalDate.of(2026, 7, 1), 7, "NORMAL", "20.00");
        insertDailySummary(10L, LocalDate.of(2026, 6, 30), 9, "LATE", "8.00");

        AttendanceDashboardStatisticsResponse response = dashboardService.getDashboard(1L, MONTH);

        assertThat(response.month()).isEqualTo(MONTH);
        assertThat(response.departmentAverageWorkHours()).containsExactly(
                new AttendanceDashboardStatisticsResponse.DepartmentAverageWorkHours(
                        2L, 2, new BigDecimal("8.25")
                ),
                new AttendanceDashboardStatisticsResponse.DepartmentAverageWorkHours(
                        3L, 1, new BigDecimal("6.00")
                )
        );
        assertThat(response.dailyAbnormalTrends()).containsExactly(
                new AttendanceDashboardStatisticsResponse.DailyAbnormalTrend(
                        LocalDate.of(2026, 7, 1), 1, 1, 0
                ),
                new AttendanceDashboardStatisticsResponse.DailyAbnormalTrend(
                        LocalDate.of(2026, 7, 2), 0, 0, 1
                )
        );
        assertThat(response.clockInHeatmap()).containsExactly(
                new AttendanceDashboardStatisticsResponse.ClockInHeatmapPoint(
                        LocalDate.of(2026, 7, 1), 8, 1
                ),
                new AttendanceDashboardStatisticsResponse.ClockInHeatmapPoint(
                        LocalDate.of(2026, 7, 1), 9, 2
                )
        );
    }

    @Test
    void returnsEmptyMetricsWhenThereAreNoActiveEmployees() {
        mockAuthorization(1L, "SUPER_ADMIN");
        when(userAttendanceClient.findEmployees()).thenReturn(ApiResponse.success(List.of()));

        AttendanceDashboardStatisticsResponse response = dashboardService.getDashboard(1L, MONTH);

        assertThat(response.departmentAverageWorkHours()).isEmpty();
        assertThat(response.dailyAbnormalTrends()).isEmpty();
        assertThat(response.clockInHeatmap()).isEmpty();
    }

    @Test
    void rejectsOrdinaryEmployeeAccessingCompanyDashboard() {
        mockAuthorization(10L, "EMPLOYEE");

        assertThatThrownBy(() -> dashboardService.getDashboard(10L, MONTH))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode.code")
                .isEqualTo(40300);
    }

    private void mockAuthorization(Long employeeId, String roleCode) {
        when(userAttendanceClient.findAuthorization(employeeId)).thenReturn(ApiResponse.success(
                new UserAttendanceClient.AuthorizationSummary(
                        List.of(new UserAttendanceClient.RoleSummary(roleCode, 1)),
                        List.of()
                )
        ));
    }

    private void insertDailySummary(
            Long employeeId,
            LocalDate workDate,
            Integer clockInHour,
            String status,
            String workHours
    ) {
        LocalDateTime clockInTime = clockInHour == null
                ? null
                : workDate.atTime(clockInHour, 0);
        jdbcTemplate.update("""
                INSERT INTO attendance_daily_summary (
                    employee_id, work_date, clock_in_time, work_hours,
                    status, calculation_version, calculated_at
                ) VALUES (?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP)
                """, employeeId, workDate, clockInTime, new BigDecimal(workHours), status);
    }
}

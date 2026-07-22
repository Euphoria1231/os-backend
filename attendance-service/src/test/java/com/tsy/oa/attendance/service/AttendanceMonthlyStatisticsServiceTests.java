package com.tsy.oa.attendance.service;

import com.tsy.oa.attendance.AttendanceServiceApplication;
import com.tsy.oa.attendance.calculation.UserAttendanceClient;
import com.tsy.oa.attendance.calculation.UserAttendanceDepartmentClient;
import com.tsy.oa.attendance.dto.AttendanceDepartmentStatisticsResponse;
import com.tsy.oa.attendance.dto.AttendanceMonthlyStatisticsResponse;
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
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AttendanceServiceApplication.class)
class AttendanceMonthlyStatisticsServiceTests {

    private static final YearMonth MONTH = YearMonth.of(2026, 7);

    @Autowired
    private AttendanceMonthlyStatisticsService statisticsService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private UserAttendanceClient userAttendanceClient;

    @MockitoBean
    private UserAttendanceDepartmentClient departmentClient;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM attendance_monthly_summary");
        jdbcTemplate.update("DELETE FROM attendance_daily_summary");
    }

    @Test
    void calculatesMonthlyMetricsAndKeepsOneSummaryAfterRecalculation() {
        insertDailySummary(10L, LocalDate.of(2026, 7, 1), "NORMAL", "8.00");
        insertDailySummary(10L, LocalDate.of(2026, 7, 2), "LATE", "8.00");
        insertDailySummary(10L, LocalDate.of(2026, 7, 3), "EARLY", "7.00");
        insertDailySummary(10L, LocalDate.of(2026, 7, 6), "ABSENT", "0.00");
        insertDailySummary(10L, LocalDate.of(2026, 7, 7), "LEAVE", "0.00");

        AttendanceMonthlyStatisticsResponse first = statisticsService.calculateMonthly(10L, MONTH);
        AttendanceMonthlyStatisticsResponse second = statisticsService.calculateMonthly(10L, MONTH);

        Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendance_monthly_summary WHERE employee_id = ? AND summary_month = ?",
                Integer.class,
                10L,
                MONTH.atDay(1)
        );
        assertThat(rowCount).isEqualTo(1);
        assertThat(second).isEqualTo(first);
        assertThat(first.expectedAttendanceDays()).isEqualTo(23);
        assertThat(first.actualAttendanceDays()).isEqualTo(3);
        assertThat(first.lateCount()).isEqualTo(1);
        assertThat(first.earlyLeaveCount()).isEqualTo(1);
        assertThat(first.absenceCount()).isEqualTo(1);
        assertThat(first.leaveDays()).isEqualTo(1);
        assertThat(first.totalWorkHours()).isEqualByComparingTo("23.00");
    }

    @Test
    void departmentLeaderOnlyAggregatesEmployeesInManagedDepartment() {
        mockAuthorization(2L, "EMPLOYEE");
        when(departmentClient.findDepartment(2L)).thenReturn(ApiResponse.success(
                new UserAttendanceDepartmentClient.DepartmentSummary(2L, 2L, 1)
        ));
        when(userAttendanceClient.findEmployees()).thenReturn(ApiResponse.success(List.of(
                new UserAttendanceClient.EmployeeSummary(10L, 2L, 1),
                new UserAttendanceClient.EmployeeSummary(11L, 2L, 1),
                new UserAttendanceClient.EmployeeSummary(12L, 3L, 1),
                new UserAttendanceClient.EmployeeSummary(13L, 2L, 0)
        )));
        insertDailySummary(10L, LocalDate.of(2026, 7, 1), "NORMAL", "8.00");
        insertDailySummary(11L, LocalDate.of(2026, 7, 1), "LATE", "7.50");

        AttendanceDepartmentStatisticsResponse response =
                statisticsService.getDepartmentStatistics(2L, MONTH, 2L);

        assertThat(response.departmentId()).isEqualTo(2L);
        assertThat(response.employeeCount()).isEqualTo(2);
        assertThat(response.expectedAttendanceDays()).isEqualTo(46);
        assertThat(response.actualAttendanceDays()).isEqualTo(2);
        assertThat(response.lateCount()).isEqualTo(1);
        assertThat(response.totalWorkHours()).isEqualByComparingTo("15.50");
    }

    @Test
    void rejectsOrdinaryEmployeeQueryingAnotherEmployee() {
        mockAuthorization(10L, "EMPLOYEE");
        when(userAttendanceClient.findEmployees()).thenReturn(ApiResponse.success(List.of(
                new UserAttendanceClient.EmployeeSummary(11L, 3L, 1)
        )));
        when(departmentClient.findDepartment(3L)).thenReturn(ApiResponse.success(
                new UserAttendanceDepartmentClient.DepartmentSummary(3L, 20L, 1)
        ));

        assertThatThrownBy(() -> statisticsService.getEmployeeStatistics(10L, MONTH, 11L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode.code")
                .isEqualTo(40300);
    }

    @Test
    void allowsOrdinaryEmployeeToQuerySelf() {
        AttendanceMonthlyStatisticsResponse response =
                statisticsService.getEmployeeStatistics(10L, MONTH, 10L);

        assertThat(response.employeeId()).isEqualTo(10L);
        assertThat(response.month()).isEqualTo(MONTH);
    }

    @Test
    void rejectsDepartmentLeaderQueryingUnmanagedDepartment() {
        mockAuthorization(2L, "EMPLOYEE");
        when(departmentClient.findDepartment(3L)).thenReturn(ApiResponse.success(
                new UserAttendanceDepartmentClient.DepartmentSummary(3L, 20L, 1)
        ));

        assertThatThrownBy(() -> statisticsService.getDepartmentStatistics(2L, MONTH, 3L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode.code")
                .isEqualTo(40300);
    }

    @Test
    void allowsSuperAdministratorToQueryAnyEmployee() {
        mockAuthorization(1L, "SUPER_ADMIN");

        AttendanceMonthlyStatisticsResponse response =
                statisticsService.getEmployeeStatistics(1L, MONTH, 11L);

        assertThat(response.employeeId()).isEqualTo(11L);
        assertThat(response.month()).isEqualTo(MONTH);
    }

    @Test
    void allowsSuperAdministratorToQueryAnyDepartment() {
        mockAuthorization(1L, "SUPER_ADMIN");
        when(userAttendanceClient.findEmployees()).thenReturn(ApiResponse.success(List.of()));

        AttendanceDepartmentStatisticsResponse response =
                statisticsService.getDepartmentStatistics(1L, MONTH, 9L);

        assertThat(response.departmentId()).isEqualTo(9L);
        assertThat(response.employeeCount()).isZero();
    }

    private void mockAuthorization(Long employeeId, String roleCode) {
        when(userAttendanceClient.findAuthorization(employeeId)).thenReturn(ApiResponse.success(
                new UserAttendanceClient.AuthorizationSummary(
                        List.of(new UserAttendanceClient.RoleSummary(roleCode, 1)),
                        List.of()
                )
        ));
    }

    private void insertDailySummary(Long employeeId, LocalDate date, String status, String workHours) {
        jdbcTemplate.update("""
                INSERT INTO attendance_daily_summary (
                    employee_id, work_date, work_hours, status, calculation_version, calculated_at
                ) VALUES (?, ?, ?, ?, 1, CURRENT_TIMESTAMP)
                """, employeeId, date, new BigDecimal(workHours), status);
    }
}

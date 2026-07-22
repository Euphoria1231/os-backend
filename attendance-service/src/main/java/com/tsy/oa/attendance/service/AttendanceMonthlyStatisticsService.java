package com.tsy.oa.attendance.service;

import com.tsy.oa.attendance.calculation.UserAttendanceClient;
import com.tsy.oa.attendance.calculation.UserAttendanceDepartmentClient;
import com.tsy.oa.attendance.calculation.UserAttendanceWorkforceProvider;
import com.tsy.oa.attendance.dto.AttendanceDepartmentStatisticsResponse;
import com.tsy.oa.attendance.dto.AttendanceMonthlyStatisticsResponse;
import com.tsy.oa.attendance.mapper.AttendanceDailySummaryMapper;
import com.tsy.oa.attendance.mapper.AttendanceMonthlySummaryMapper;
import com.tsy.oa.attendance.model.AttendanceDailySummary;
import com.tsy.oa.attendance.model.AttendanceMonthlySummary;
import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
public class AttendanceMonthlyStatisticsService {

    private static final int ENABLED = 1;
    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final AttendanceDailySummaryMapper dailySummaryMapper;
    private final AttendanceMonthlySummaryMapper monthlySummaryMapper;
    private final UserAttendanceClient userClient;
    private final UserAttendanceDepartmentClient departmentClient;
    private final UserAttendanceWorkforceProvider workforceProvider;
    private final Clock clock;

    public AttendanceMonthlyStatisticsService(
            AttendanceDailySummaryMapper dailySummaryMapper,
            AttendanceMonthlySummaryMapper monthlySummaryMapper,
            UserAttendanceClient userClient,
            UserAttendanceDepartmentClient departmentClient,
            UserAttendanceWorkforceProvider workforceProvider,
            Clock clock
    ) {
        this.dailySummaryMapper = dailySummaryMapper;
        this.monthlySummaryMapper = monthlySummaryMapper;
        this.userClient = userClient;
        this.departmentClient = departmentClient;
        this.workforceProvider = workforceProvider;
        this.clock = clock;
    }

    @Transactional
    public AttendanceMonthlyStatisticsResponse calculateMonthly(Long employeeId, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        List<AttendanceDailySummary> dailySummaries = dailySummaryMapper.findByEmployeeAndDateRange(
                employeeId,
                startDate,
                month.atEndOfMonth()
        );
        AttendanceMonthlySummary summary = summarize(employeeId, month, dailySummaries);
        monthlySummaryMapper.upsert(summary);
        return AttendanceMonthlyStatisticsResponse.from(
                monthlySummaryMapper.findByEmployeeAndMonth(employeeId, startDate)
        );
    }

    @Transactional
    public AttendanceMonthlyStatisticsResponse getEmployeeStatistics(
            Long operatorEmployeeId,
            YearMonth month,
            Long employeeId
    ) {
        requireEmployeeAccess(operatorEmployeeId, employeeId);
        return calculateMonthly(employeeId, month);
    }

    @Transactional
    public AttendanceDepartmentStatisticsResponse getDepartmentStatistics(
            Long operatorEmployeeId,
            YearMonth month,
            Long departmentId
    ) {
        requireDepartmentAccess(operatorEmployeeId, departmentId);
        List<AttendanceMonthlyStatisticsResponse> employeeSummaries = workforceProvider
                .findActiveEmployeeIdsByDepartment(departmentId)
                .stream()
                .map(employeeId -> calculateMonthly(employeeId, month))
                .toList();
        return aggregateDepartment(departmentId, month, employeeSummaries);
    }

    private AttendanceMonthlySummary summarize(
            Long employeeId,
            YearMonth month,
            List<AttendanceDailySummary> dailySummaries
    ) {
        int actualAttendanceDays = 0;
        int lateCount = 0;
        int earlyLeaveCount = 0;
        int absenceCount = 0;
        int leaveDays = 0;
        BigDecimal totalWorkHours = BigDecimal.ZERO;
        for (AttendanceDailySummary daily : dailySummaries) {
            totalWorkHours = totalWorkHours.add(daily.getWorkHours());
            switch (daily.getStatus()) {
                case "NORMAL" -> actualAttendanceDays++;
                case "LATE" -> {
                    actualAttendanceDays++;
                    lateCount++;
                }
                case "EARLY", "EARLY_LEAVE" -> {
                    actualAttendanceDays++;
                    earlyLeaveCount++;
                }
                case "ABSENT" -> absenceCount++;
                case "LEAVE" -> leaveDays++;
                default -> {
                }
            }
        }

        AttendanceMonthlySummary summary = new AttendanceMonthlySummary();
        summary.setEmployeeId(employeeId);
        summary.setSummaryMonth(month.atDay(1));
        summary.setExpectedAttendanceDays(countWeekdays(month));
        summary.setActualAttendanceDays(actualAttendanceDays);
        summary.setLateCount(lateCount);
        summary.setEarlyLeaveCount(earlyLeaveCount);
        summary.setAbsenceCount(absenceCount);
        summary.setLeaveDays(leaveDays);
        summary.setTotalWorkHours(totalWorkHours);
        summary.setCalculatedAt(LocalDateTime.now(clock));
        return summary;
    }

    private int countWeekdays(YearMonth month) {
        int weekdays = 0;
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            DayOfWeek dayOfWeek = month.atDay(day).getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                weekdays++;
            }
        }
        return weekdays;
    }

    private AttendanceDepartmentStatisticsResponse aggregateDepartment(
            Long departmentId,
            YearMonth month,
            List<AttendanceMonthlyStatisticsResponse> summaries
    ) {
        return new AttendanceDepartmentStatisticsResponse(
                departmentId,
                month,
                summaries.size(),
                summaries.stream().mapToInt(AttendanceMonthlyStatisticsResponse::expectedAttendanceDays).sum(),
                summaries.stream().mapToInt(AttendanceMonthlyStatisticsResponse::actualAttendanceDays).sum(),
                summaries.stream().mapToInt(AttendanceMonthlyStatisticsResponse::lateCount).sum(),
                summaries.stream().mapToInt(AttendanceMonthlyStatisticsResponse::earlyLeaveCount).sum(),
                summaries.stream().mapToInt(AttendanceMonthlyStatisticsResponse::absenceCount).sum(),
                summaries.stream().mapToInt(AttendanceMonthlyStatisticsResponse::leaveDays).sum(),
                summaries.stream()
                        .map(AttendanceMonthlyStatisticsResponse::totalWorkHours)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }

    private void requireEmployeeAccess(Long operatorEmployeeId, Long employeeId) {
        if (operatorEmployeeId.equals(employeeId) || isSuperAdministrator(operatorEmployeeId)) {
            return;
        }
        UserAttendanceClient.EmployeeSummary employee = requireData(userClient.findEmployees()).stream()
                .filter(candidate -> employeeId.equals(candidate.id()) && ENABLED == candidate.status())
                .findFirst()
                .orElseThrow(() -> new BusinessException(CommonErrorCode.FORBIDDEN));
        requireManagedDepartment(operatorEmployeeId, employee.departmentId());
    }

    private void requireDepartmentAccess(Long operatorEmployeeId, Long departmentId) {
        if (isSuperAdministrator(operatorEmployeeId)) {
            return;
        }
        requireManagedDepartment(operatorEmployeeId, departmentId);
    }

    private boolean isSuperAdministrator(Long employeeId) {
        UserAttendanceClient.AuthorizationSummary authorization =
                requireData(userClient.findAuthorization(employeeId));
        return safeList(authorization.roles()).stream().anyMatch(role ->
                ENABLED == role.status() && SUPER_ADMIN.equals(role.code())
        );
    }

    private void requireManagedDepartment(Long operatorEmployeeId, Long departmentId) {
        if (departmentId == null) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        UserAttendanceDepartmentClient.DepartmentSummary department =
                requireData(departmentClient.findDepartment(departmentId));
        if (ENABLED != department.status() || !operatorEmployeeId.equals(department.leaderEmployeeId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    private <T> T requireData(ApiResponse<T> response) {
        if (response == null || response.code() != 0 || response.data() == null) {
            throw new IllegalStateException("用户服务返回错误");
        }
        return response.data();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}

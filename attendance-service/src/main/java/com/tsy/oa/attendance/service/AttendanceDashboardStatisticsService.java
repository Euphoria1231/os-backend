package com.tsy.oa.attendance.service;

import com.tsy.oa.attendance.calculation.UserAttendanceClient;
import com.tsy.oa.attendance.dto.AttendanceDashboardStatisticsResponse;
import com.tsy.oa.attendance.mapper.AttendanceDailySummaryMapper;
import com.tsy.oa.attendance.model.AttendanceDailySummary;
import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AttendanceDashboardStatisticsService {

    private static final int ENABLED = 1;
    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final AttendanceDailySummaryMapper dailySummaryMapper;
    private final UserAttendanceClient userClient;

    public AttendanceDashboardStatisticsService(
            AttendanceDailySummaryMapper dailySummaryMapper,
            UserAttendanceClient userClient
    ) {
        this.dailySummaryMapper = dailySummaryMapper;
        this.userClient = userClient;
    }

    public AttendanceDashboardStatisticsResponse getDashboard(
            Long operatorEmployeeId,
            YearMonth month
    ) {
        requireSuperAdministrator(operatorEmployeeId);
        Map<Long, UserAttendanceClient.EmployeeSummary> activeEmployees = requireData(
                userClient.findEmployees()
        ).stream()
                .filter(employee -> employee.id() != null)
                .filter(employee -> employee.departmentId() != null)
                .filter(employee -> ENABLED == employee.status())
                .collect(Collectors.toMap(
                        UserAttendanceClient.EmployeeSummary::id,
                        Function.identity(),
                        (first, ignored) -> first
                ));
        if (activeEmployees.isEmpty()) {
            return new AttendanceDashboardStatisticsResponse(
                    month, List.of(), List.of(), List.of()
            );
        }

        List<AttendanceDailySummary> summaries = dailySummaryMapper.findByDateRange(
                month.atDay(1),
                month.atEndOfMonth()
        ).stream()
                .filter(summary -> activeEmployees.containsKey(summary.getEmployeeId()))
                .toList();

        return new AttendanceDashboardStatisticsResponse(
                month,
                calculateDepartmentAverages(activeEmployees, summaries),
                calculateDailyAbnormalTrends(summaries),
                calculateClockInHeatmap(summaries)
        );
    }

    private List<AttendanceDashboardStatisticsResponse.DepartmentAverageWorkHours>
    calculateDepartmentAverages(
            Map<Long, UserAttendanceClient.EmployeeSummary> activeEmployees,
            List<AttendanceDailySummary> summaries
    ) {
        Map<Long, Integer> employeeCounts = activeEmployees.values().stream()
                .collect(Collectors.groupingBy(
                        UserAttendanceClient.EmployeeSummary::departmentId,
                        Collectors.summingInt(employee -> 1)
                ));
        Map<Long, BigDecimal> totalHours = new HashMap<>();
        for (AttendanceDailySummary summary : summaries) {
            Long departmentId = activeEmployees.get(summary.getEmployeeId()).departmentId();
            totalHours.merge(departmentId, summary.getWorkHours(), BigDecimal::add);
        }
        return employeeCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new AttendanceDashboardStatisticsResponse.DepartmentAverageWorkHours(
                        entry.getKey(),
                        entry.getValue(),
                        totalHours.getOrDefault(entry.getKey(), BigDecimal.ZERO)
                                .divide(BigDecimal.valueOf(entry.getValue()), 2, RoundingMode.HALF_UP)
                ))
                .toList();
    }

    private List<AttendanceDashboardStatisticsResponse.DailyAbnormalTrend>
    calculateDailyAbnormalTrends(List<AttendanceDailySummary> summaries) {
        Map<LocalDate, int[]> dailyCounts = new TreeMap<>();
        for (AttendanceDailySummary summary : summaries) {
            int index = switch (summary.getStatus()) {
                case "LATE" -> 0;
                case "EARLY", "EARLY_LEAVE" -> 1;
                case "ABSENT" -> 2;
                default -> -1;
            };
            if (index >= 0) {
                dailyCounts.computeIfAbsent(summary.getWorkDate(), ignored -> new int[3])[index]++;
            }
        }
        return dailyCounts.entrySet().stream()
                .map(entry -> new AttendanceDashboardStatisticsResponse.DailyAbnormalTrend(
                        entry.getKey(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]
                ))
                .toList();
    }

    private List<AttendanceDashboardStatisticsResponse.ClockInHeatmapPoint>
    calculateClockInHeatmap(List<AttendanceDailySummary> summaries) {
        Map<HeatmapKey, Integer> clockInCounts = new HashMap<>();
        for (AttendanceDailySummary summary : summaries) {
            if (summary.getClockInTime() != null) {
                HeatmapKey key = new HeatmapKey(
                        summary.getWorkDate(),
                        summary.getClockInTime().getHour()
                );
                clockInCounts.merge(key, 1, Integer::sum);
            }
        }
        List<AttendanceDashboardStatisticsResponse.ClockInHeatmapPoint> points = new ArrayList<>();
        clockInCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(
                        Comparator.comparing(HeatmapKey::workDate).thenComparingInt(HeatmapKey::hour)
                ))
                .forEach(entry -> points.add(
                        new AttendanceDashboardStatisticsResponse.ClockInHeatmapPoint(
                                entry.getKey().workDate(),
                                entry.getKey().hour(),
                                entry.getValue()
                        )
                ));
        return List.copyOf(points);
    }

    private void requireSuperAdministrator(Long employeeId) {
        UserAttendanceClient.AuthorizationSummary authorization = requireData(
                userClient.findAuthorization(employeeId)
        );
        boolean allowed = safeList(authorization.roles()).stream().anyMatch(role ->
                ENABLED == role.status() && SUPER_ADMIN.equals(role.code())
        );
        if (!allowed) {
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

    private record HeatmapKey(LocalDate workDate, int hour) {
    }
}

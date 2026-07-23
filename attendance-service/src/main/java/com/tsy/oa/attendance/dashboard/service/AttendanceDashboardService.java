package com.tsy.oa.attendance.dashboard.service;

import com.tsy.oa.attendance.dashboard.dto.AttendanceDailyTrendResponse;
import com.tsy.oa.attendance.dashboard.dto.AttendanceStatisticsResponse;
import com.tsy.oa.attendance.dashboard.mapper.AttendanceDashboardMapper;
import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class AttendanceDashboardService {

    private final AttendanceDashboardMapper dashboardMapper;

    public AttendanceDashboardService(AttendanceDashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    @Transactional(readOnly = true)
    public AttendanceStatisticsResponse getStatistics(String month) {
        YearMonth yearMonth = parseMonth(month);
        List<AttendanceDailyTrendResponse> dailyTrend = dashboardMapper.findDailyTrend(
                yearMonth.atDay(1),
                yearMonth.plusMonths(1).atDay(1)
        );
        return new AttendanceStatisticsResponse(
                yearMonth.toString(),
                sum(dailyTrend, AttendanceDailyTrendResponse::normalCount),
                sum(dailyTrend, AttendanceDailyTrendResponse::lateCount),
                sum(dailyTrend, AttendanceDailyTrendResponse::earlyLeaveCount),
                sum(dailyTrend, AttendanceDailyTrendResponse::absentCount),
                dailyTrend
        );
    }

    private YearMonth parseMonth(String month) {
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
    }

    private long sum(
            List<AttendanceDailyTrendResponse> dailyTrend,
            java.util.function.Function<AttendanceDailyTrendResponse, Long> selector
    ) {
        return dailyTrend.stream().map(selector).mapToLong(Long::longValue).sum();
    }
}

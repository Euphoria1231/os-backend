package com.tsy.oa.flow.dashboard.service;

import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.flow.dashboard.dto.ApprovalStatisticsResponse;
import com.tsy.oa.flow.dashboard.dto.ApprovalStatusCountsResponse;
import com.tsy.oa.flow.mapper.FlowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@Service
public class FlowDashboardService {

    private final FlowMapper flowMapper;

    public FlowDashboardService(FlowMapper flowMapper) {
        this.flowMapper = flowMapper;
    }

    @Transactional(readOnly = true)
    public ApprovalStatisticsResponse getStatistics(String month) {
        YearMonth yearMonth = parseMonth(month);
        LocalDateTime startTime = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endTime = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
        ApprovalStatusCountsResponse statusCounts = flowMapper.countDashboardStatuses(startTime, endTime);
        return new ApprovalStatisticsResponse(
                yearMonth.toString(),
                valueOrZero(statusCounts.pendingCount()),
                valueOrZero(statusCounts.approvedCount()),
                valueOrZero(statusCounts.rejectedCount()),
                flowMapper.findDashboardTypeDistribution(startTime, endTime),
                flowMapper.findDashboardDailyTrend(startTime, endTime)
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

    private long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }
}

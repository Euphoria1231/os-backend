package com.tsy.oa.flow.dashboard.dto;

import java.util.List;

public record ApprovalStatisticsResponse(
        String month,
        long pendingCount,
        long approvedCount,
        long rejectedCount,
        List<ApprovalTypeDistributionResponse> typeDistribution,
        List<ApprovalDailyTrendResponse> dailyTrend
) {
    public ApprovalStatisticsResponse {
        typeDistribution = List.copyOf(typeDistribution);
        dailyTrend = List.copyOf(dailyTrend);
    }
}

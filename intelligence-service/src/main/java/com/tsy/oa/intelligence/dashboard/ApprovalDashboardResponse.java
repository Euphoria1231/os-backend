package com.tsy.oa.intelligence.dashboard;

import java.util.List;

public record ApprovalDashboardResponse(
        String month,
        long pendingCount,
        long approvedCount,
        long rejectedCount,
        List<ApprovalTypeDistributionResponse> typeDistribution,
        List<ApprovalDailyTrendResponse> dailyTrend
) {
    public ApprovalDashboardResponse {
        typeDistribution = List.copyOf(typeDistribution);
        dailyTrend = List.copyOf(dailyTrend);
    }
}

package com.tsy.oa.intelligence.dashboard;

import com.tsy.oa.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "flow-service",
        contextId = "approvalDashboardClient",
        path = "/internal/flow/dashboard"
)
public interface ApprovalDashboardClient {

    @GetMapping("/statistics")
    ApiResponse<ApprovalStatisticsResponse> statistics(@RequestParam String month);

    record ApprovalStatisticsResponse(
            String month,
            long pendingCount,
            long approvedCount,
            long rejectedCount,
            List<ApprovalTypeDistributionResponse> typeDistribution,
            List<ApprovalDailyTrendResponse> dailyTrend
    ) {
    }
}

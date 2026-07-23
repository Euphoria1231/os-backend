package com.tsy.oa.intelligence.dashboard;

import com.tsy.oa.common.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ApprovalDashboardService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalDashboardService.class);
    private static final String FAILURE_MESSAGE = "审批统计暂时不可用";

    private final ApprovalDashboardClient client;

    public ApprovalDashboardService(ApprovalDashboardClient client) {
        this.client = client;
    }

    public DashboardSectionResponse<ApprovalDashboardResponse> getApprovals(String month) {
        String normalizedMonth = DashboardMonth.normalize(month);
        try {
            ApiResponse<ApprovalDashboardClient.ApprovalStatisticsResponse> response =
                    client.statistics(normalizedMonth);
            if (response == null || response.code() != 0 || response.data() == null) {
                throw new IllegalStateException("flow-service returned an unsuccessful response");
            }
            ApprovalDashboardClient.ApprovalStatisticsResponse data = response.data();
            return DashboardSectionResponse.success(new ApprovalDashboardResponse(
                    data.month(),
                    data.pendingCount(),
                    data.approvedCount(),
                    data.rejectedCount(),
                    data.typeDistribution(),
                    data.dailyTrend()
            ));
        } catch (RuntimeException exception) {
            log.warn("Approval dashboard source is unavailable: {}", exception.getClass().getSimpleName());
            return DashboardSectionResponse.failed(FAILURE_MESSAGE);
        }
    }
}

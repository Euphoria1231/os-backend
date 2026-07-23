package com.tsy.oa.intelligence.dashboard;

import com.tsy.oa.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(
        name = "user-service",
        contextId = "organizationDashboardClient",
        path = "/internal/user/dashboard"
)
public interface OrganizationDashboardClient {

    @GetMapping("/organization")
    ApiResponse<OrganizationStatisticsResponse> organization();

    record OrganizationStatisticsResponse(
            long totalEmployees,
            long enabledEmployees,
            long disabledEmployees,
            List<NameCountResponse> departmentEmployeeCounts,
            List<NameCountResponse> positionEmployeeCounts
    ) {
    }
}

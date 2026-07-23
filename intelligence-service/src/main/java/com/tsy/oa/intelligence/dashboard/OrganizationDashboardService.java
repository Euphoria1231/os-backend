package com.tsy.oa.intelligence.dashboard;

import com.tsy.oa.common.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrganizationDashboardService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationDashboardService.class);
    private static final String FAILURE_MESSAGE = "组织统计暂时不可用";

    private final OrganizationDashboardClient client;

    public OrganizationDashboardService(OrganizationDashboardClient client) {
        this.client = client;
    }

    public DashboardSectionResponse<OrganizationDashboardResponse> getOrganization() {
        try {
            ApiResponse<OrganizationDashboardClient.OrganizationStatisticsResponse> response = client.organization();
            if (response == null || response.code() != 0 || response.data() == null) {
                throw new IllegalStateException("user-service returned an unsuccessful response");
            }
            OrganizationDashboardClient.OrganizationStatisticsResponse data = response.data();
            return DashboardSectionResponse.success(new OrganizationDashboardResponse(
                    data.totalEmployees(),
                    data.enabledEmployees(),
                    data.disabledEmployees(),
                    data.departmentEmployeeCounts(),
                    data.positionEmployeeCounts()
            ));
        } catch (RuntimeException exception) {
            log.warn("Organization dashboard source is unavailable: {}", exception.getClass().getSimpleName());
            return DashboardSectionResponse.failed(FAILURE_MESSAGE);
        }
    }
}

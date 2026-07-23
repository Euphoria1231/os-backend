package com.tsy.oa.intelligence.dashboard;

import java.util.List;

public record OrganizationDashboardResponse(
        long totalEmployees,
        long enabledEmployees,
        long disabledEmployees,
        List<NameCountResponse> departmentEmployeeCounts,
        List<NameCountResponse> positionEmployeeCounts
) {
    public OrganizationDashboardResponse {
        departmentEmployeeCounts = List.copyOf(departmentEmployeeCounts);
        positionEmployeeCounts = List.copyOf(positionEmployeeCounts);
    }
}

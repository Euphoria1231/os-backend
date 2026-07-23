package com.tsy.oa.user.dashboard.dto;

import java.util.List;

public record OrganizationStatisticsResponse(
        long totalEmployees,
        long enabledEmployees,
        long disabledEmployees,
        List<NameCountResponse> departmentEmployeeCounts,
        List<NameCountResponse> positionEmployeeCounts
) {
    public OrganizationStatisticsResponse {
        departmentEmployeeCounts = List.copyOf(departmentEmployeeCounts);
        positionEmployeeCounts = List.copyOf(positionEmployeeCounts);
    }
}

package com.tsy.oa.user.employee.dto;

public record ApprovalRouteResponse(
        Long applicantId,
        Long directLeaderId,
        String directLeaderName,
        Long departmentLeaderId,
        String departmentLeaderName
) {
}

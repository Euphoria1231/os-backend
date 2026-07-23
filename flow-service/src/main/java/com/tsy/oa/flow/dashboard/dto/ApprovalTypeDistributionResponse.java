package com.tsy.oa.flow.dashboard.dto;

public record ApprovalTypeDistributionResponse(
        String applicationType,
        Long count
) {
}

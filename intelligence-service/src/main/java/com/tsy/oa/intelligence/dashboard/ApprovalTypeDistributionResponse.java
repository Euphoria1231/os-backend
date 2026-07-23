package com.tsy.oa.intelligence.dashboard;

public record ApprovalTypeDistributionResponse(
        String applicationType,
        long count
) {
}

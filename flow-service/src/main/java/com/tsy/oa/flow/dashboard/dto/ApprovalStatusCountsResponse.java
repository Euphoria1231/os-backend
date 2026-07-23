package com.tsy.oa.flow.dashboard.dto;

public record ApprovalStatusCountsResponse(
        Long pendingCount,
        Long approvedCount,
        Long rejectedCount
) {
}

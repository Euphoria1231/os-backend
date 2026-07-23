package com.tsy.oa.flow.dashboard.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.flow.dashboard.dto.ApprovalStatisticsResponse;
import com.tsy.oa.flow.dashboard.service.FlowDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/flow/dashboard")
public class FlowDashboardController {

    private final FlowDashboardService dashboardService;

    public FlowDashboardController(FlowDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/statistics")
    public ApiResponse<ApprovalStatisticsResponse> statistics(@RequestParam String month) {
        return ApiResponse.success(dashboardService.getStatistics(month));
    }
}

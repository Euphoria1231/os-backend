package com.tsy.oa.user.dashboard.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.user.dashboard.dto.OrganizationStatisticsResponse;
import com.tsy.oa.user.dashboard.service.OrganizationStatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/user/dashboard")
public class OrganizationStatisticsController {

    private final OrganizationStatisticsService statisticsService;

    public OrganizationStatisticsController(OrganizationStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/organization")
    public ApiResponse<OrganizationStatisticsResponse> organization() {
        return ApiResponse.success(statisticsService.getOrganizationStatistics());
    }
}

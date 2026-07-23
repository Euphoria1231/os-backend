package com.tsy.oa.intelligence.dashboard;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/intelligence/dashboard")
public class DashboardController {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final OrganizationDashboardService organizationService;

    public DashboardController(OrganizationDashboardService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/organization")
    public ApiResponse<DashboardSectionResponse<OrganizationDashboardResponse>> organization(
            @RequestHeader(value = "X-Roles", defaultValue = "") List<String> roles
    ) {
        requireAdministrator(roles);
        return ApiResponse.success(organizationService.getOrganization());
    }

    private void requireAdministrator(List<String> roles) {
        if (roles == null || roles.stream().noneMatch(SUPER_ADMIN::equals)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}

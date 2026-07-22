package com.tsy.oa.intelligence.search.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.intelligence.search.dto.ApplicationSearchResponse;
import com.tsy.oa.intelligence.search.dto.IndexHealthResponse;
import com.tsy.oa.intelligence.search.dto.NoticeSearchResponse;
import com.tsy.oa.intelligence.search.dto.RebuildProgressResponse;
import com.tsy.oa.intelligence.search.dto.SearchPageResponse;
import com.tsy.oa.intelligence.search.service.SearchIndexAdministrationService;
import com.tsy.oa.intelligence.search.service.SearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/intelligence/search")
public class SearchController {

    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";
    private static final String DEPARTMENT_MANAGER_ROLE = "DEPARTMENT_MANAGER";

    private final SearchService searchService;
    private final SearchIndexAdministrationService administrationService;

    public SearchController(
            SearchService searchService,
            SearchIndexAdministrationService administrationService
    ) {
        this.searchService = searchService;
        this.administrationService = administrationService;
    }

    @GetMapping("/notices")
    public ApiResponse<SearchPageResponse<NoticeSearchResponse>> searchNotices(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize
    ) {
        return ApiResponse.success(searchService.searchNotices(keyword, page, pageSize));
    }

    @GetMapping("/applications")
    public ApiResponse<SearchPageResponse<ApplicationSearchResponse>> searchApplications(
            @RequestHeader("X-Employee-Id") @Min(1) long employeeId,
            @RequestHeader(value = "X-Roles", defaultValue = "") List<String> roles,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize
    ) {
        return ApiResponse.success(searchService.searchApplications(
                keyword, type, status, page, pageSize, employeeId,
                isAdministrator(roles), hasRole(roles, DEPARTMENT_MANAGER_ROLE)
        ));
    }

    @PostMapping("/indexes/notices/rebuild")
    public ApiResponse<RebuildProgressResponse> rebuildNotices(
            @RequestHeader(value = "X-Roles", defaultValue = "") List<String> roles
    ) {
        requireAdministrator(roles);
        return ApiResponse.success(administrationService.rebuildNotices());
    }

    @PostMapping("/indexes/applications/rebuild")
    public ApiResponse<RebuildProgressResponse> rebuildApplications(
            @RequestHeader(value = "X-Roles", defaultValue = "") List<String> roles
    ) {
        requireAdministrator(roles);
        return ApiResponse.success(administrationService.rebuildApplications());
    }

    @GetMapping("/indexes/health")
    public ApiResponse<IndexHealthResponse> health() {
        return ApiResponse.success(administrationService.health());
    }

    private boolean isAdministrator(List<String> roles) {
        return hasRole(roles, SUPER_ADMIN_ROLE);
    }

    private boolean hasRole(List<String> roles, String role) {
        return roles != null && roles.stream().anyMatch(role::equals);
    }

    private void requireAdministrator(List<String> roles) {
        if (!isAdministrator(roles)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}

package com.tsy.oa.intelligence.search.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.common.log.BusinessOperationLogger;
import com.tsy.oa.common.log.HttpOperationLogContexts;
import com.tsy.oa.common.log.OperationLogContext;
import com.tsy.oa.intelligence.search.dto.ApplicationSearchResponse;
import com.tsy.oa.intelligence.search.dto.IndexHealthResponse;
import com.tsy.oa.intelligence.search.dto.NoticeSearchResponse;
import com.tsy.oa.intelligence.search.dto.RebuildProgressResponse;
import com.tsy.oa.intelligence.search.dto.SearchPageResponse;
import com.tsy.oa.intelligence.search.service.SearchIndexAdministrationService;
import com.tsy.oa.intelligence.search.service.SearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.servlet.http.HttpServletRequest;
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

    private final SearchService searchService;
    private final SearchIndexAdministrationService administrationService;
    private final BusinessOperationLogger operationLogger;

    public SearchController(
            SearchService searchService,
            SearchIndexAdministrationService administrationService,
            BusinessOperationLogger operationLogger
    ) {
        this.searchService = searchService;
        this.administrationService = administrationService;
        this.operationLogger = operationLogger;
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
                isAdministrator(roles)
        ));
    }

    @PostMapping("/indexes/notices/rebuild")
    public ApiResponse<RebuildProgressResponse> rebuildNotices(
            @RequestHeader("X-Employee-Id") @Min(1) long employeeId,
            @RequestHeader(value = "X-Roles", defaultValue = "") List<String> roles,
            HttpServletRequest httpRequest
    ) {
        OperationLogContext context = rebuildContext(
                httpRequest, employeeId, "NOTICE_INDEX", "oa-notices", "重建公告索引"
        );
        return ApiResponse.success(operationLogger.execute(context, () -> {
            requireAdministrator(roles);
            return administrationService.rebuildNotices();
        }));
    }

    @PostMapping("/indexes/applications/rebuild")
    public ApiResponse<RebuildProgressResponse> rebuildApplications(
            @RequestHeader("X-Employee-Id") @Min(1) long employeeId,
            @RequestHeader(value = "X-Roles", defaultValue = "") List<String> roles,
            HttpServletRequest httpRequest
    ) {
        OperationLogContext context = rebuildContext(
                httpRequest,
                employeeId,
                "APPLICATION_INDEX",
                "oa-applications",
                "重建审批申请索引"
        );
        return ApiResponse.success(operationLogger.execute(context, () -> {
            requireAdministrator(roles);
            return administrationService.rebuildApplications();
        }));
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

    private OperationLogContext rebuildContext(
            HttpServletRequest request,
            long employeeId,
            String targetType,
            String targetId,
            String summary
    ) {
        return HttpOperationLogContexts.create(
                request,
                employeeId,
                null,
                "SEARCH",
                "REBUILD_INDEX",
                targetType,
                targetId,
                summary
        );
    }
}

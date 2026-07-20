package com.tsy.oa.user.rbac.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.user.rbac.dto.ApiPermissionRequest;
import com.tsy.oa.user.rbac.dto.ApiPermissionResponse;
import com.tsy.oa.user.rbac.service.RbacService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/api-permissions")
public class ApiPermissionController {

    private final RbacService rbacService;

    public ApiPermissionController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @PostMapping
    public ApiResponse<ApiPermissionResponse> create(
            @Valid @RequestBody ApiPermissionRequest request
    ) {
        return ApiResponse.success(rbacService.createApiPermission(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ApiPermissionResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(rbacService.getApiPermission(id));
    }

    @GetMapping
    public ApiResponse<List<ApiPermissionResponse>> list() {
        return ApiResponse.success(rbacService.listApiPermissions());
    }

    @PutMapping("/{id}")
    public ApiResponse<ApiPermissionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ApiPermissionRequest request
    ) {
        return ApiResponse.success(rbacService.updateApiPermission(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        rbacService.deleteApiPermission(id);
        return ApiResponse.success(null);
    }
}

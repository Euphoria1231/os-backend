package com.tsy.oa.user.rbac.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.user.rbac.dto.RoleGrantRequest;
import com.tsy.oa.user.rbac.dto.RoleRequest;
import com.tsy.oa.user.rbac.dto.RoleResponse;
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
@RequestMapping("/api/user/roles")
public class RoleController {

    private final RbacService rbacService;

    public RoleController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @PostMapping
    public ApiResponse<RoleResponse> create(@Valid @RequestBody RoleRequest request) {
        return ApiResponse.success(rbacService.createRole(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RoleResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(rbacService.getRole(id));
    }

    @GetMapping
    public ApiResponse<List<RoleResponse>> list() {
        return ApiResponse.success(rbacService.listRoles());
    }

    @PutMapping("/{id}")
    public ApiResponse<RoleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody RoleRequest request
    ) {
        return ApiResponse.success(rbacService.updateRole(id, request));
    }

    @PutMapping("/{id}/permissions")
    public ApiResponse<Void> assignPermissions(
            @PathVariable Long id,
            @Valid @RequestBody RoleGrantRequest request
    ) {
        rbacService.assignRolePermissions(id, request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        rbacService.deleteRole(id);
        return ApiResponse.success(null);
    }
}

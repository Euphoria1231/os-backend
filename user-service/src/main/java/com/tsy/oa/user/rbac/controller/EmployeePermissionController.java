package com.tsy.oa.user.rbac.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.user.rbac.dto.EmployeeAuthorizationResponse;
import com.tsy.oa.user.rbac.dto.EmployeeRoleRequest;
import com.tsy.oa.user.rbac.service.RbacService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/employees")
public class EmployeePermissionController {

    private final RbacService rbacService;

    public EmployeePermissionController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @PutMapping("/{id}/roles")
    public ApiResponse<Void> assignRoles(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRoleRequest request
    ) {
        rbacService.assignEmployeeRoles(id, request);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/permissions")
    public ApiResponse<EmployeeAuthorizationResponse> getAuthorization(@PathVariable Long id) {
        return ApiResponse.success(rbacService.getEmployeeAuthorization(id));
    }
}

package com.tsy.oa.user.rbac.dto;

import java.util.List;

public record EmployeeAuthorizationResponse(
        List<RoleResponse> roles,
        List<MenuResponse> menus,
        List<ApiPermissionResponse> apiPermissions
) {

    public EmployeeAuthorizationResponse {
        roles = List.copyOf(roles);
        menus = List.copyOf(menus);
        apiPermissions = List.copyOf(apiPermissions);
    }
}

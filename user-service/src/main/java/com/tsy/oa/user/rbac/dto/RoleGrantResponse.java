package com.tsy.oa.user.rbac.dto;

import java.util.List;

public record RoleGrantResponse(
        List<Long> menuIds,
        List<Long> apiPermissionIds
) {
}

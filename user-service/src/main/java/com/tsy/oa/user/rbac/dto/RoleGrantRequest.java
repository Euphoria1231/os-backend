package com.tsy.oa.user.rbac.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record RoleGrantRequest(
        @NotNull(message = "菜单ID列表不能为空") List<@Positive Long> menuIds,
        @NotNull(message = "接口权限ID列表不能为空") List<@Positive Long> apiPermissionIds
) {
}

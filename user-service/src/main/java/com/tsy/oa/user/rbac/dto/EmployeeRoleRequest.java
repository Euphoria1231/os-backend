package com.tsy.oa.user.rbac.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record EmployeeRoleRequest(
        @NotNull(message = "角色ID列表不能为空") List<@Positive Long> roleIds
) {
}

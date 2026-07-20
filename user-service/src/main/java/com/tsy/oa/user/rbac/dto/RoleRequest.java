package com.tsy.oa.user.rbac.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RoleRequest(
        @NotBlank(message = "角色编码不能为空")
        @Size(max = 50, message = "角色编码不能超过50个字符")
        @Pattern(regexp = "[A-Za-z0-9_]+", message = "角色编码只能包含字母、数字和下划线")
        String code,

        @NotBlank(message = "角色名称不能为空")
        @Size(max = 100, message = "角色名称不能超过100个字符")
        String name,

        @NotNull(message = "角色状态不能为空")
        @Min(value = 0, message = "角色状态只能为0或1")
        @Max(value = 1, message = "角色状态只能为0或1")
        Integer status
) {
}

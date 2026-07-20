package com.tsy.oa.user.rbac.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MenuRequest(
        @NotNull(message = "上级菜单ID不能为空")
        @Min(value = 0, message = "上级菜单ID不能小于0")
        Long parentId,

        @NotBlank(message = "菜单名称不能为空")
        @Size(max = 100, message = "菜单名称不能超过100个字符")
        String name,

        @Size(max = 200, message = "路由地址不能超过200个字符")
        String path,

        @Size(max = 200, message = "组件名称不能超过200个字符")
        String component,

        @Size(max = 100, message = "权限标识不能超过100个字符")
        String permission,

        @NotBlank(message = "菜单类型不能为空")
        @Pattern(regexp = "(?i)DIRECTORY|MENU|BUTTON", message = "菜单类型不合法")
        String type,

        @NotNull(message = "显示顺序不能为空")
        @Min(value = 0, message = "显示顺序不能小于0")
        Integer sortOrder,

        @NotNull(message = "菜单状态不能为空")
        @Min(value = 0, message = "菜单状态只能为0或1")
        @Max(value = 1, message = "菜单状态只能为0或1")
        Integer status
) {
}

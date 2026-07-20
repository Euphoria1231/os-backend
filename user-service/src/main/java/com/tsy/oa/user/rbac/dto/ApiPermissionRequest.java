package com.tsy.oa.user.rbac.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ApiPermissionRequest(
        @NotBlank(message = "接口权限编码不能为空")
        @Size(max = 100, message = "接口权限编码不能超过100个字符")
        @Pattern(regexp = "[A-Za-z0-9_]+", message = "接口权限编码只能包含字母、数字和下划线")
        String code,

        @NotBlank(message = "接口权限名称不能为空")
        @Size(max = 100, message = "接口权限名称不能超过100个字符")
        String name,

        @NotBlank(message = "HTTP 方法不能为空")
        @Pattern(regexp = "(?i)GET|POST|PUT|DELETE|PATCH", message = "HTTP 方法不合法")
        String httpMethod,

        @NotBlank(message = "接口路径不能为空")
        @Size(max = 200, message = "接口路径不能超过200个字符")
        String pathPattern,

        @NotNull(message = "接口权限状态不能为空")
        @Min(value = 0, message = "接口权限状态只能为0或1")
        @Max(value = 1, message = "接口权限状态只能为0或1")
        Integer status
) {
}

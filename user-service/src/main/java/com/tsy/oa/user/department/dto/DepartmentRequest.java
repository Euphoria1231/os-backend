package com.tsy.oa.user.department.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotNull(message = "上级部门不能为空")
        @PositiveOrZero(message = "上级部门ID不能为负数")
        Long parentId,

        @NotBlank(message = "部门名称不能为空")
        @Size(max = 100, message = "部门名称不能超过100个字符")
        String name,

        Long leaderEmployeeId,

        @NotNull(message = "显示顺序不能为空")
        @PositiveOrZero(message = "显示顺序不能为负数")
        Integer sortOrder,

        @NotNull(message = "部门状态不能为空")
        @Min(value = 0, message = "部门状态只能为0或1")
        @Max(value = 1, message = "部门状态只能为0或1")
        Integer status
) {
}

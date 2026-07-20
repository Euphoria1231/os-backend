package com.tsy.oa.user.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmployeeCreateRequest(
        @NotBlank(message = "员工编号不能为空") @Size(max = 50) String employeeNo,
        @NotBlank(message = "登录账号不能为空") @Size(max = 50) String username,
        @NotBlank(message = "登录密码不能为空") @Size(min = 8, max = 72, message = "密码长度必须为8到72个字符") String password,
        @NotBlank(message = "员工姓名不能为空") @Size(max = 100) String realName,
        @NotNull(message = "部门不能为空") Long departmentId,
        @NotNull(message = "岗位不能为空") Long positionId,
        Long leaderId,
        @Size(max = 30) String phone,
        @Email(message = "邮箱格式不正确") @Size(max = 100) String email,
        @NotNull @Min(0) @Max(1) Integer status
) {
}

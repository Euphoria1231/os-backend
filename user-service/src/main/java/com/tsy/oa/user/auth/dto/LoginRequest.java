package com.tsy.oa.user.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "登录账号不能为空") String username,
        @NotBlank(message = "登录密码不能为空") String password
) {
}

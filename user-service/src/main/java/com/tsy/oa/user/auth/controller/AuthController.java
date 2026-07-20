package com.tsy.oa.user.auth.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.user.auth.dto.LoginRequest;
import com.tsy.oa.user.auth.dto.LoginResponse;
import com.tsy.oa.user.auth.service.AuthService;
import com.tsy.oa.user.employee.dto.EmployeeResponse;
import com.tsy.oa.user.error.UserErrorCode;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        authService.logout(extractToken(authorization));
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    public ApiResponse<EmployeeResponse> currentUser(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        return ApiResponse.success(authService.currentUser(extractToken(authorization)));
    }

    private String extractToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(UserErrorCode.TOKEN_INVALID);
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new BusinessException(UserErrorCode.TOKEN_INVALID);
        }
        return token;
    }
}

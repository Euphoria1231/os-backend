package com.tsy.oa.user.auth.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.common.log.BusinessOperationLogger;
import com.tsy.oa.common.log.HttpOperationLogContexts;
import com.tsy.oa.common.log.OperationLogContext;
import com.tsy.oa.user.auth.dto.LoginRequest;
import com.tsy.oa.user.auth.dto.LoginResponse;
import com.tsy.oa.user.auth.service.AuthService;
import com.tsy.oa.user.employee.dto.EmployeeResponse;
import com.tsy.oa.user.error.UserErrorCode;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
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
    private static final String EMPLOYEE_HEADER = "X-Employee-Id";

    private final AuthService authService;
    private final BusinessOperationLogger operationLogger;

    public AuthController(AuthService authService, BusinessOperationLogger operationLogger) {
        this.authService = authService;
        this.operationLogger = operationLogger;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        OperationLogContext context = logContext(
                httpRequest, null, request.username(), "LOGIN", "EMPLOYEE", null,
                "用户登录：" + request.username()
        );
        LoginResponse response = operationLogger.executeWithContext(
                context,
                () -> authService.login(request),
                result -> logContext(
                        httpRequest,
                        result.employee().id(),
                        null,
                        "LOGIN",
                        "EMPLOYEE",
                        result.employee().id().toString(),
                        "用户登录：" + result.employee().username()
                )
        );
        return ApiResponse.success(response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = EMPLOYEE_HEADER, required = false) Long employeeId,
            HttpServletRequest httpRequest
    ) {
        OperationLogContext context = logContext(
                httpRequest, employeeId, null, "LOGOUT", "EMPLOYEE",
                employeeId == null ? null : employeeId.toString(), "用户退出登录"
        );
        operationLogger.execute(context, () -> authService.logout(extractToken(authorization)));
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

    private OperationLogContext logContext(
            HttpServletRequest request,
            Long operatorId,
            String operatorName,
            String operationType,
            String targetType,
            String targetId,
            String summary
    ) {
        return HttpOperationLogContexts.create(
                request, operatorId, operatorName, "AUTH", operationType,
                targetType, targetId, summary
        );
    }
}

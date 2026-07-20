package com.tsy.oa.user.auth.dto;

import com.tsy.oa.user.employee.dto.EmployeeResponse;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        EmployeeResponse employee
) {
}

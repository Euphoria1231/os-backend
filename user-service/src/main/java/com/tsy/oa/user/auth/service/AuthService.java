package com.tsy.oa.user.auth.service;

import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.common.security.JwtClaims;
import com.tsy.oa.common.security.JwtTokenService;
import com.tsy.oa.user.auth.dto.LoginRequest;
import com.tsy.oa.user.auth.dto.LoginResponse;
import com.tsy.oa.user.employee.dto.EmployeeResponse;
import com.tsy.oa.user.employee.mapper.EmployeeMapper;
import com.tsy.oa.user.employee.model.Employee;
import com.tsy.oa.user.error.UserErrorCode;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
public class AuthService {

    private final EmployeeMapper employeeMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(
            EmployeeMapper employeeMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            TokenBlacklistService tokenBlacklistService
    ) {
        this.employeeMapper = employeeMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Employee employee = employeeMapper.findByUsername(request.username().trim());
        if (employee == null || !passwordEncoder.matches(request.password(), employee.getPasswordHash())) {
            throw new BusinessException(UserErrorCode.INVALID_CREDENTIALS);
        }
        if (!Integer.valueOf(1).equals(employee.getStatus())) {
            throw new BusinessException(UserErrorCode.EMPLOYEE_DISABLED);
        }

        String token = jwtTokenService.issueToken(
                employee.getId(),
                employee.getUsername(),
                List.of(),
                List.of()
        );
        return new LoginResponse(
                token,
                "Bearer",
                jwtTokenService.validity().toSeconds(),
                EmployeeResponse.from(employee)
        );
    }

    public EmployeeResponse currentUser(String token) {
        JwtClaims claims = requireActiveClaims(token);
        Employee employee = employeeMapper.findById(claims.employeeId());
        if (employee == null || !Integer.valueOf(1).equals(employee.getStatus())) {
            throw new BusinessException(UserErrorCode.TOKEN_INVALID);
        }
        return EmployeeResponse.from(employee);
    }

    public void logout(String token) {
        JwtClaims claims = requireActiveClaims(token);
        Duration ttl = jwtTokenService.remainingTtl(token);
        tokenBlacklistService.blacklist(claims.tokenId(), ttl);
    }

    private JwtClaims requireActiveClaims(String token) {
        try {
            JwtClaims claims = jwtTokenService.parseToken(token);
            if (tokenBlacklistService.isBlacklisted(claims.tokenId())) {
                throw new BusinessException(UserErrorCode.TOKEN_INVALID);
            }
            return claims;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(UserErrorCode.TOKEN_INVALID);
        }
    }
}

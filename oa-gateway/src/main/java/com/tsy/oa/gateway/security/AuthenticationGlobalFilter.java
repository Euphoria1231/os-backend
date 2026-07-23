package com.tsy.oa.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.security.JwtClaims;
import com.tsy.oa.common.security.JwtTokenService;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class AuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String EMPLOYEE_ID_HEADER = "X-Employee-Id";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String ROLES_HEADER = "X-Roles";
    private static final String PERMISSIONS_HEADER = "X-Permissions";
    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/user/auth/login",
            "/api/user/health"
    );
    private static final Set<String> AUTHENTICATED_PATHS = Set.of(
            "/api/user/auth/logout",
            "/api/user/auth/me",
            "/api/user/operation-logs/mine"
    );

    private final JwtTokenService jwtTokenService;
    private final TokenBlacklistChecker tokenBlacklistChecker;
    private final ObjectMapper objectMapper;
    private final PathPatternParser pathPatternParser = new PathPatternParser();

    public AuthenticationGlobalFilter(
            JwtTokenService jwtTokenService,
            TokenBlacklistChecker tokenBlacklistChecker,
            ObjectMapper objectMapper
    ) {
        this.jwtTokenService = jwtTokenService;
        this.tokenBlacklistChecker = tokenBlacklistChecker;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        if (shouldSkipAuthentication(exchange.getRequest().getMethod(), path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (token == null) {
            return writeFailure(exchange, HttpStatus.UNAUTHORIZED, GatewaySecurityErrorCode.TOKEN_INVALID);
        }

        JwtClaims claims;
        try {
            claims = jwtTokenService.parseToken(token);
        } catch (JwtException | IllegalArgumentException exception) {
            return writeFailure(exchange, HttpStatus.UNAUTHORIZED, GatewaySecurityErrorCode.TOKEN_INVALID);
        }

        JwtClaims activeClaims = claims;
        return tokenBlacklistChecker.isBlacklisted(claims.tokenId())
                .flatMap(blacklisted -> {
                    if (Boolean.TRUE.equals(blacklisted)) {
                        return writeFailure(
                                exchange,
                                HttpStatus.UNAUTHORIZED,
                                GatewaySecurityErrorCode.TOKEN_INVALID
                        );
                    }
                    if (!isAuthorized(exchange.getRequest().getMethod(), path, activeClaims)) {
                        return writeFailure(exchange, HttpStatus.FORBIDDEN, GatewaySecurityErrorCode.FORBIDDEN);
                    }
                    return chain.filter(withIdentityHeaders(exchange, activeClaims));
                });
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean shouldSkipAuthentication(HttpMethod method, String path) {
        return HttpMethod.OPTIONS.equals(method) || !path.startsWith("/api/") || PUBLIC_PATHS.contains(path);
    }

    private boolean isAuthorized(HttpMethod method, String path, JwtClaims claims) {
        if (AUTHENTICATED_PATHS.contains(path) || claims.roles().contains(SUPER_ADMIN_ROLE)) {
            return true;
        }
        if (method == null) {
            return false;
        }
        return claims.permissions().stream().anyMatch(permission -> matches(permission, method, path));
    }

    private boolean matches(String authority, HttpMethod method, String path) {
        int separator = authority.indexOf(':');
        if (separator <= 0 || separator == authority.length() - 1) {
            return false;
        }
        String allowedMethod = authority.substring(0, separator);
        String pathPattern = authority.substring(separator + 1);
        if (!method.name().equalsIgnoreCase(allowedMethod)) {
            return false;
        }
        try {
            PathPattern pattern = pathPatternParser.parse(pathPattern);
            return pattern.matches(org.springframework.http.server.PathContainer.parsePath(path));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private ServerWebExchange withIdentityHeaders(ServerWebExchange exchange, JwtClaims claims) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(EMPLOYEE_ID_HEADER);
                    headers.remove(USERNAME_HEADER);
                    headers.remove(ROLES_HEADER);
                    headers.remove(PERMISSIONS_HEADER);
                    headers.set(EMPLOYEE_ID_HEADER, String.valueOf(claims.employeeId()));
                    headers.set(USERNAME_HEADER, claims.username());
                    headers.put(ROLES_HEADER, claims.roles());
                    headers.put(PERMISSIONS_HEADER, claims.permissions());
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    private String extractToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private Mono<Void> writeFailure(
            ServerWebExchange exchange,
            HttpStatus status,
            GatewaySecurityErrorCode errorCode
    ) {
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(ApiResponse.failure(errorCode));
        } catch (JsonProcessingException exception) {
            body = "{\"code\":50000,\"message\":\"系统内部错误\",\"data\":null}"
                    .getBytes(StandardCharsets.UTF_8);
        }
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}

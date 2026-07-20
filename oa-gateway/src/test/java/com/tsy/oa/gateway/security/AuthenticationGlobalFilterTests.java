package com.tsy.oa.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsy.oa.common.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationGlobalFilterTests {

    private static final String SECRET = "test-jwt-secret-key-with-at-least-thirty-two-bytes";

    private final JwtTokenService jwtTokenService = new JwtTokenService(SECRET, Duration.ofHours(1));
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void allowsLoginWithoutToken() {
        AuthenticationGlobalFilter filter = filter(tokenId -> Mono.just(false));
        MockServerWebExchange exchange = exchange(HttpMethod.POST, "/api/user/auth/login", null);
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, capture(forwarded)).block();

        assertEquals(exchange, forwarded.get());
    }

    @Test
    void rejectsProtectedRequestWithoutToken() throws Exception {
        AuthenticationGlobalFilter filter = filter(tokenId -> Mono.just(false));
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/api/user/employees", null);

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertEquals(40102, responseCode(exchange));
    }

    @Test
    void forwardsIdentityWhenTokenContainsMatchingPermission() {
        AuthenticationGlobalFilter filter = filter(tokenId -> Mono.just(false));
        String token = jwtTokenService.issueToken(
                10L,
                "zhangsan",
                List.of("EMPLOYEE"),
                List.of("GET:/api/user/employees/**")
        );
        MockServerWebExchange exchange = exchange(
                HttpMethod.GET,
                "/api/user/employees/10",
                "Bearer " + token
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, capture(forwarded)).block();

        ServerWebExchange forwardedExchange = forwarded.get();
        assertEquals("10", forwardedExchange.getRequest().getHeaders().getFirst("X-Employee-Id"));
        assertEquals("zhangsan", forwardedExchange.getRequest().getHeaders().getFirst("X-Username"));
    }

    @Test
    void rejectsBlacklistedOrUnauthorizedToken() throws Exception {
        String token = jwtTokenService.issueToken(
                10L,
                "zhangsan",
                List.of("EMPLOYEE"),
                List.of("GET:/api/user/notices/**")
        );

        AuthenticationGlobalFilter blacklistedFilter = filter(tokenId -> Mono.just(true));
        MockServerWebExchange blacklisted = exchange(
                HttpMethod.GET, "/api/user/employees/10", "Bearer " + token
        );
        blacklistedFilter.filter(blacklisted, ignored -> Mono.empty()).block();
        assertEquals(HttpStatus.UNAUTHORIZED, blacklisted.getResponse().getStatusCode());
        assertEquals(40102, responseCode(blacklisted));

        AuthenticationGlobalFilter unauthorizedFilter = filter(tokenId -> Mono.just(false));
        MockServerWebExchange unauthorized = exchange(
                HttpMethod.GET, "/api/user/employees/10", "Bearer " + token
        );
        unauthorizedFilter.filter(unauthorized, ignored -> Mono.empty()).block();
        assertEquals(HttpStatus.FORBIDDEN, unauthorized.getResponse().getStatusCode());
        assertEquals(40302, responseCode(unauthorized));
    }

    @Test
    void removesSpoofedIdentityHeadersBeforeForwarding() {
        AuthenticationGlobalFilter filter = filter(tokenId -> Mono.just(false));
        String token = jwtTokenService.issueToken(
                10L,
                "zhangsan",
                List.of("SUPER_ADMIN"),
                List.of()
        );
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/user/roles")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-Employee-Id", "999")
                .header("X-Username", "spoofed")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, capture(forwarded)).block();

        assertEquals("10", forwarded.get().getRequest().getHeaders().getFirst("X-Employee-Id"));
        assertEquals("zhangsan", forwarded.get().getRequest().getHeaders().getFirst("X-Username"));
    }

    private AuthenticationGlobalFilter filter(TokenBlacklistChecker blacklistChecker) {
        return new AuthenticationGlobalFilter(jwtTokenService, blacklistChecker, objectMapper);
    }

    private GatewayFilterChain capture(AtomicReference<ServerWebExchange> forwarded) {
        return exchange -> {
            forwarded.set(exchange);
            return Mono.empty();
        };
    }

    private MockServerWebExchange exchange(HttpMethod method, String path, String authorization) {
        MockServerHttpRequest.BodyBuilder request = MockServerHttpRequest.method(method, path);
        if (authorization != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return MockServerWebExchange.from(request.build());
    }

    private int responseCode(MockServerWebExchange exchange) throws Exception {
        String body = exchange.getResponse().getBodyAsString().block();
        assertTrue(body != null && !body.isBlank());
        return objectMapper.readTree(body).path("code").asInt();
    }
}

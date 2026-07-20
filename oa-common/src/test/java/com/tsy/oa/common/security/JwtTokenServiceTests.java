package com.tsy.oa.common.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenServiceTests {

    private static final String SECRET = "test-jwt-secret-key-with-at-least-thirty-two-bytes";

    @Test
    void issuedTokenPreservesIdentityRolesAndPermissions() {
        JwtTokenService tokenService = new JwtTokenService(SECRET, Duration.ofHours(1));

        String token = tokenService.issueToken(
                10L,
                "zhangsan",
                List.of("ADMIN"),
                List.of("GET:/api/user/**")
        );
        JwtClaims claims = tokenService.parseToken(token);

        assertEquals(10L, claims.employeeId());
        assertEquals("zhangsan", claims.username());
        assertEquals(List.of("ADMIN"), claims.roles());
        assertEquals(List.of("GET:/api/user/**"), claims.permissions());
        assertFalse(claims.tokenId().isBlank());
        assertTrue(tokenService.remainingTtl(token).isPositive());
    }
}

package com.tsy.oa.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class JwtTokenService {

    private static final String EMPLOYEE_ID_CLAIM = "employeeId";
    private static final String ROLES_CLAIM = "roles";
    private static final String PERMISSIONS_CLAIM = "permissions";

    private final SecretKey signingKey;
    private final Duration validity;

    public JwtTokenService(String secret, Duration validity) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 bytes");
        }
        if (validity.isZero() || validity.isNegative()) {
            throw new IllegalArgumentException("JWT validity must be positive");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.validity = validity;
    }

    public String issueToken(
            Long employeeId,
            String username,
            List<String> roles,
            List<String> permissions
    ) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(validity);

        return Jwts.builder()
                .subject(username)
                .id(UUID.randomUUID().toString())
                .claim(EMPLOYEE_ID_CLAIM, employeeId)
                .claim(ROLES_CLAIM, roles)
                .claim(PERMISSIONS_CLAIM, permissions)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public JwtClaims parseToken(String token) {
        Claims claims = parseClaims(token);
        Number employeeId = claims.get(EMPLOYEE_ID_CLAIM, Number.class);
        return new JwtClaims(
                employeeId.longValue(),
                claims.getSubject(),
                toStringList(claims.get(ROLES_CLAIM, List.class)),
                toStringList(claims.get(PERMISSIONS_CLAIM, List.class)),
                claims.getId(),
                claims.getExpiration().toInstant()
        );
    }

    public Duration remainingTtl(String token) {
        Duration remaining = Duration.between(Instant.now(), parseClaims(token).getExpiration().toInstant());
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    public Duration validity() {
        return validity;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private List<String> toStringList(List<?> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().map(String::valueOf).toList();
    }
}

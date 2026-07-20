package com.tsy.oa.common.security;

import java.time.Instant;
import java.util.List;

public record JwtClaims(
        Long employeeId,
        String username,
        List<String> roles,
        List<String> permissions,
        String tokenId,
        Instant expiresAt
) {

    public JwtClaims {
        roles = List.copyOf(roles);
        permissions = List.copyOf(permissions);
    }
}

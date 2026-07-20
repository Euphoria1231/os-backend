package com.tsy.oa.gateway.security;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface TokenBlacklistChecker {

    Mono<Boolean> isBlacklisted(String tokenId);
}

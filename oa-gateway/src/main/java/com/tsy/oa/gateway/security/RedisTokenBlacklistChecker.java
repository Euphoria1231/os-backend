package com.tsy.oa.gateway.security;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RedisTokenBlacklistChecker implements TokenBlacklistChecker {

    private static final String KEY_PREFIX = "auth:blacklist:";

    private final ReactiveStringRedisTemplate redisTemplate;

    public RedisTokenBlacklistChecker(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Boolean> isBlacklisted(String tokenId) {
        return redisTemplate.hasKey(KEY_PREFIX + tokenId);
    }
}

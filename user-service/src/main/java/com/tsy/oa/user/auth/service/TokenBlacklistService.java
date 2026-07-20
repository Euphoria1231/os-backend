package com.tsy.oa.user.auth.service;

import java.time.Duration;

public interface TokenBlacklistService {

    void blacklist(String tokenId, Duration ttl);

    boolean isBlacklisted(String tokenId);
}

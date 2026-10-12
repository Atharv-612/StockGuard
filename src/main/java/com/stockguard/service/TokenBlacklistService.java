package com.stockguard.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory thread-safe JWT revocation registry.
 * Stores invalidated tokens until their natural expiration time.
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    // Map storing token signature/hash -> expiration Date
    private final Map<String, Date> blacklistedTokens = new ConcurrentHashMap<>();

    public void blacklistToken(String token, Date expirationDate) {
        if (token == null || token.isBlank()) return;
        blacklistedTokens.put(token, expirationDate != null ? expirationDate : new Date(System.currentTimeMillis() + 86400000));
        log.info("Token successfully blacklisted. Active blacklisted count: {}", blacklistedTokens.size());
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) return false;
        Date expiry = blacklistedTokens.get(token);
        if (expiry == null) {
            return false;
        }
        if (expiry.before(new Date())) {
            blacklistedTokens.remove(token);
            return false;
        }
        return true;
    }

    /**
     * Periodic cleanup of expired tokens every 30 minutes to prevent memory leak.
     */
    public void cleanupExpiredTokens() {
        Date now = new Date();
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue().before(now));
    }
}

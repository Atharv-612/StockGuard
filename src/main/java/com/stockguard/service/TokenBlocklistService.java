package com.stockguard.service;

import com.stockguard.entity.TokenBlocklist;
import com.stockguard.repository.TokenBlocklistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Manages the server-side JWT revocation list.
 * Tokens are added when a user logs out or when an admin forces a session revocation.
 * Expired tokens are pruned automatically every hour to keep the table small.
 */
@Service
public class TokenBlocklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlocklistService.class);

    private final TokenBlocklistRepository blocklistRepository;

    public TokenBlocklistService(TokenBlocklistRepository blocklistRepository) {
        this.blocklistRepository = blocklistRepository;
    }

    /**
     * Revoke a token by adding its JTI to the blocklist.
     *
     * @param jti       The unique JWT ID claim from the token.
     * @param userEmail Email of the token's owner.
     * @param expiresAt The token's natural expiry (from the 'exp' claim).
     * @param reason    Why the token was revoked (e.g. "LOGOUT").
     */
    @Transactional
    public void revokeToken(String jti, String userEmail, Date expiresAt, String reason) {
        if (blocklistRepository.existsByJti(jti)) {
            return; // Already revoked — idempotent
        }
        TokenBlocklist entry = new TokenBlocklist();
        entry.setJti(jti);
        entry.setUserEmail(userEmail);
        entry.setExpiresAt(toLocalDateTime(expiresAt));
        entry.setRevokeReason(reason != null ? reason : "LOGOUT");
        blocklistRepository.save(entry);
        log.debug("JWT revoked for user '{}' (jti={})", userEmail, jti);
    }

    /**
     * Returns true if the token's JTI has been explicitly revoked.
     */
    public boolean isRevoked(String jti) {
        return blocklistRepository.existsByJti(jti);
    }

    /**
     * Clean up naturally expired tokens from the blocklist.
     * Runs every hour. Keeps the table lean without manual intervention.
     */
    @Scheduled(fixedRate = 3_600_000) // 1 hour
    @Transactional
    public void pruneExpiredTokens() {
        int deleted = blocklistRepository.deleteExpiredBefore(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Token blocklist pruned: {} expired entries removed", deleted);
        }
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) return LocalDateTime.now().plusDays(1);
        return Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}

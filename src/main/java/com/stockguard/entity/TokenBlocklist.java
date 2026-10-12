package com.stockguard.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Stores invalidated JWT tokens (logout / forced revocation).
 * The JwtAuthenticationFilter checks this table before accepting a token.
 * Expired tokens are automatically cleaned up by a scheduled task.
 */
@Entity
@Table(name = "token_blocklist", indexes = {
        @Index(name = "idx_token_jti", columnList = "jti", unique = true),
        @Index(name = "idx_token_expires", columnList = "expires_at")
})
public class TokenBlocklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** JWT ID claim — unique identifier for the token */
    @Column(name = "jti", nullable = false, unique = true, length = 100)
    private String jti;

    /** Email of the user who owns the token */
    @Column(name = "user_email", nullable = false, length = 120)
    private String userEmail;

    /** When this token naturally expires; used for scheduled clean-up */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** When the token was explicitly revoked */
    @Column(name = "revoked_at", nullable = false, updatable = false)
    private LocalDateTime revokedAt = LocalDateTime.now();

    /** Reason for revocation (e.g. LOGOUT, FORCED_LOGOUT, PASSWORD_CHANGE) */
    @Column(name = "revoke_reason", length = 50)
    private String revokeReason = "LOGOUT";

    public TokenBlocklist() {}

    public Long getId() { return id; }

    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    public String getRevokeReason() { return revokeReason; }
    public void setRevokeReason(String revokeReason) { this.revokeReason = revokeReason; }
}

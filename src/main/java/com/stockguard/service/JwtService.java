package com.stockguard.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Stateless JWT service.
 * Every token carries a unique 'jti' (JWT ID) claim used by the blocklist
 * to support server-side logout/revocation without storing all tokens.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${stockguard.jwt.secret}")
    private String secretKey;

    @Value("${stockguard.jwt.expiration-ms}")
    private long jwtExpiration;

    // ---- Public API ----

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Extract the unique token ID (jti) — used for blocklist lookups */
    public String extractJti(String token) {
        return extractClaim(token, claims -> claims.get("jti", String.class));
    }

    public Long extractShopId(String token) {
        return extractClaim(token, claims -> {
            Object shopId = claims.get("shopId");
            if (shopId instanceof Integer) return ((Integer) shopId).longValue();
            if (shopId instanceof Long) return (Long) shopId;
            return null;
        });
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generate a signed JWT for the given user, embedding shopId, role, and a unique jti.
     */
    public String generateToken(String username, Long shopId, String role) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("shopId", shopId);
        extraClaims.put("role", role);
        extraClaims.put("jti", UUID.randomUUID().toString()); // unique token ID for blocklist
        return buildToken(extraClaims, username, jwtExpiration);
    }

    /**
     * Validates the token signature, expiry, and username match.
     * The caller (JwtAuthenticationFilter) is responsible for additionally
     * checking the blocklist.
     */
    public boolean isTokenValid(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            return extractedUsername.equals(username) && !isTokenExpired(token);
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // ---- Private helpers ----

    private String buildToken(Map<String, Object> extraClaims, String username, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            if (keyBytes.length < 32) {
                throw new IllegalStateException(
                        "JWT secret is too short (< 256 bits). " +
                        "Generate a secure key with: openssl rand -base64 32");
            }
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "JWT secret is not valid Base64. Set a proper JWT_SECRET environment variable.", e);
        }
    }
}

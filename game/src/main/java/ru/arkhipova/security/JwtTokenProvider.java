package ru.arkhipova.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Creates, signs, and validates JWT access tokens for registered users.
 *
 * <p>The system has no guest mode — every token is bound to a user UUID via the {@code userId} claim.
 * Token subject is always {@code "USER"} (kept for forward compatibility with role-based scopes).
 */
@Component
@Slf4j
public class JwtTokenProvider {

    public static final String SUBJECT_USER = "USER";

    private final SecretKey key;
    private final long expiration;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * Generates a signed JWT token for a registered user.
     */
    public String generateToken(UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(SUBJECT_USER)
                .claim("userId", userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Extracts the user identifier from a token. Returns {@code null} if the claim is missing or
     * malformed, so the caller can treat it as "no auth" instead of bombing with a 500.
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = parse(token);
        String userIdRaw = claims.get("userId", String.class);
        if (userIdRaw == null) {
            return null;
        }
        try {
            return UUID.fromString(userIdRaw);
        } catch (IllegalArgumentException ex) {
            log.debug("JWT userId claim is not a valid UUID: {}", userIdRaw);
            return null;
        }
    }

    /**
     * Validates token signature and expiration.
     */
    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}

package com.devlog.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtProvider.class);

    private static final String ISSUER = "devlog";
    private static final String AUDIENCE = "devlog-api";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_TYPE = "typ";
    private static final long CLOCK_SKEW_SECONDS = 30L;
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration accessTokenExpiration;
    private final Duration refreshTokenExpiration;

    public JwtProvider(JwtProperties properties) {
        byte[] keyBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret must be at least " + MIN_SECRET_BYTES
                            + " bytes (UTF-8), got " + keyBytes.length);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiration = properties.accessTokenExpiration();
        this.refreshTokenExpiration = properties.refreshTokenExpiration();
    }

    public String generateAccessToken(Long userId, String email) {
        return buildToken(userId, email, TokenType.ACCESS, accessTokenExpiration);
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration.toSeconds();
    }

    public String generateRefreshToken(Long userId, String email) {
        return buildToken(userId, email, TokenType.REFRESH, refreshTokenExpiration);
    }

    public TokenValidationResult validateAccessToken(String token) {
        return validate(token, TokenType.ACCESS);
    }

    public TokenValidationResult validateRefreshToken(String token) {
        return validate(token, TokenType.REFRESH);
    }

    public Long getUserId(String token) {
        String subject = parseClaims(token).getSubject();
        if (subject == null) {
            throw new IllegalStateException("JWT subject (userId) is missing");
        }
        return Long.parseLong(subject);
    }

    public String getEmail(String token) {
        return parseClaims(token).get(CLAIM_EMAIL, String.class);
    }

    private String buildToken(Long userId, String email, TokenType type, Duration expiration) {
        Instant now = Instant.now();
        Instant exp = now.plus(expiration);
        return Jwts.builder()
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_TYPE, type.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private TokenValidationResult validate(String token, TokenType expectedType) {
        try {
            Claims claims = parseClaims(token);
            String typ = claims.get(CLAIM_TYPE, String.class);
            if (!expectedType.name().equals(typ)) {
                log.warn("JWT type mismatch: expected={}, actual={}, prefix={}",
                        expectedType, typ, maskToken(token));
                return TokenValidationResult.INVALID;
            }
            return TokenValidationResult.VALID;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: prefix={}", maskToken(token));
            return TokenValidationResult.EXPIRED;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT invalid: prefix={}, reason={}", maskToken(token), e.getClass().getSimpleName());
            return TokenValidationResult.INVALID;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .requireAudience(AUDIENCE)
                .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 8) + "...";
    }
}

package com.example.sanitationassessment.auth;

import com.example.sanitationassessment.config.JwtProperties;
import com.example.sanitationassessment.domain.UserRole;
import com.example.sanitationassessment.dto.user.SystemUserResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final Duration expiration;

    public JwtTokenProvider(JwtProperties properties) {
        byte[] keyBytes = Decoders.BASE64.decode(properties.secret());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expiration = properties.expiration();
    }

    public String generateToken(SystemUserResponse user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiration);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public AuthenticatedUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Long userId = ((Number) claims.get("userId")).longValue();

        String username = claims.getSubject();

        UserRole role = UserRole.valueOf(claims.get("role", String.class));

        String tokenId = claims.getId();
        if (tokenId == null || tokenId.isBlank()) {
            throw new MalformedJwtException(
                    "JWT token id is missing"
            );
        }
        Instant expiresAt =
                claims.getExpiration().toInstant();

        return new AuthenticatedUser(userId, username, role, tokenId, expiresAt);
    }
}
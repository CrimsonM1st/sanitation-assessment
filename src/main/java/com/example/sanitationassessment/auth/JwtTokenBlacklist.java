package com.example.sanitationassessment.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class JwtTokenBlacklist {

    private static final String KEY_PREFIX =
            "sanitation:auth:token:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public JwtTokenBlacklist(
            StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void revoke(
            String tokenId,
            Instant expiresAt) {

        Duration remaining =
                Duration.between(Instant.now(), expiresAt);

        if (remaining.isNegative() || remaining.isZero()) {
            return;
        }

        redisTemplate.opsForValue().set(
                KEY_PREFIX + tokenId,
                "1",
                remaining
        );
    }

    public boolean isRevoked(String tokenId) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(KEY_PREFIX + tokenId)
        );
    }
}

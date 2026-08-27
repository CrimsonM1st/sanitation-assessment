package com.example.sanitationassessment.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtTokenBlacklistTest {
    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private JwtTokenBlacklist jwtTokenBlacklist;

    @Test
    void validTokenShouldBeStoredWithRemainingTtl() {
        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        Instant expiresAt = Instant.now().plusSeconds(120);

        jwtTokenBlacklist.revoke("token-1", expiresAt);

        ArgumentCaptor<Duration> ttlCaptor =
                ArgumentCaptor.forClass(Duration.class);

        verify(valueOperations).set(
                eq("sanitation:auth:token:blacklist:token-1"),
                eq("1"),
                ttlCaptor.capture()
        );
        long seconds = ttlCaptor.getValue().toSeconds();

        assertTrue(seconds >= 118);
        assertTrue(seconds <= 120);
    }

    @Test
    void expiredTokenShouldNotBeStored() {
        jwtTokenBlacklist.revoke(
                "expired-token",
                Instant.now().minusSeconds(1)
        );

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void existingBlacklistKeyShouldBeReportedAsRevoked() {
        when(redisTemplate.hasKey(
                "sanitation:auth:token:blacklist:token-1"))
                .thenReturn(true);

        assertTrue(jwtTokenBlacklist.isRevoked("token-1"));
    }

    @Test
    void missingBlacklistKeyShouldBeReportedAsNotRevoked() {
        when(redisTemplate.hasKey(
                "sanitation:auth:token:blacklist:token-1"))
                .thenReturn(false);
        assertFalse(jwtTokenBlacklist.isRevoked("token-1"));
    }
}

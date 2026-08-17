package com.example.sanitationassessment.lock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisLockTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisLock redisLock;

    @Test
    void redisLockAddSuccess() {
        when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(
                eq("lock:test"),
                anyString(),
                eq(Duration.ofSeconds(10))
        )).thenReturn(true);
        String token = redisLock.tryLock(
                "lock:test",
                Duration.ofSeconds(10)
        );
        assertNotNull(token);
    }

    @Test
    void redisLockAddFailed() {
        when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(
                eq("lock:test"),
                anyString(),
                eq(Duration.ofSeconds(10))
        )).thenReturn(false);
        String token = redisLock.tryLock(
                "lock:test",
                Duration.ofSeconds(10)
        );
        assertNull(token);
    }

    @Test
    void correctTokenUnlockSuccess() {
        when(stringRedisTemplate.<Long>execute(
                ArgumentMatchers.any(),
                eq(List.of("lock:test")),
                eq("my-token")
        )).thenReturn(1L);
        assertTrue(redisLock.unlock(
                "lock:test",
                "my-token"
        ));
    }

    @Test
    void incorrectTokenUnlockFailed() {
        when(stringRedisTemplate.<Long>execute(
                ArgumentMatchers.any(),
                eq(List.of("lock:test")),
                eq("wrong-token")
        )).thenReturn(0L);
        assertFalse(redisLock.unlock(
                "lock:test",
                "wrong-token"
        ));
    }
}
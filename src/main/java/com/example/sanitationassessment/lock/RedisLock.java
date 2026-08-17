package com.example.sanitationassessment.lock;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
public class RedisLock {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisLock(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String tryLock(String key, Duration timeout) {
        String token = UUID.randomUUID().toString();

        Boolean success = stringRedisTemplate
                .opsForValue()
                .setIfAbsent(key, token, timeout);

        if (Boolean.TRUE.equals(success)) {
            return token;
        }

        return null;
    }

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT =
            new DefaultRedisScript<>(
                    """
                            if redis.call('get', KEYS[1]) == ARGV[1] then
                                return redis.call('del', KEYS[1])
                            else
                                return 0
                            end
                            """,
                    Long.class
            );

    public boolean unlock(String key, String token) {
        Long result = stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                List.of(key),
                token
        );

        return Long.valueOf(1L).equals(result);
    }
}

package com.example.sanitationassessment.cache;

import com.example.sanitationassessment.domain.AssessmentTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class AssessmentTaskCache {
    private static final Logger log =
            LoggerFactory.getLogger(AssessmentTaskCache.class);

    private static final String KEY_PREFIX =
            "sanitation:assessment-task:";

    private static final Duration BASE_TTL =
            Duration.ofMinutes(10);

    private static final String NULL_VALUE = "__NULL__";

    private static final Duration NULL_TTL =
            Duration.ofMinutes(1);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public AssessmentTaskCache(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    private String buildKey(Long id) {
        return KEY_PREFIX + id;
    }

    private Duration normalTtl() {
        long randomSeconds = ThreadLocalRandom.current()
                .nextLong(0, 121);

        return BASE_TTL.plusSeconds(randomSeconds);
    }

    public AssessmentTaskCacheResult get(Long id) {
        String key = buildKey(id);

        try {
            String json = stringRedisTemplate
                    .opsForValue()
                    .get(key);

            if (!StringUtils.hasText(json)) {
                return AssessmentTaskCacheResult.miss();
            }
            if (NULL_VALUE.equals(json)) {
                return AssessmentTaskCacheResult.hit(null);
            }

            AssessmentTask task = objectMapper.readValue(
                    json,
                    AssessmentTask.class
            );

            return AssessmentTaskCacheResult.hit(task);
        } catch (Exception exception) {
            log.warn("读取任务缓存失败，key={}", key, exception);
            return AssessmentTaskCacheResult.miss();
        }
    }

    public void put(AssessmentTask task) {
        String key = buildKey(task.getId());
        try {
            String json = objectMapper.writeValueAsString(task);
            stringRedisTemplate.opsForValue().set(key, json, normalTtl());
        } catch (Exception exception) {
            log.warn("写入任务缓存失败，key={}", key, exception);
        }

    }

    public void evict(Long id) {
        String key = buildKey(id);
        try {
            stringRedisTemplate.delete(key);
        } catch (RuntimeException exception) {
            log.warn("删除任务缓存失败，key={}", key, exception);
        }
    }

    public void putNull(Long id) {
        String key = buildKey(id);

        try {
            stringRedisTemplate.opsForValue().set(
                    key,
                    NULL_VALUE,
                    NULL_TTL
            );
        } catch (RuntimeException exception) {
            log.warn("写入任务空值缓存失败，key={}", key, exception);
        }
    }
}

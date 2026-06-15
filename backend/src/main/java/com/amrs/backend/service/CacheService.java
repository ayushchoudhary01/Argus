package com.amrs.backend.service;

import com.amrs.backend.model.DivergenceReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration TTL = Duration.ofHours(24);

    public DivergenceReport get(String key) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached == null) return null;
            return objectMapper.convertValue(cached, DivergenceReport.class);
        } catch (Exception e) {
            log.warn("Cache read failed for key={}", key, e);
            return null;
        }
    }

    public void put(String key, DivergenceReport report) {
        try {
            redisTemplate.opsForValue().set(key, report, TTL);
        } catch (Exception e) {
            log.warn("Cache write failed for key={}", key, e);
        }
    }
}

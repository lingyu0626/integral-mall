package com.integral.mall.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class RedisCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public RedisCacheService(ObjectMapper objectMapper,
                             org.springframework.beans.factory.ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    public <T> T getOrLoad(String key, Duration ttl, TypeReference<T> typeRef, Supplier<T> loader) {
        if (!StringUtils.hasText(key) || loader == null) return loader.get();
        if (redisTemplate == null) return loader.get();

        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (StringUtils.hasText(cached)) {
                return objectMapper.readValue(cached, typeRef);
            }
        } catch (Exception e) {
            log.warn("读取Redis缓存失败 key={}: {}", key, e.getMessage());
        }

        T fresh = loader.get();
        try {
            String payload = objectMapper.writeValueAsString(fresh);
            long seconds = ttl == null ? 60L : Math.max(5L, ttl.getSeconds());
            redisTemplate.opsForValue().set(key, payload, seconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入Redis缓存失败 key={}: {}", key, e.getMessage());
        }
        return fresh;
    }

    public void evictByPrefix(String prefix) {
        if (!StringUtils.hasText(prefix) || redisTemplate == null) return;
        try {
            Set<String> keys = redisTemplate.keys(prefix + "*");
            if (keys == null || keys.isEmpty()) return;
            redisTemplate.delete(keys);
        } catch (Exception e) {
            log.warn("删除Redis缓存失败 prefix={}: {}", prefix, e.getMessage());
        }
    }
}


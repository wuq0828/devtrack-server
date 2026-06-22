package com.nx.devtrack.app.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 登录态(生产)。key: devtrack:login_token:{token} -> userId,带 TTL。
 * 仅当 devtrack.token.store=redis 时启用(生产 profile)。
 */
@Component
@ConditionalOnProperty(name = "devtrack.token.store", havingValue = "redis")
public class RedisTokenStore implements TokenStore {

    private static final String PREFIX = "devtrack:login_token:";

    private final StringRedisTemplate redisTemplate;

    public RedisTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(String token, Long userId, long ttlSeconds) {
        redisTemplate.opsForValue().set(PREFIX + token, String.valueOf(userId), Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public Long getUserId(String token) {
        String v = redisTemplate.opsForValue().get(PREFIX + token);
        return v == null ? null : Long.valueOf(v);
    }

    @Override
    public void remove(String token) {
        redisTemplate.delete(PREFIX + token);
    }
}

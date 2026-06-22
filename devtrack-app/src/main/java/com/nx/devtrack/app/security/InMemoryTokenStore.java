package com.nx.devtrack.app.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存登录态(本地默认,零依赖)。生产用 RedisTokenStore。
 */
@Component
@ConditionalOnProperty(name = "devtrack.token.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryTokenStore implements TokenStore {

    private record Entry(Long userId, long expireAtMillis) {
    }

    private final ConcurrentHashMap<String, Entry> map = new ConcurrentHashMap<>();

    @Override
    public void save(String token, Long userId, long ttlSeconds) {
        map.put(token, new Entry(userId, System.currentTimeMillis() + ttlSeconds * 1000));
    }

    @Override
    public Long getUserId(String token) {
        Entry e = map.get(token);
        if (e == null) {
            return null;
        }
        if (e.expireAtMillis() < System.currentTimeMillis()) {
            map.remove(token);
            return null;
        }
        return e.userId();
    }

    @Override
    public void remove(String token) {
        map.remove(token);
    }
}

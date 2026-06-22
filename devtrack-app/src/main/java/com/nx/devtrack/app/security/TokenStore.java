package com.nx.devtrack.app.security;

/**
 * 登录态缓存。token -> userId,带 TTL。
 * 对齐 nx-skyline 的"Redis 缓存 + DB 兜底"模式:本地用内存实现零依赖,生产切 Redis。
 */
public interface TokenStore {

    void save(String token, Long userId, long ttlSeconds);

    /** 命中返回 userId,未命中/已过期返回 null */
    Long getUserId(String token);

    void remove(String token);
}

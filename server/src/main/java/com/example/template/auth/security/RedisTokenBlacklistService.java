package com.example.template.auth.security;

import com.example.template.auth.config.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 基于 Redis 的 {@link TokenBlacklistService} 实现。
 *
 * <p>key 形如 {@code {redis-prefix}:blacklist:{jti}}，value 占位 "1"，TTL=令牌剩余有效期。
 * 不打印 token 明文；Redis 异常统一抛 {@link RedisUnavailableException} 供上层 fail-closed。</p>
 */
@Service
public class RedisTokenBlacklistService implements TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenBlacklistService.class);
    private static final String BLACKLIST_SEGMENT = ":blacklist:";
    private static final String PRESENT = "1";

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;
    private final boolean enabled;

    public RedisTokenBlacklistService(StringRedisTemplate redisTemplate, AuthProperties authProperties) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = authProperties.getToken().getRedisPrefix();
        this.enabled = authProperties.getToken().isEnableBlacklist();
    }

    @Override
    public void blacklist(String jti, long ttlSeconds) {
        if (!enabled || !StringUtils.hasText(jti) || ttlSeconds <= 0) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(buildKey(jti), PRESENT, Duration.ofSeconds(ttlSeconds));
        } catch (RuntimeException e) {
            log.error("写入 token 黑名单失败：{}", e.getMessage());
            throw new RedisUnavailableException("token 黑名单写入失败", e);
        }
    }

    @Override
    public boolean isBlacklisted(String jti) {
        if (!enabled) {
            return false;
        }
        if (!StringUtils.hasText(jti)) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(jti)));
        } catch (RuntimeException e) {
            log.error("查询 token 黑名单失败：{}", e.getMessage());
            throw new RedisUnavailableException("token 黑名单查询失败", e);
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private String buildKey(String jti) {
        return keyPrefix + BLACKLIST_SEGMENT + jti;
    }
}

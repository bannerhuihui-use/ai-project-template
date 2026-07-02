package com.example.template.auth.security;

/**
 * Token 黑名单服务：按 jti 维度让单个令牌立即失效。
 *
 * <p>黑名单 TTL 与令牌剩余有效期一致，令牌自然过期后 key 随之消失。
 * 通过 {@link #isEnabled()} 暴露开关：关闭时鉴权不查黑名单、拉黑为 no-op，
 * 由上层回退为 {@code token_version} 自增（整账号失效）。</p>
 */
public interface TokenBlacklistService {

    /**
     * 将令牌 jti 加入黑名单。
     *
     * @param jti        令牌唯一 ID
     * @param ttlSeconds 黑名单存活秒数（令牌剩余有效期）；&lt;=0 表示已过期，忽略
     * @throws RedisUnavailableException Redis 异常
     */
    void blacklist(String jti, long ttlSeconds);

    /**
     * 判断令牌 jti 是否在黑名单中。
     *
     * @param jti 令牌唯一 ID
     * @return true 表示已被拉黑
     * @throws RedisUnavailableException Redis 异常（鉴权侧据此 fail-closed）
     */
    boolean isBlacklisted(String jti);

    /**
     * 黑名单是否启用。
     *
     * @return true 表示启用 Redis 黑名单
     */
    boolean isEnabled();
}

package com.example.template.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 鉴权配置（前缀 auth）。
 */
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private Jwt jwt = new Jwt();
    private Token token = new Token();
    private Whitelist whitelist = new Whitelist();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public Whitelist getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(Whitelist whitelist) {
        this.whitelist = whitelist;
    }

    /**
     * JWT 配置。
     */
    public static class Jwt {
        /** 签名密钥；生产必须通过环境变量注入，禁止默认值。 */
        private String secret;
        /** access token 有效期（秒），默认 2 小时。 */
        private long accessExpireSeconds = 7200;
        /** refresh token 有效期（秒），默认 7 天。 */
        private long refreshExpireSeconds = 604800;
        /** 签发者。 */
        private String issuer = "template-server";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getAccessExpireSeconds() {
            return accessExpireSeconds;
        }

        public void setAccessExpireSeconds(long accessExpireSeconds) {
            this.accessExpireSeconds = accessExpireSeconds;
        }

        public long getRefreshExpireSeconds() {
            return refreshExpireSeconds;
        }

        public void setRefreshExpireSeconds(long refreshExpireSeconds) {
            this.refreshExpireSeconds = refreshExpireSeconds;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
    }

    /**
     * Token 黑名单 / Redis 配置。
     */
    public static class Token {
        /** Redis key 前缀；黑名单 key 形如 {prefix}:blacklist:{jti}。 */
        private String redisPrefix = "auth:token";
        /** 是否启用 Redis 黑名单；关闭后 logout 回退为 token_version 自增。 */
        private boolean enableBlacklist = true;

        public String getRedisPrefix() {
            return redisPrefix;
        }

        public void setRedisPrefix(String redisPrefix) {
            this.redisPrefix = redisPrefix;
        }

        public boolean isEnableBlacklist() {
            return enableBlacklist;
        }

        public void setEnableBlacklist(boolean enableBlacklist) {
            this.enableBlacklist = enableBlacklist;
        }
    }

    /**
     * 白名单配置。
     */
    public static class Whitelist {
        /** 无需鉴权即可访问的路径（Ant 风格）。 */
        private List<String> paths = new ArrayList<>();

        public List<String> getPaths() {
            return paths;
        }

        public void setPaths(List<String> paths) {
            this.paths = paths;
        }
    }
}

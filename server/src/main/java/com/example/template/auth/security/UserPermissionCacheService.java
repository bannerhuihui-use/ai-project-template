package com.example.template.auth.security;

import com.example.template.auth.repository.PermissionRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户按钮/接口权限码短时缓存，减轻 Jwt 鉴权每请求查库压力。
 *
 * <p>RBAC 变更后应调用 {@link #invalidateAll()} 或 {@link #invalidateUser(Long)} 失效缓存；
 * 同时业务侧仍会自增 token_version，旧令牌无法续用。</p>
 */
@Service
public class UserPermissionCacheService {

    private static final int TTL_MINUTES = 2;
    private static final int MAX_ENTRIES = 10_000;

    private final PermissionRepository permissionRepository;
    private final Cache<Long, List<String>> cache = Caffeine.newBuilder()
            .expireAfterWrite(TTL_MINUTES, TimeUnit.MINUTES)
            .maximumSize(MAX_ENTRIES)
            .build();

    public UserPermissionCacheService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    /**
     * 获取用户按钮/接口权限码（带缓存）。
     */
    public List<String> getButtonApiCodes(Long userId) {
        return cache.get(userId, permissionRepository::findButtonApiCodesByUserId);
    }

    public void invalidateUser(Long userId) {
        if (userId != null) {
            cache.invalidate(userId);
        }
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }
}

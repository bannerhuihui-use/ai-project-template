package com.example.template.auth.service;

/**
 * 管理员账号管理（v1.2.1：禁用、强制下线）。所有高风险动作统一收敛到 token_version 自增。
 */
public interface AuthAdminService {

    /**
     * 禁用账号：置 status=DISABLED 并自增 token_version（旧令牌全失效）。幂等。
     *
     * @param targetUserId 目标用户 ID
     */
    void disableUser(Long targetUserId);

    /**
     * 强制下线：仅自增 token_version 使旧令牌全失效，不改变账号状态。幂等。
     *
     * @param targetUserId 目标用户 ID
     */
    void forceLogout(Long targetUserId);
}

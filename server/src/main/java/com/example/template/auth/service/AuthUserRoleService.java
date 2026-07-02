package com.example.template.auth.service;

import java.util.List;

/**
 * 管理端：用户角色配置。
 */
public interface AuthUserRoleService {

    /**
     * 覆盖式设置用户角色：单事务内先清空再写入，并使该用户 token 失效（重新加载角色/权限）。
     *
     * @param userId    用户 ID
     * @param roleCodes 角色编码列表（空=清空）
     */
    void replaceUserRoles(Long userId, List<String> roleCodes);
}

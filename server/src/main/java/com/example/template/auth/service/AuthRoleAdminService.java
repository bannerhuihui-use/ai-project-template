package com.example.template.auth.service;

import com.example.template.auth.dto.RoleCreateRequest;
import com.example.template.auth.dto.RoleItem;

/**
 * 管理端角色维护（新建等）。
 */
public interface AuthRoleAdminService {

    /**
     * 新建角色（默认状态 NORMAL，无初始权限）。
     *
     * @param request 角色编码与名称
     * @return 新建角色信息
     */
    RoleItem createRole(RoleCreateRequest request);
}

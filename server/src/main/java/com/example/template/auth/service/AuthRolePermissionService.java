package com.example.template.auth.service;

import com.example.template.auth.dto.PermissionItem;
import com.example.template.common.PageResult;

import java.util.List;

/**
 * 管理端：权限点查询与角色权限配置。
 */
public interface AuthRolePermissionService {

    /**
     * 分页查询权限点。
     *
     * @param type     权限类型过滤（可空）
     * @param keyword  编码/名称模糊（可空）
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageResult<PermissionItem> pagePermissions(String type, String keyword, int pageNum, int pageSize);

    /**
     * 查询角色已绑定的权限。
     *
     * @param roleId 角色 ID
     * @return 权限列表
     */
    List<PermissionItem> getRolePermissions(Long roleId);

    /**
     * 覆盖式设置角色权限：单事务内先清空再写入，并使该角色下所有用户 token 失效。
     *
     * @param roleId          角色 ID
     * @param permissionCodes 权限编码列表（空=清空）
     */
    void replaceRolePermissions(Long roleId, List<String> permissionCodes);
}

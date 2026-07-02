package com.example.template.auth.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户角色关联数据访问。覆盖式更新的事务边界由 Service 控制。
 */
@Repository
public class SysUserRoleRepository {

    private final JdbcTemplate jdbcTemplate;

    public SysUserRoleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 软删除用户的全部角色绑定（覆盖式更新第一步）。
     *
     * @param userId 用户 ID
     * @return 受影响行数
     */
    public int softDeleteByUserId(Long userId) {
        return jdbcTemplate.update(
                "UPDATE sys_user_role SET deleted = 1 WHERE user_id = ? AND deleted = 0", userId);
    }

    /**
     * 批量插入用户角色绑定（覆盖式更新第二步）。
     *
     * @param userId  用户 ID
     * @param roleIds 角色 ID 列表
     */
    public void insertUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)",
                roleIds,
                roleIds.size(),
                (ps, roleId) -> {
                    ps.setLong(1, userId);
                    ps.setLong(2, roleId);
                });
    }

    /**
     * 查询用户当前绑定的角色 ID（未删除）。
     *
     * @param userId 用户 ID
     * @return 角色 ID 列表
     */
    public List<Long> findRoleIdsByUserId(Long userId) {
        return jdbcTemplate.queryForList(
                "SELECT role_id FROM sys_user_role WHERE user_id = ? AND deleted = 0", Long.class, userId);
    }
}

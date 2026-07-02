package com.example.template.auth.repository;

import com.example.template.auth.dto.RoleItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 角色数据访问。
 */
@Repository
public class SysRoleRepository {

    private final JdbcTemplate jdbcTemplate;

    public SysRoleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询用户拥有的、正常状态的角色编码。
     *
     * @param userId 用户 ID
     * @return 角色编码列表
     */
    public List<String> findRoleCodesByUserId(Long userId) {
        String sql = "SELECT r.role_code FROM sys_role r "
                + "JOIN sys_user_role ur ON ur.role_id = r.id "
                + "WHERE ur.user_id = ? AND ur.deleted = 0 AND r.deleted = 0 AND r.status = 'NORMAL' "
                + "ORDER BY r.role_code";
        return jdbcTemplate.queryForList(sql, String.class, userId);
    }

    /**
     * 统计拥有指定角色编码的有效用户数。
     */
    public long countUsersWithRoleCode(String roleCode) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT count(DISTINCT ur.user_id) FROM sys_user_role ur "
                        + "JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.status = 'NORMAL' "
                        + "JOIN sys_user u ON u.id = ur.user_id AND u.deleted = 0 "
                        + "WHERE ur.deleted = 0 AND r.role_code = ?",
                Long.class, roleCode);
        return count == null ? 0L : count;
    }

    /**
     * 查询全部有效角色（下拉列表用）。
     */
    public List<RoleItem> findAllActiveRoles() {
        return jdbcTemplate.query(
                "SELECT id, role_code, role_name FROM sys_role "
                        + "WHERE deleted = 0 AND status = 'NORMAL' ORDER BY role_code",
                (rs, rowNum) -> new RoleItem(rs.getLong("id"), rs.getString("role_code"),
                        rs.getString("role_name")));
    }

    /**
     * 按角色编码批量查询有效角色 ID（仅 deleted=0 且 status=NORMAL）。
     * 用于用户角色覆盖时的 code→id 映射与合法性校验。
     *
     * @param roleCodes 角色编码集合
     * @return 有效角色 ID 列表
     */
    public List<Long> findActiveIdsByCodes(Collection<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Collections.emptyList();
        }
        String placeholders = String.join(",", Collections.nCopies(roleCodes.size(), "?"));
        String sql = "SELECT id FROM sys_role"
                + " WHERE deleted = 0 AND status = 'NORMAL' AND role_code IN (" + placeholders + ")";
        return jdbcTemplate.queryForList(sql, Long.class, roleCodes.toArray());
    }

    /**
     * 判断角色是否存在且有效（用于角色权限配置前置校验）。
     *
     * @param roleId 角色 ID
     * @return 存在且未删除返回 true
     */
    public boolean existsActiveById(Long roleId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM sys_role WHERE id = ? AND deleted = 0", Integer.class, roleId);
        return count != null && count > 0;
    }

    /**
     * 判断角色编码是否已存在（未删除）。
     */
    public boolean existsActiveByCode(String roleCode) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM sys_role WHERE role_code = ? AND deleted = 0", Integer.class, roleCode);
        return count != null && count > 0;
    }

    /**
     * 新建角色，默认状态 NORMAL。
     *
     * @return 新角色 ID
     */
    public Long insert(String roleCode, String roleName) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO sys_role (role_code, role_name, status) VALUES (?, ?, 'NORMAL')",
                    new String[]{"id"});
            ps.setString(1, roleCode);
            ps.setString(2, roleName);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("insert sys_role failed: no generated key");
        }
        return key.longValue();
    }
}

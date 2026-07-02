package com.example.template.auth.repository;

import com.example.template.auth.dto.UserAdminItem;
import com.example.template.auth.model.SysUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 用户主体数据访问。
 */
@Repository
public class SysUserRepository {

    private static final RowMapper<SysUser> ROW_MAPPER = (rs, rowNum) -> {
        SysUser user = new SysUser();
        user.setId(rs.getLong("id"));
        user.setUserType(rs.getString("user_type"));
        user.setNickname(rs.getString("nickname"));
        user.setAvatarUrl(rs.getString("avatar_url"));
        user.setPhone(rs.getString("phone"));
        user.setEmail(rs.getString("email"));
        user.setStatus(rs.getString("status"));
        user.setTokenVersion(rs.getInt("token_version"));
        user.setDeleted(rs.getInt("deleted"));
        return user;
    };

    private static final RowMapper<UserAdminItem> ADMIN_ITEM_MAPPER = (rs, rowNum) -> {
        UserAdminItem item = new UserAdminItem();
        item.setUserId(rs.getLong("id"));
        item.setUsername(rs.getString("username"));
        item.setNickname(rs.getString("nickname"));
        item.setEmail(rs.getString("email"));
        item.setPhone(rs.getString("phone"));
        item.setStatus(rs.getString("status"));
        item.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        item.setLastLoginTime(toLocalDateTime(rs.getTimestamp("last_login_time")));
        String roleCodes = rs.getString("role_codes");
        if (StringUtils.hasText(roleCodes)) {
            item.setRoles(Arrays.asList(roleCodes.split(",")));
        } else {
            item.setRoles(List.of());
        }
        return item;
    };

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    private final JdbcTemplate jdbcTemplate;

    public SysUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 按主键查询未删除用户。
     *
     * @param id 用户 ID
     * @return 用户
     */
    public Optional<SysUser> findActiveById(Long id) {
        String sql = "SELECT id, user_type, nickname, avatar_url, phone, email, status, token_version, deleted "
                + "FROM sys_user WHERE id = ? AND deleted = 0 LIMIT 1";
        List<SysUser> list = jdbcTemplate.query(sql, ROW_MAPPER, id);
        return list.stream().findFirst();
    }

    /**
     * 分页查询后台用户（仅 ADMIN 类型，含用户名与角色聚合）。
     *
     * @param username 用户名模糊（可空）
     * @param status   状态（可空）
     * @param roleCode 角色编码（可空）
     * @param offset   偏移
     * @param limit    条数
     * @return 用户列表
     */
    public List<UserAdminItem> findAdminPage(String username, String status, String roleCode, int offset, int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT u.id, ui.identifier AS username, u.nickname, u.email, u.phone, u.status, "
                        + "u.created_at, u.last_login_time, "
                        + "(SELECT string_agg(r.role_code, ',' ORDER BY r.role_code) "
                        + " FROM sys_role r JOIN sys_user_role ur ON ur.role_id = r.id AND ur.deleted = 0 "
                        + " WHERE ur.user_id = u.id AND r.deleted = 0) AS role_codes "
                        + "FROM sys_user u "
                        + "JOIN user_identity ui ON ui.user_id = u.id AND ui.identity_type = 'USERNAME' "
                        + "AND ui.deleted = 0 "
                        + "WHERE u.deleted = 0 AND u.user_type = 'ADMIN' ");
        List<Object> params = new ArrayList<>();
        appendAdminFilters(sql, params, username, status, roleCode);
        sql.append("ORDER BY u.id DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), ADMIN_ITEM_MAPPER, params.toArray());
    }

    /**
     * 统计后台用户总数（与 {@link #findAdminPage} 相同过滤条件）。
     */
    public long countAdminPage(String username, String status, String roleCode) {
        StringBuilder sql = new StringBuilder(
                "SELECT count(*) FROM sys_user u "
                        + "JOIN user_identity ui ON ui.user_id = u.id AND ui.identity_type = 'USERNAME' "
                        + "AND ui.deleted = 0 "
                        + "WHERE u.deleted = 0 AND u.user_type = 'ADMIN' ");
        List<Object> params = new ArrayList<>();
        appendAdminFilters(sql, params, username, status, roleCode);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count == null ? 0L : count;
    }

    private void appendAdminFilters(StringBuilder sql, List<Object> params,
                                    String username, String status, String roleCode) {
        if (StringUtils.hasText(username)) {
            sql.append("AND ui.identifier ILIKE ? ");
            params.add("%" + username.trim() + "%");
        }
        if (StringUtils.hasText(status)) {
            sql.append("AND u.status = ? ");
            params.add(status.trim());
        }
        if (StringUtils.hasText(roleCode)) {
            sql.append("AND EXISTS (SELECT 1 FROM sys_user_role ur "
                    + "JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0 "
                    + "WHERE ur.user_id = u.id AND ur.deleted = 0 AND r.role_code = ?) ");
            params.add(roleCode.trim());
        }
    }

    /**
     * 查询单个后台用户详情（含用户名与角色）。
     */
    public Optional<UserAdminItem> findAdminItemById(Long userId) {
        String sql = "SELECT u.id, ui.identifier AS username, u.nickname, u.email, u.phone, u.status, "
                + "u.created_at, u.last_login_time, "
                + "(SELECT string_agg(r.role_code, ',' ORDER BY r.role_code) "
                + " FROM sys_role r JOIN sys_user_role ur ON ur.role_id = r.id AND ur.deleted = 0 "
                + " WHERE ur.user_id = u.id AND r.deleted = 0) AS role_codes "
                + "FROM sys_user u "
                + "JOIN user_identity ui ON ui.user_id = u.id AND ui.identity_type = 'USERNAME' "
                + "AND ui.deleted = 0 "
                + "WHERE u.deleted = 0 AND u.user_type = 'ADMIN' AND u.id = ? LIMIT 1";
        List<UserAdminItem> list = jdbcTemplate.query(sql, ADMIN_ITEM_MAPPER, userId);
        return list.stream().findFirst();
    }

    /**
     * 新建后台用户主体。
     *
     * @param nickname 昵称
     * @param email    邮箱
     * @param phone    手机号
     * @return 新用户 ID
     */
    public Long insertAdminUser(String nickname, String email, String phone) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO sys_user (user_type, nickname, email, phone, status) "
                            + "VALUES ('ADMIN', ?, ?, ?, 'NORMAL')",
                    new String[]{"id"});
            ps.setString(1, nickname);
            ps.setString(2, email);
            ps.setString(3, phone);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("insert sys_user failed: no generated key");
        }
        return key.longValue();
    }

    /**
     * 更新后台用户基本信息（不含状态；状态单独更新以便控制 token 失效策略）。
     */
    public int updateProfile(Long id, String nickname, String email, String phone) {
        return jdbcTemplate.update(
                "UPDATE sys_user SET nickname = ?, email = ?, phone = ?, updated_at = now() WHERE id = ? AND deleted = 0",
                nickname, email, phone, id);
    }

    /**
     * 更新最后登录时间。
     *
     * @param id 用户 ID
     */
    public void updateLastLoginTime(Long id) {
        jdbcTemplate.update(
                "UPDATE sys_user SET last_login_time = now(), updated_at = now() WHERE id = ?", id);
    }

    /**
     * 自增 token 版本号：使该用户此前签发的所有令牌（access + refresh）立即失效。
     * 用于退出登录（黑名单关闭时）、修改密码、禁用用户、强制下线等场景。
     *
     * @param id 用户 ID
     * @return 受影响行数
     */
    public int incrementTokenVersion(Long id) {
        return jdbcTemplate.update(
                "UPDATE sys_user SET token_version = token_version + 1, updated_at = now() WHERE id = ?", id);
    }

    /**
     * 按角色批量自增 token 版本号：使该角色下所有用户此前签发的令牌立即失效。
     * 用于角色权限变更后让相关用户重新加载权限。
     *
     * @param roleId 角色 ID
     * @return 受影响行数（受影响用户数）
     */
    public int incrementTokenVersionByRoleId(Long roleId) {
        return jdbcTemplate.update(
                "UPDATE sys_user SET token_version = token_version + 1, updated_at = now() "
                        + "WHERE deleted = 0 AND id IN ("
                        + "SELECT user_id FROM sys_user_role WHERE role_id = ? AND deleted = 0)",
                roleId);
    }

    /**
     * 更新账号状态（如禁用）。
     *
     * @param id     用户 ID
     * @param status 目标状态
     * @return 受影响行数
     */
    public int updateStatus(Long id, String status) {
        return jdbcTemplate.update(
                "UPDATE sys_user SET status = ?, updated_at = now() WHERE id = ?", status, id);
    }

    /**
     * 更新密码最后修改时间。
     *
     * @param id 用户 ID
     * @return 受影响行数
     */
    public int updatePasswordUpdatedAt(Long id) {
        return jdbcTemplate.update(
                "UPDATE sys_user SET password_updated_at = now(), updated_at = now() WHERE id = ?", id);
    }
}

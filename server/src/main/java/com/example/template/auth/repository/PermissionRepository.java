package com.example.template.auth.repository;

import com.example.template.auth.model.SysPermission;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 权限数据访问（统一权限中心）。
 *
 * <p>查询用户权限时统一过滤 {@code p.deleted = 0 AND p.status = 'NORMAL'}，
 * 避免软删/禁用的权限点仍然生效。角色权限覆盖写入的事务边界由 Service 控制。</p>
 */
@Repository
public class PermissionRepository {

    private static final String BASE_COLUMNS =
            "id, permission_code, permission_name, permission_type, parent_id, module, action, "
                    + "route_path, route_name, component_path, redirect, icon, clickable, breadcrumb, "
                    + "always_show, is_external_link, external_link_url, sort_no, visible, keep_alive, "
                    + "data_scope_code, status, remark, created_at, updated_at, deleted";

    private static final RowMapper<SysPermission> ROW_MAPPER = (rs, rowNum) -> {
        SysPermission p = new SysPermission();
        p.setId(rs.getLong("id"));
        p.setPermissionCode(rs.getString("permission_code"));
        p.setPermissionName(rs.getString("permission_name"));
        p.setPermissionType(rs.getString("permission_type"));
        long parentId = rs.getLong("parent_id");
        p.setParentId(rs.wasNull() ? null : parentId);
        p.setModule(rs.getString("module"));
        p.setAction(rs.getString("action"));
        p.setRoutePath(rs.getString("route_path"));
        p.setRouteName(rs.getString("route_name"));
        p.setComponentPath(rs.getString("component_path"));
        p.setRedirect(rs.getString("redirect"));
        p.setIcon(rs.getString("icon"));
        p.setClickable(getNullableInt(rs, "clickable"));
        p.setBreadcrumb(getNullableInt(rs, "breadcrumb"));
        p.setAlwaysShow(getNullableInt(rs, "always_show"));
        p.setIsExternalLink(getNullableInt(rs, "is_external_link"));
        p.setExternalLinkUrl(rs.getString("external_link_url"));
        p.setSortNo(getNullableInt(rs, "sort_no"));
        p.setVisible(getNullableInt(rs, "visible"));
        p.setKeepAlive(getNullableInt(rs, "keep_alive"));
        p.setDataScopeCode(rs.getString("data_scope_code"));
        p.setStatus(rs.getString("status"));
        p.setRemark(rs.getString("remark"));
        p.setDeleted(getNullableInt(rs, "deleted"));
        return p;
    };

    private static Integer getNullableInt(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private final JdbcTemplate jdbcTemplate;

    public PermissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询用户可见的菜单权限（MENU 类型，按角色聚合去重，按 sort_no 排序）。
     *
     * @param userId 用户 ID
     * @return 菜单权限列表
     */
    public List<SysPermission> findMenusByUserId(Long userId) {
        String sql = "SELECT DISTINCT " + prefixColumns("p")
                + " FROM sys_permission p"
                + " JOIN sys_role_permission rp ON rp.permission_id = p.id AND rp.deleted = 0"
                + " JOIN sys_user_role ur ON ur.role_id = rp.role_id AND ur.deleted = 0"
                + " JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.status = 'NORMAL'"
                + " WHERE ur.user_id = ? AND p.deleted = 0 AND p.status = 'NORMAL' AND p.permission_type = 'MENU'"
                + " ORDER BY sort_no, id";
        return jdbcTemplate.query(sql, ROW_MAPPER, userId);
    }

    /**
     * 查询用户拥有的按钮/接口权限编码（BUTTON + API，去重）。
     *
     * @param userId 用户 ID
     * @return 权限编码列表
     */
    public List<String> findButtonApiCodesByUserId(Long userId) {
        String sql = "SELECT DISTINCT p.permission_code FROM sys_permission p"
                + " JOIN sys_role_permission rp ON rp.permission_id = p.id AND rp.deleted = 0"
                + " JOIN sys_user_role ur ON ur.role_id = rp.role_id AND ur.deleted = 0"
                + " JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.status = 'NORMAL'"
                + " WHERE ur.user_id = ? AND p.deleted = 0 AND p.status = 'NORMAL'"
                + " AND p.permission_type IN ('BUTTON', 'API')"
                + " ORDER BY p.permission_code";
        return jdbcTemplate.queryForList(sql, String.class, userId);
    }

    /**
     * 查询某角色已绑定的权限（未删除）。
     *
     * @param roleId 角色 ID
     * @return 权限列表
     */
    public List<SysPermission> findByRoleId(Long roleId) {
        String sql = "SELECT " + prefixColumns("p") + " FROM sys_permission p"
                + " JOIN sys_role_permission rp ON rp.permission_id = p.id AND rp.deleted = 0"
                + " WHERE rp.role_id = ? AND p.deleted = 0"
                + " ORDER BY p.sort_no, p.id";
        return jdbcTemplate.query(sql, ROW_MAPPER, roleId);
    }

    /**
     * 按权限编码批量查询有效权限 ID（仅 deleted=0 且 status=NORMAL）。
     * 用于角色权限覆盖时的 code→id 映射与合法性校验。
     *
     * @param codes 权限编码集合
     * @return 有效权限 ID 列表
     */
    public List<Long> findActiveIdsByCodes(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Collections.emptyList();
        }
        String placeholders = String.join(",", Collections.nCopies(codes.size(), "?"));
        String sql = "SELECT id FROM sys_permission"
                + " WHERE deleted = 0 AND status = 'NORMAL' AND permission_code IN (" + placeholders + ")";
        return jdbcTemplate.queryForList(sql, Long.class, codes.toArray());
    }

    /**
     * 分页查询权限点（管理端）。
     *
     * @param type    权限类型过滤（可空）
     * @param keyword 编码/名称模糊（可空）
     * @param offset  偏移
     * @param limit   每页大小
     * @return 权限列表
     */
    public List<SysPermission> findPage(String type, String keyword, int offset, int limit) {
        List<Object> args = new ArrayList<>();
        String where = buildPageWhere(type, keyword, args);
        String sql = "SELECT " + BASE_COLUMNS + " FROM sys_permission" + where
                + " ORDER BY sort_no, id LIMIT ? OFFSET ?";
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.query(sql, ROW_MAPPER, args.toArray());
    }

    /**
     * 统计权限点总数（与 {@link #findPage} 同条件）。
     *
     * @param type    权限类型过滤（可空）
     * @param keyword 编码/名称模糊（可空）
     * @return 总数
     */
    public long countPage(String type, String keyword) {
        List<Object> args = new ArrayList<>();
        String where = buildPageWhere(type, keyword, args);
        String sql = "SELECT count(*) FROM sys_permission" + where;
        Long total = jdbcTemplate.queryForObject(sql, Long.class, args.toArray());
        return total == null ? 0L : total;
    }

    /**
     * 软删除角色的全部权限关联（覆盖式更新第一步）。
     *
     * @param roleId 角色 ID
     * @return 受影响行数
     */
    public int softDeleteByRoleId(Long roleId) {
        return jdbcTemplate.update(
                "UPDATE sys_role_permission SET deleted = 1 WHERE role_id = ? AND deleted = 0", roleId);
    }

    /**
     * 批量插入角色权限关联（覆盖式更新第二步）。
     *
     * @param roleId        角色 ID
     * @param permissionIds 权限 ID 列表
     */
    public void insertRolePermissions(Long roleId, List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO sys_role_permission (role_id, permission_id) VALUES (?, ?)",
                permissionIds,
                permissionIds.size(),
                (ps, permissionId) -> {
                    ps.setLong(1, roleId);
                    ps.setLong(2, permissionId);
                });
    }

    private String buildPageWhere(String type, String keyword, List<Object> args) {
        StringBuilder where = new StringBuilder(" WHERE deleted = 0");
        if (StringUtils.hasText(type)) {
            where.append(" AND permission_type = ?");
            args.add(type);
        }
        if (StringUtils.hasText(keyword)) {
            where.append(" AND (permission_code ILIKE ? OR permission_name ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        return where.toString();
    }

    private static String prefixColumns(String alias) {
        String[] cols = BASE_COLUMNS.split(",\\s*");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(alias).append('.').append(cols[i]);
        }
        return sb.toString();
    }
}

-- auth v2.1 增量：用户管理权限点（幂等，可重复执行）
INSERT INTO sys_permission (permission_code, permission_name, permission_type, module, action)
SELECT v.code, v.name, 'API', 'auth', v.act
FROM (VALUES
    ('auth:user:read',           '查看用户', 'read_user'),
    ('auth:user:create',         '新建用户', 'create_user'),
    ('auth:user:update',         '编辑用户', 'update_user'),
    ('auth:user:reset_password', '重置密码', 'reset_password')
) AS v(code, name, act)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code AND p.deleted = 0
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p
  ON p.permission_code IN (
        'auth:user:read', 'auth:user:create', 'auth:user:update', 'auth:user:reset_password'
     )
 AND p.deleted = 0
WHERE r.role_code = 'ADMIN' AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id AND rp.deleted = 0
  );

-- auth v2.1：修复 sys_user_role 唯一索引（支持软删后同对 (user_id,role_id) 再插入）
DROP INDEX IF EXISTS uk_user_role;
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_role ON sys_user_role (user_id, role_id) WHERE deleted = 0;

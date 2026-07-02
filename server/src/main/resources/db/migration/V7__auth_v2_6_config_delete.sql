-- auth v2.6 增量：系统配置「删除」权限（幂等）

INSERT INTO sys_permission (permission_code, permission_name, permission_type, module, action)
SELECT 'auth:config:delete', '删除系统配置', 'API', 'system', 'delete_config'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = 'auth:config:delete' AND p.deleted = 0
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'auth:config:delete' AND p.deleted = 0
WHERE r.role_code = 'ADMIN' AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id AND rp.deleted = 0
  );

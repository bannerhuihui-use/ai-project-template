-- auth v2.3 增量：审计日志菜单（幂等）
INSERT INTO sys_permission
    (permission_code, permission_name, permission_type, parent_id, module,
     route_path, route_name, component_path, icon, clickable, sort_no, visible)
SELECT 'menu:system:audit', '审计日志', 'MENU',
       (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:root' AND deleted = 0),
       'system', '/system/audit', 'SystemAudit', '/pages/system/audit/index', 'icon-audit', 1, 30, 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = 'menu:system:audit' AND p.deleted = 0
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'menu:system:audit' AND p.deleted = 0
WHERE r.role_code = 'ADMIN' AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id AND rp.deleted = 0
  );

-- 审计日志 API 权限
INSERT INTO sys_permission (permission_code, permission_name, permission_type, module, action)
SELECT 'auth:audit:read', '查看审计日志', 'API', 'system', 'read_audit'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = 'auth:audit:read' AND p.deleted = 0
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'auth:audit:read' AND p.deleted = 0
WHERE r.role_code = 'ADMIN' AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id AND rp.deleted = 0
  );

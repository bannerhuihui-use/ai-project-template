-- auth v2.2 增量：角色权限管理菜单（幂等）
INSERT INTO sys_permission
    (permission_code, permission_name, permission_type, parent_id, module,
     route_path, route_name, component_path, icon, clickable, sort_no, visible)
SELECT 'menu:auth:role', '角色权限', 'MENU',
       (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:root' AND deleted = 0),
       'auth', '/system/role', 'AuthRole', '/pages/auth/role/index', 'icon-safety', 1, 20, 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = 'menu:auth:role' AND p.deleted = 0
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'menu:auth:role' AND p.deleted = 0
WHERE r.role_code = 'ADMIN' AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id AND rp.deleted = 0
  );

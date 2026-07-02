-- auth v2.4 增量：新建角色权限 + 系统配置表/菜单/权限（幂等）

-- ---------- 1. 新建角色 API 权限 ----------
INSERT INTO sys_permission (permission_code, permission_name, permission_type, module, action)
SELECT 'auth:role:create', '新建角色', 'API', 'auth', 'create_role'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = 'auth:role:create' AND p.deleted = 0
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'auth:role:create' AND p.deleted = 0
WHERE r.role_code = 'ADMIN' AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id AND rp.deleted = 0
  );

-- ---------- 2. 系统配置表 ----------
CREATE TABLE IF NOT EXISTS sys_config (
    id           bigserial    PRIMARY KEY,
    config_key   varchar(128) NOT NULL,
    config_name  varchar(128) NOT NULL,
    config_value text,
    config_group varchar(64)  NOT NULL DEFAULT 'default',
    value_type   varchar(32)  NOT NULL DEFAULT 'STRING',
    description  varchar(512),
    editable     smallint     NOT NULL DEFAULT 1,
    sort_no      int          NOT NULL DEFAULT 0,
    created_at   timestamp    NOT NULL DEFAULT now(),
    updated_at   timestamp    NOT NULL DEFAULT now(),
    deleted      smallint     NOT NULL DEFAULT 0
);
COMMENT ON TABLE sys_config IS '系统配置表（键值参数，供后台与业务模块读取）';
COMMENT ON COLUMN sys_config.config_key IS '配置键，全局唯一';
COMMENT ON COLUMN sys_config.value_type IS 'STRING/NUMBER/BOOLEAN/JSON';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_config_key ON sys_config (config_key) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sys_config_group ON sys_config (config_group);

-- ---------- 3. 系统配置种子数据 ----------
INSERT INTO sys_config (config_key, config_name, config_value, config_group, value_type, description, editable, sort_no)
SELECT v.k, v.n, v.v, v.g, v.t, v.d, 1, v.s
FROM (VALUES
    ('site.name',              '站点名称',       '管理后台',           'site',   'STRING',  '展示在登录页与页头', 10),
    ('site.logo',              '站点 Logo URL',  '',                   'site',   'STRING',  '站点 Logo 图片地址', 20),
    ('site.copyright',         '版权信息',       '© 2026 Template',    'site',   'STRING',  '页脚版权文案', 30),
    ('auth.login.captcha_enabled', '登录验证码', 'false',              'auth',   'BOOLEAN', '是否开启登录图形验证码', 10),
    ('auth.password.min_length',   '密码最小长度', '8',                'auth',   'NUMBER',  '新建/改密时的最小长度', 20),
    ('auth.session.idle_minutes',  '空闲超时(分钟)', '120',            'auth',   'NUMBER',  '前端空闲登出提示参考值', 30),
    ('system.maintenance_mode',    '维护模式',     'false',            'system', 'BOOLEAN', '开启后仅超级管理员可登录', 10),
    ('system.audit.retention_days','审计日志保留天数', '30',          'system', 'NUMBER',  '与 Mongo TTL 策略对齐的展示说明', 20)
) AS v(k, n, v, g, t, d, s)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config c WHERE c.config_key = v.k AND c.deleted = 0
);

-- ---------- 4. 系统配置 API 权限 ----------
INSERT INTO sys_permission (permission_code, permission_name, permission_type, module, action)
SELECT v.code, v.name, 'API', 'system', v.act
FROM (VALUES
    ('auth:config:read',   '查看系统配置', 'read_config'),
    ('auth:config:update', '更新系统配置', 'update_config')
) AS v(code, name, act)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code AND p.deleted = 0
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('auth:config:read', 'auth:config:update') AND p.deleted = 0
WHERE r.role_code = 'ADMIN' AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id AND rp.deleted = 0
  );

-- ---------- 5. 系统配置菜单 ----------
INSERT INTO sys_permission
    (permission_code, permission_name, permission_type, parent_id, module,
     route_path, route_name, component_path, icon, clickable, sort_no, visible)
SELECT 'menu:system:config', '系统配置', 'MENU',
       (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:root' AND deleted = 0),
       'system', '/system/config', 'SystemConfig', '/pages/system/config/index', 'icon-setting', 1, 20, 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = 'menu:system:config' AND p.deleted = 0
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'menu:system:config' AND p.deleted = 0
WHERE r.role_code = 'ADMIN' AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id AND rp.deleted = 0
  );

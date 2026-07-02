-- =====================================================================
-- AI 项目模板 — PostgreSQL 全量 DDL + 种子数据（当前快照）
-- 文档：docs/DB/SCHEMA.md
-- 等价于 Flyway migration V1~V8 的合并结果（幂等，可重复执行）
--
-- 推荐：新库由应用启动时 Flyway 自动迁移（server/src/main/resources/db/migration/）
-- 手动：psql -h HOST -U USER -d DBNAME -f docs/DB/template-full.sql
--
-- 默认管理员：用户名 admin / 密码 123456（BCrypt，生产务必修改）
-- =====================================================================
-- -----------------------------------------------------------------
-- Flyway V1: V1__auth_core.sql
-- -----------------------------------------------------------------
-- =====================================================================
-- auth-core 鉴权核心表（Flyway V1）
-- 设计文档：docs/DB/SCHEMA.md
-- =====================================================================

-- ---------- 1. sys_user 用户主体 ----------
CREATE TABLE IF NOT EXISTS sys_user (
    id                  bigserial    PRIMARY KEY,
    user_type           varchar(32)  NOT NULL,
    nickname            varchar(64),
    avatar_url          varchar(512),
    phone               varchar(32),
    email               varchar(128),
    status              varchar(32)  NOT NULL DEFAULT 'NORMAL',
    token_version       int          NOT NULL DEFAULT 0,
    password_updated_at timestamp,
    last_login_time     timestamp,
    created_at          timestamp    NOT NULL DEFAULT now(),
    updated_at          timestamp    NOT NULL DEFAULT now(),
    deleted             smallint     NOT NULL DEFAULT 0
);
-- 兼容已存在的旧表：幂等补列（PostgreSQL 9.6+ 支持 ADD COLUMN IF NOT EXISTS）
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS token_version       int       NOT NULL DEFAULT 0;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS password_updated_at timestamp;
COMMENT ON TABLE sys_user IS '用户主体表';
COMMENT ON COLUMN sys_user.user_type IS '用户类型：ADMIN / MEMBER';
COMMENT ON COLUMN sys_user.status IS '状态：NORMAL / DISABLED / DELETED';
COMMENT ON COLUMN sys_user.token_version IS 'token 版本：自增即让该用户此前所有令牌失效（改密/禁用/强制下线）';
COMMENT ON COLUMN sys_user.password_updated_at IS '密码最后修改时间（可选，审计用）';
COMMENT ON COLUMN sys_user.deleted IS '逻辑删除：0 未删除，1 已删除';

CREATE INDEX IF NOT EXISTS idx_user_type ON sys_user (user_type);
CREATE INDEX IF NOT EXISTS idx_phone     ON sys_user (phone);
CREATE INDEX IF NOT EXISTS idx_status    ON sys_user (status);

-- ---------- 2. user_identity 登录身份 ----------
CREATE TABLE IF NOT EXISTS user_identity (
    id              bigserial    PRIMARY KEY,
    user_id         bigint       NOT NULL,
    identity_type   varchar(32)  NOT NULL,
    identifier      varchar(128) NOT NULL,
    credential      varchar(255),
    app_id          varchar(64),
    union_id        varchar(128),
    status          varchar(32)  NOT NULL DEFAULT 'NORMAL',
    last_login_time timestamp,
    created_at      timestamp    NOT NULL DEFAULT now(),
    updated_at      timestamp    NOT NULL DEFAULT now(),
    deleted         smallint     NOT NULL DEFAULT 0
);
COMMENT ON TABLE user_identity IS '登录身份表';
COMMENT ON COLUMN user_identity.identity_type IS '登录方式：USERNAME / PHONE / EMAIL / WECHAT_MINIAPP';
COMMENT ON COLUMN user_identity.identifier IS '登录标识：用户名 / 手机号 / 邮箱 / openid';
COMMENT ON COLUMN user_identity.credential IS '登录凭证：BCrypt 密码；微信登录为空';
COMMENT ON COLUMN user_identity.app_id IS '微信小程序 appid，可空';
COMMENT ON COLUMN user_identity.union_id IS '微信 unionid，可空';

-- 唯一键含可空列 app_id：PostgreSQL 中 NULL 互不相等，普通唯一索引拦不住「用户名/手机/邮箱重复」。
-- 用 COALESCE(app_id,'') 表达式把 NULL 归一为空串，语义等价且兼容 PostgreSQL 12+。
-- 若确定运行环境为 PostgreSQL 15+，亦可改用：
--   CREATE UNIQUE INDEX IF NOT EXISTS uk_identity
--       ON user_identity (identity_type, identifier, app_id) NULLS NOT DISTINCT;
CREATE UNIQUE INDEX IF NOT EXISTS uk_identity
    ON user_identity (identity_type, identifier, COALESCE(app_id, ''));
CREATE INDEX IF NOT EXISTS idx_user_id    ON user_identity (user_id);
CREATE INDEX IF NOT EXISTS idx_identifier ON user_identity (identifier);

-- ---------- 3. sys_role 角色 ----------
CREATE TABLE IF NOT EXISTS sys_role (
    id          bigserial    PRIMARY KEY,
    role_code   varchar(64)  NOT NULL,
    role_name   varchar(64)  NOT NULL,
    status      varchar(32)  NOT NULL DEFAULT 'NORMAL',
    created_at  timestamp    NOT NULL DEFAULT now(),
    updated_at  timestamp    NOT NULL DEFAULT now(),
    deleted     smallint     NOT NULL DEFAULT 0
);
COMMENT ON TABLE sys_role IS '角色表';
COMMENT ON COLUMN sys_role.role_code IS '角色编码：ADMIN / SUPER_ADMIN / OPERATOR';

CREATE UNIQUE INDEX IF NOT EXISTS uk_role_code ON sys_role (role_code);

-- ---------- 4. sys_user_role 用户角色关联 ----------
CREATE TABLE IF NOT EXISTS sys_user_role (
    id          bigserial PRIMARY KEY,
    user_id     bigint    NOT NULL,
    role_id     bigint    NOT NULL,
    created_at  timestamp NOT NULL DEFAULT now(),
    deleted     smallint  NOT NULL DEFAULT 0
);
COMMENT ON TABLE sys_user_role IS '用户角色关联表';

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_role ON sys_user_role (user_id, role_id) WHERE deleted = 0;

-- ---------- 5. auth_login_device 登录设备记录 ----------
CREATE TABLE IF NOT EXISTS auth_login_device (
    id              bigserial    PRIMARY KEY,
    user_id         bigint       NOT NULL,
    identity_type   varchar(32)  NOT NULL,
    device_id       varchar(128),
    device_name     varchar(128),
    platform        varchar(64),
    ip              varchar(64),
    user_agent      varchar(512),
    last_login_time timestamp,
    created_at      timestamp    NOT NULL DEFAULT now(),
    updated_at      timestamp    NOT NULL DEFAULT now(),
    deleted         smallint     NOT NULL DEFAULT 0
);
COMMENT ON TABLE auth_login_device IS '登录设备记录表';
COMMENT ON COLUMN auth_login_device.device_id IS '设备 ID（前端可选传入）；为空则每次登录新增一条';
COMMENT ON COLUMN auth_login_device.platform IS '平台：WEB / IOS / ANDROID / MINIAPP 等';

CREATE INDEX IF NOT EXISTS idx_login_device_user_id         ON auth_login_device (user_id);
CREATE INDEX IF NOT EXISTS idx_login_device_device_id       ON auth_login_device (device_id);
CREATE INDEX IF NOT EXISTS idx_login_device_last_login_time ON auth_login_device (last_login_time);

-- =====================================================================
-- 初始化管理员（方案 A）
-- 密码必须是 BCrypt 密文。下方示例口令为 "123456"，仅供本地演示，生产务必重置。
-- 生成方式见 server/README.md「初始化后台管理员」。
-- =====================================================================
-- 1) 管理员主体
INSERT INTO sys_user (user_type, nickname, status)
SELECT 'ADMIN', '管理员', 'NORMAL'
WHERE NOT EXISTS (
    SELECT 1 FROM user_identity WHERE identity_type = 'USERNAME' AND identifier = 'admin'
);

-- 2) 用户名登录身份（BCrypt("123456") 示例值，请替换为自行生成的密文）
INSERT INTO user_identity (user_id, identity_type, identifier, credential, status)
SELECT u.id, 'USERNAME', 'admin',
       '$2a$10$80q/EroxaxNvlS7kNBg9Bel8P3iyMHSx8TEJ9KemE.ofg0ZIaSOvS', 'NORMAL'
FROM sys_user u
WHERE u.user_type = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM user_identity WHERE identity_type = 'USERNAME' AND identifier = 'admin')
ORDER BY u.id DESC
LIMIT 1;

-- 3) 角色
INSERT INTO sys_role (role_code, role_name, status)
SELECT 'ADMIN', '管理员', 'NORMAL'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'ADMIN');

-- 4) 绑定用户-角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT ui.user_id, r.id
FROM user_identity ui
JOIN sys_role r ON r.role_code = 'ADMIN'
WHERE ui.identity_type = 'USERNAME' AND ui.identifier = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role sur WHERE sur.user_id = ui.user_id AND sur.role_id = r.id
  );

-- =====================================================================
-- auth v2.0 RBAC：统一权限中心（MENU / BUTTON / API / DATA）
-- 设计文档：docs/DB/SCHEMA.md、docs/API/auth-v2.0-rbac.md
-- 顺序要点：先建表 -> 先灌种子 -> 再开启方法级 @PreAuthorize（不可颠倒）
-- =====================================================================

-- ---------- 6. sys_permission 统一权限定义 ----------
CREATE TABLE IF NOT EXISTS sys_permission (
    id                bigserial    PRIMARY KEY,
    permission_code   varchar(128) NOT NULL,
    permission_name   varchar(128) NOT NULL,
    permission_type   varchar(16)  NOT NULL,
    parent_id         bigint,
    module            varchar(64),
    action            varchar(64),
    route_path        varchar(255),
    route_name        varchar(128),
    component_path    varchar(255),
    redirect          varchar(255),
    icon              varchar(255),
    clickable         smallint     NOT NULL DEFAULT 1,
    breadcrumb        smallint     NOT NULL DEFAULT 1,
    always_show       smallint     NOT NULL DEFAULT 0,
    is_external_link  smallint     NOT NULL DEFAULT 0,
    external_link_url varchar(512),
    sort_no           int          NOT NULL DEFAULT 0,
    visible           smallint     NOT NULL DEFAULT 1,
    keep_alive        smallint     NOT NULL DEFAULT 0,
    data_scope_code   varchar(64),
    status            varchar(32)  NOT NULL DEFAULT 'NORMAL',
    remark            varchar(255),
    created_at        timestamp    NOT NULL DEFAULT now(),
    updated_at        timestamp    NOT NULL DEFAULT now(),
    deleted           smallint     NOT NULL DEFAULT 0
);
COMMENT ON TABLE sys_permission IS '统一权限定义表：MENU 菜单 / BUTTON 按钮 / API 接口 / DATA 数据范围（预留）';
COMMENT ON COLUMN sys_permission.permission_code IS '权限编码（全局唯一）：{module}:{resource}:{action}，菜单建议 menu:<module>:<node>';
COMMENT ON COLUMN sys_permission.permission_type IS 'MENU / BUTTON / API / DATA';
COMMENT ON COLUMN sys_permission.parent_id IS '父节点 ID（菜单树）';
COMMENT ON COLUMN sys_permission.clickable IS '目录是否可点击：1 可点 / 0 仅分组展示';
COMMENT ON COLUMN sys_permission.data_scope_code IS '数据权限范围码（DATA 预留，v2.1 落地）';

-- 唯一/普通索引（唯一键带 deleted=0 条件，允许软删后同码复用）
CREATE UNIQUE INDEX IF NOT EXISTS uk_permission_code
    ON sys_permission (permission_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_permission_type   ON sys_permission (permission_type);
CREATE INDEX IF NOT EXISTS idx_permission_parent ON sys_permission (parent_id);
CREATE INDEX IF NOT EXISTS idx_permission_module ON sys_permission (module);
CREATE INDEX IF NOT EXISTS idx_permission_status ON sys_permission (status);
CREATE INDEX IF NOT EXISTS idx_permission_sort   ON sys_permission (sort_no);

-- ---------- 7. sys_role_permission 角色权限关联 ----------
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id            bigserial PRIMARY KEY,
    role_id       bigint    NOT NULL,
    permission_id bigint    NOT NULL,
    created_at    timestamp NOT NULL DEFAULT now(),
    deleted       smallint  NOT NULL DEFAULT 0
);
COMMENT ON TABLE sys_role_permission IS '角色权限关联表（多对多）';

CREATE UNIQUE INDEX IF NOT EXISTS uk_role_permission
    ON sys_role_permission (role_id, permission_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_role_permission_role_id       ON sys_role_permission (role_id);
CREATE INDEX IF NOT EXISTS idx_role_permission_permission_id ON sys_role_permission (permission_id);

-- =====================================================================
-- auth v2.0 种子数据（必做，防引导死锁；全部幂等）
-- =====================================================================

-- 1) SUPER_ADMIN 角色（代码层短路所有权限码校验）
INSERT INTO sys_role (role_code, role_name, status)
SELECT 'SUPER_ADMIN', '超级管理员', 'NORMAL'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'SUPER_ADMIN' AND deleted = 0);

-- 2) 把初始化 admin 账号提升为 SUPER_ADMIN，保证首个账号可配置一切（引导兜底）
INSERT INTO sys_user_role (user_id, role_id)
SELECT ui.user_id, r.id
FROM user_identity ui
JOIN sys_role r ON r.role_code = 'SUPER_ADMIN' AND r.deleted = 0
WHERE ui.identity_type = 'USERNAME' AND ui.identifier = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role sur WHERE sur.user_id = ui.user_id AND sur.role_id = r.id
  );

-- 3) 权限点（API/BUTTON 类，幂等；含 v1.2.1 存量接口编码）
INSERT INTO sys_permission (permission_code, permission_name, permission_type, module, action)
SELECT v.code, v.name, 'API', 'auth', v.act
FROM (VALUES
    ('auth:user:read',             '查看用户',      'read_user'),
    ('auth:user:create',           '新建用户',      'create_user'),
    ('auth:user:update',           '编辑用户',      'update_user'),
    ('auth:user:reset_password',   '重置密码',      'reset_password'),
    ('auth:user:disable',          '禁用用户',      'disable_user'),
    ('auth:user:force_logout',     '强制下线',      'force_logout'),
    ('auth:user:grant_role',       '分配用户角色',  'grant_role'),
    ('auth:permission:read',       '查看权限',      'read_permission'),
    ('auth:role:read',             '查看角色权限',  'read_role'),
    ('auth:role:grant_permission', '配置角色权限',  'grant_permission'),
    ('auth:rbac:ping',             'RBAC探活',      'ping')
) AS v(code, name, act)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code AND p.deleted = 0
);

-- 4) 基础菜单（保证后台登录后不空白；幂等）
--    根菜单：系统管理（目录，可点击=0 仅分组）
INSERT INTO sys_permission
    (permission_code, permission_name, permission_type, parent_id, module,
     route_path, route_name, component_path, icon, clickable, sort_no, visible)
SELECT 'menu:system:root', '系统管理', 'MENU', NULL, 'system',
       '/system', 'System', NULL, 'icon-setting', 0, 100, 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = 'menu:system:root' AND p.deleted = 0
);
--    子菜单：用户管理（挂在系统管理下）
INSERT INTO sys_permission
    (permission_code, permission_name, permission_type, parent_id, module,
     route_path, route_name, component_path, icon, clickable, sort_no, visible)
SELECT 'menu:auth:user', '用户管理', 'MENU',
       (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:root' AND deleted = 0),
       'auth', '/system/user', 'AuthUser', '/pages/auth/user/index', 'icon-user', 1, 10, 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = 'menu:auth:user' AND p.deleted = 0
);
--    子菜单：角色权限（挂在系统管理下）
INSERT INTO sys_permission
    (permission_code, permission_name, permission_type, parent_id, module,
     route_path, route_name, component_path, icon, clickable, sort_no, visible)
SELECT 'menu:auth:role', '角色权限', 'MENU',
       (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:root' AND deleted = 0),
       'auth', '/system/role', 'AuthRole', '/pages/auth/role/index', 'icon-safety', 1, 20, 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = 'menu:auth:role' AND p.deleted = 0
);
--    子菜单：审计日志（挂在系统管理下）
INSERT INTO sys_permission
    (permission_code, permission_name, permission_type, parent_id, module,
     route_path, route_name, component_path, icon, clickable, sort_no, visible)
SELECT 'menu:system:audit', '审计日志', 'MENU',
       (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:root' AND deleted = 0),
       'system', '/system/audit', 'SystemAudit', '/pages/system/audit/index', 'icon-audit', 1, 30, 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = 'menu:system:audit' AND p.deleted = 0
);

-- 5) 给 ADMIN 角色授予全部上述权限（API + 菜单；幂等）
--    SUPER_ADMIN 因代码短路无需逐条授权；ADMIN 必须显式授权，否则上线后被 403。
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p
  ON p.permission_code IN (
        'auth:user:read', 'auth:user:create', 'auth:user:update', 'auth:user:reset_password',
        'auth:user:disable', 'auth:user:force_logout', 'auth:user:grant_role',
        'auth:permission:read', 'auth:role:read', 'auth:role:grant_permission', 'auth:rbac:ping',
        'menu:system:root', 'menu:auth:user', 'menu:auth:role', 'menu:system:audit'
     )
 AND p.deleted = 0
WHERE r.role_code = 'ADMIN' AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id AND rp.deleted = 0
  );

-- -----------------------------------------------------------------
-- Flyway V2: V2__auth_v2_1_user_perms.sql
-- -----------------------------------------------------------------
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

-- -----------------------------------------------------------------
-- Flyway V3: V3__auth_v2_2_rbac_menu.sql
-- -----------------------------------------------------------------
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

-- -----------------------------------------------------------------
-- Flyway V4: V4__auth_v2_3_audit_menu.sql
-- -----------------------------------------------------------------
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

-- -----------------------------------------------------------------
-- Flyway V5: V5__auth_v2_4_role_config.sql
-- -----------------------------------------------------------------
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

-- -----------------------------------------------------------------
-- Flyway V6: V6__auth_v2_5_config_create.sql
-- -----------------------------------------------------------------
-- auth v2.5 增量：系统配置「新建配置项」权限（幂等）

INSERT INTO sys_permission (permission_code, permission_name, permission_type, module, action)
SELECT 'auth:config:create', '新建系统配置', 'API', 'system', 'create_config'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = 'auth:config:create' AND p.deleted = 0
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'auth:config:create' AND p.deleted = 0
WHERE r.role_code = 'ADMIN' AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id AND rp.deleted = 0
  );

-- -----------------------------------------------------------------
-- Flyway V7: V7__auth_v2_6_config_delete.sql
-- -----------------------------------------------------------------
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

-- -----------------------------------------------------------------
-- Flyway V8: V8__file_core.sql
-- -----------------------------------------------------------------
-- file-core 文件元数据表与权限种子（幂等）

CREATE TABLE IF NOT EXISTS sys_file (
    id              bigserial    PRIMARY KEY,
    file_key        varchar(64)  NOT NULL,
    original_name   varchar(256) NOT NULL,
    storage_type    varchar(16)  NOT NULL,
    storage_path    varchar(512) NOT NULL,
    content_type    varchar(128) NOT NULL,
    file_size       bigint       NOT NULL,
    file_hash       varchar(64),
    biz_type        varchar(32)  NOT NULL,
    access_level    varchar(16)  NOT NULL DEFAULT 'private',
    uploader_id     bigint       NOT NULL,
    uploader_type   varchar(16)  NOT NULL,
    status          varchar(16)  NOT NULL DEFAULT 'NORMAL',
    created_at      timestamp    NOT NULL DEFAULT now(),
    updated_at      timestamp    NOT NULL DEFAULT now(),
    deleted         smallint     NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_file IS '文件元数据表';
COMMENT ON COLUMN sys_file.file_key IS '对外唯一标识（UUID）';
COMMENT ON COLUMN sys_file.storage_path IS '存储相对路径或 OSS object key';
COMMENT ON COLUMN sys_file.biz_type IS '业务分类：avatar/image/document/attachment';
COMMENT ON COLUMN sys_file.access_level IS '访问级别：public/private';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_file_key ON sys_file (file_key) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sys_file_uploader ON sys_file (uploader_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_sys_file_biz_type ON sys_file (biz_type);

-- API 权限
INSERT INTO sys_permission (permission_code, permission_name, permission_type, module, action)
SELECT 'file:upload', '上传文件', 'API', 'file', 'upload'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = 'file:upload' AND p.deleted = 0
);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, module, action)
SELECT 'file:read', '查看文件', 'API', 'file', 'read'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = 'file:read' AND p.deleted = 0
);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, module, action)
SELECT 'file:delete', '删除文件', 'API', 'file', 'delete'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = 'file:delete' AND p.deleted = 0
);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, module, action)
SELECT 'file:admin', '管理全部文件', 'API', 'file', 'admin'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = 'file:admin' AND p.deleted = 0
);

-- ADMIN / SUPER_ADMIN 默认绑定 file 权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('file:upload', 'file:read', 'file:delete') AND p.deleted = 0
WHERE r.role_code IN ('ADMIN', 'SUPER_ADMIN') AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id AND rp.deleted = 0
  );

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'file:admin' AND p.deleted = 0
WHERE r.role_code = 'SUPER_ADMIN' AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id AND rp.deleted = 0
  );

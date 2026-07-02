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

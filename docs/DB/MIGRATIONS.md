# 数据库迁移（Flyway）

后端启动时由 **Flyway** 自动执行 `server/src/main/resources/db/migration/` 下的脚本。

全量 SQL 快照（与 V1~V8 等价）：[`template-full.sql`](./template-full.sql)

## 版本顺序

| 版本 | 文件 | 说明 |
|------|------|------|
| V1 | `V1__auth_core.sql` | 鉴权核心表 + RBAC 表 + 管理员/角色/权限/菜单种子 |
| V2 | `V2__auth_v2_1_user_perms.sql` | 用户管理权限补全 + uk_user_role 索引修复 |
| V3 | `V3__auth_v2_2_rbac_menu.sql` | 角色权限菜单 |
| V4 | `V4__auth_v2_3_audit_menu.sql` | 审计菜单 + auth:audit:read |
| V5 | `V5__auth_v2_4_role_config.sql` | sys_config 表 + 配置菜单/权限 + auth:role:create |
| V6 | `V6__auth_v2_5_config_create.sql` | auth:config:create |
| V7 | `V7__auth_v2_6_config_delete.sql` | auth:config:delete |
| V8 | `V8__file_core.sql` | sys_file 表 + file:* 权限 |

> V2~V4 与 V1 部分种子重复，均为 `NOT EXISTS` 幂等插入，可安全重复执行。

## 全新数据库

配置好 `.env` 中的 PostgreSQL 连接后，直接：

```bash
cd server && mvn spring-boot:run
```

Flyway 按 V1→V8 依次执行。

## 手动初始化（不启 Flyway）

空库执行：

```bash
psql -h HOST -U USER -d DBNAME -f docs/DB/template-full.sql
```

若需与 Flyway 对齐，可手动 baseline 到 V8 或设置 `spring.flyway.baseline-version=8`。

## 已手动执行过 SQL 的库

若表已存在但无 `flyway_schema_history`，首次启用 Flyway 会尝试从 V1 建表并失败。任选其一：

1. **推荐（结构已完整）**：Flyway baseline 到 V8
2. **空库重建**：删库或换库名后重新启动

baseline 示例（PostgreSQL 手动插入 history，版本按实际调整）：

```sql
CREATE TABLE IF NOT EXISTS flyway_schema_history (
    installed_rank INT NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INT,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT now(),
    execution_time INT NOT NULL,
    success BOOLEAN NOT NULL
);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success)
VALUES (1, '8', '<< Flyway Baseline >>', 'BASELINE', '<< Flyway Baseline >>', NULL, 'manual', 0, true);
```

## 关闭 Flyway

测试 profile 默认 `spring.flyway.enabled=false`。本地也可在 `.env` 设置 `FLYWAY_ENABLED=false` 后自行执行 `template-full.sql`。

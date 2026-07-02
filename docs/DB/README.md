# DB 数据库设计文档

> 记录数据库选型、表结构、索引与种子数据。模板当前快照以 [`SCHEMA.md`](./SCHEMA.md) 为准。

## 1. 存储选型（模板默认）

| 存储 | 用途 | 说明 |
|------|------|------|
| PostgreSQL | 核心业务 | 鉴权、配置、文件元数据等 |
| MongoDB | 集中式日志（可选） | WARN+ERROR 审计查询 |
| Redis | 缓存 / JWT 黑名单 | 鉴权 fail-closed 依赖 |

## 2. 设计规范

- 表名：小写下划线，如 `sys_user`
- 主键：`bigserial`
- 公共字段：`created_at` / `updated_at` / `deleted`（逻辑删除）
- 禁止物理外键（应用层维护关系）
- 唯一索引带 `WHERE deleted = 0`

## 3. 当前表结构

完整说明见 **[`SCHEMA.md`](./SCHEMA.md)**，包含 9 张表字段、索引、种子数据汇总。

## 4. SQL 与迁移

| 文件 | 用途 |
|------|------|
| [`template-full.sql`](./template-full.sql) | 全量 DDL + 种子（V1~V8 合并，幂等） |
| [`MIGRATIONS.md`](./MIGRATIONS.md) | Flyway 版本说明与 baseline 方案 |
| `server/src/main/resources/db/migration/V*.sql` | 应用启动自动执行的增量脚本 |

**推荐**：新环境配置 `.env` 后 `mvn spring-boot:run`，由 Flyway 自动迁移。

**手动**（空库）：

```bash
psql -h HOST -U USER -d DBNAME -f docs/DB/template-full.sql
```

## 5. ER 图

见 [`SCHEMA.md`](./SCHEMA.md) 第 1 节 Mermaid 图。

## 6. Redis Key 规范（鉴权）

| 业务 | Key 模式 | TTL | 说明 |
|------|----------|-----|------|
| JWT 黑名单 | `auth:token:blacklist:{jti}` | 令牌剩余有效期 | logout / refresh 轮换 |

## 7. 新业务模块

新增业务表时，在本目录补充表说明，并新增 Flyway `V9__xxx.sql`（勿直接改历史版本）。

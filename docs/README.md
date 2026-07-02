# 项目文档中心

> 本仓库是 **AI 协作项目模板** 的文档索引。模板当前能力快照见 [`TEMPLATE.md`](./TEMPLATE.md)。

## 文档导航

| 模块 | 说明 | 入口 |
|------|------|------|
| **模板总览** | 当前已落地能力、fork 指引 | [TEMPLATE.md](./TEMPLATE.md) |
| PRD | 产品需求模板（新业务模块填写） | [PRD/README.md](./PRD/README.md) |
| API | 接口规范与 RBAC 契约 | [API/README.md](./API/README.md) |
| DB | 表结构、种子数据、Flyway | [DB/README.md](./DB/README.md) |
| LOGGING | 日志与集中式 Mongo | [LOGGING/README.md](./LOGGING/README.md) |
| DEPLOY | 部署文档模板 | [DEPLOY/README.md](./DEPLOY/README.md) |
| DEPLOY | 本地多项目环境隔离 | [DEPLOY/local-env-isolation.md](./DEPLOY/local-env-isolation.md) |
| UI | 后台设计参考 | [UI/stitch_standard_admin_framework/systematic_efficiency/DESIGN.md](./UI/stitch_standard_admin_framework/systematic_efficiency/DESIGN.md) |

## 核心参考（已落地）

| 主题 | 文档 |
|------|------|
| 数据库表结构 + 种子 | [DB/SCHEMA.md](./DB/SCHEMA.md) |
| 全量建表 SQL | [DB/template-full.sql](./DB/template-full.sql) |
| Flyway 迁移 | [DB/MIGRATIONS.md](./DB/MIGRATIONS.md) |
| RBAC 接口契约 | [API/auth-v2.0-rbac.md](./API/auth-v2.0-rbac.md) |
| 后端启动与鉴权 | [../server/README.md](../server/README.md) |
| 后台前端 | [../web/admin/README.md](../web/admin/README.md) |

## 协作流程

遵循「先分析、再设计、最后编码」（详见 `.cursorrules`）：

1. 填写 `AI_CONTEXT.md` 与 `PRD`
2. 设计 `DB` / `API`
3. 确认后再编码

## 技术栈

- 后端：Java 17 / Spring Boot 3
- 前端：React 18 + Ant Design 5（`web/admin`）
- 存储：PostgreSQL（默认）/ Redis / MongoDB（可选）
- 详见根目录 [`AI_CONTEXT.md`](../AI_CONTEXT.md)

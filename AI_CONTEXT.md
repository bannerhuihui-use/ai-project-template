# AI 协作上下文（AI_CONTEXT）

> fork 新项目时**首先填写**本文件。模板默认能力见 [`docs/TEMPLATE.md`](docs/TEMPLATE.md)。

---

## 1. 项目名称

`<待填写，如 xxx 管理平台>`

---

## 2. 项目简介

`<一句话说明：这个项目是做什么的、给谁用、解决什么问题>`

---

## 3. 技术栈

**后端**

- Java 17
- Spring Boot 3
- PostgreSQL（默认数据源）
- Redis（JWT 黑名单）
- MongoDB（集中式日志，可选）

**前端**

- React 18 + TypeScript + Vite
- Ant Design 5（`web/admin`）

**其他**（按需删除）

- RabbitMQ
- 阿里云 OSS（file-core）

---

## 4. 模块

模板已内置（一般无需重写 PRD）：

| 模块 | 说明 | 文档 |
|------|------|------|
| auth-core | JWT + RBAC + 用户管理 | [server/README.md](server/README.md) |
| config-core | 系统配置 | [docs/DB/SCHEMA.md](docs/DB/SCHEMA.md) |
| file-core | 文件上传 local/oss | [server/README.md](server/README.md) |
| web/admin | 通用后台壳 | [web/admin/README.md](web/admin/README.md) |

**新增业务模块**（在此列出）：

- `<module-a>` — `<说明>`

---

## 5. 核心业务流程

`<用 3-5 句话描述项目核心业务链路>`

---

## 6. AI 协作要求

- 优先分析需求 → 输出设计方案 → 确认后再编码
- 新增模块：PRD → DB → API → 风险分析 → 确认 → 编码
- 规范见 `.cursorrules`

---

## 7. 编码规范要点

- 统一返回 `Result<T>` / `PageResult<T>`
- Controller 只做参数校验；Service 业务；Repository 数据访问
- 禁止硬编码、`System.out.println`
- 鉴权走 JWT + `@PreAuthorize`，详见 `.cursorrules`

---

## 8. 相关文档

| 文档 | 说明 |
|------|------|
| [docs/TEMPLATE.md](docs/TEMPLATE.md) | 模板当前能力 |
| [docs/DB/SCHEMA.md](docs/DB/SCHEMA.md) | 表结构与种子 |
| [docs/DB/template-full.sql](docs/DB/template-full.sql) | 全量 SQL |
| [docs/API/auth-v2.0-rbac.md](docs/API/auth-v2.0-rbac.md) | RBAC 接口契约 |
| [docs/README.md](docs/README.md) | 文档索引 |

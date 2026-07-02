# AI 项目模板 — 当前能力总览

> 本文档记录模板**当前已落地**的能力快照，作为 fork 新项目时的起点说明。  
> 权限/RBAC 等核心能力已稳定，特殊需求在现有基础上增量修改即可。

---

## 1. 模板定位

面向 **Java 17 + Spring Boot 3** 的后台管理系统脚手架，内置：

- 统一鉴权（JWT + RBAC + 动态菜单）
- 通用后台前端（React + Ant Design）
- 文件上传（本地 / 阿里云 OSS）
- 系统配置、审计日志、集中式日志（Mongo，可选）
- AI 协作规范（`.cursorrules` + 文档骨架）

**默认管理员**：用户名 `admin`，密码 `123456`（BCrypt 种子，**生产务必修改**）。

---

## 2. 目录结构

| 路径 | 说明 |
|------|------|
| `.cursorrules` | AI 编码规范（分层、日志、鉴权、Swagger 约定） |
| `AI_CONTEXT.md` | 新项目说明书模板（fork 后首先填写） |
| `docs/` | 设计文档（PRD / API / DB / DEPLOY / LOGGING） |
| `docs/TEMPLATE.md` | **本文件**：模板当前能力快照 |
| `docs/DB/SCHEMA.md` | 数据库表结构与种子数据说明 |
| `docs/DB/template-full.sql` | 全量 DDL + 种子 SQL（与 Flyway V1~V8 等价） |
| `server/` | Spring Boot 后端 |
| `web/admin/` | 通用后台前端 |

---

## 3. 后端模块

### 3.1 auth-core（鉴权）

- **用户模型**：`sys_user`（主体）+ `user_identity`（USERNAME/PHONE/EMAIL/WECHAT_MINIAPP 登录身份）
- **JWT**：access（默认 2h）+ refresh（默认 7d），Redis 黑名单（可关闭）
- **账号安全**：`token_version` 改密/禁用/强制下线即全令牌失效；refresh 轮换防重放
- **RBAC**：`sys_permission`（MENU/BUTTON/API/DATA）+ `sys_role_permission`
- **SUPER_ADMIN**：代码层短路全部权限码；种子 admin 默认绑定 SUPER_ADMIN + ADMIN

主要接口见 [`server/README.md`](../server/README.md) 与 [`docs/API/auth-v2.0-rbac.md`](./API/auth-v2.0-rbac.md)。

### 3.2 file-core（文件）

- 表：`sys_file`（元数据）
- 存储：`file.storage-type=local|oss`（`.env` 中 `FILE_STORAGE_TYPE`）
- 公开读：`GET /api/v1/public/files/{fileKey}/download`（仅 `access=public`）
- 业务分类：`avatar` / `image` 默认 public；`document` / `attachment` 默认 private

### 3.3 config-core（系统配置）

- 表：`sys_config`（键值配置，分组 site/auth/system）
- 后台页：`/system/config`（Logo 上传对接 file-core）

### 3.4 日志与审计

- 本地：`logs/app.log`、`logs/error.log`，`X-Trace-Id` 全链路
- 中央 Mongo（可选）：WARN+ERROR 入库，审计页查询
- 详见 [`docs/LOGGING/README.md`](./LOGGING/README.md)

---

## 4. 前端（web/admin）

- React 18 + TS + Vite + Ant Design 5 + Zustand + React Router 6
- 动态菜单 / 按钮权限 / 401 自动续签 / session 过期跳转登录
- 已落地页面：登录、Dashboard、用户管理、角色权限、系统配置、审计日志、改密

快速启动见 [`web/admin/README.md`](../web/admin/README.md)。

---

## 5. 数据库

| 表 | 用途 |
|----|------|
| `sys_user` | 用户主体 |
| `user_identity` | 登录身份 |
| `sys_role` / `sys_user_role` | 角色与绑定 |
| `auth_login_device` | 登录设备记录 |
| `sys_permission` / `sys_role_permission` | RBAC 权限 |
| `sys_config` | 系统配置 |
| `sys_file` | 文件元数据 |

- **推荐初始化**：配置 `.env` 后 `mvn spring-boot:run`，Flyway 自动执行 `V1~V8`
- **手动初始化**：`psql ... -f docs/DB/template-full.sql`
- 详细字段与种子见 [`docs/DB/SCHEMA.md`](./DB/SCHEMA.md)

---

## 6. 环境变量要点（server/.env）

| 变量 | 说明 |
|------|------|
| `POSTGRES_*` | PostgreSQL 连接（默认数据源） |
| `JWT_SECRET` | JWT 密钥（生产必填，≥32 字符） |
| `REDIS_*` | Redis（令牌黑名单，建议启用） |
| `FILE_STORAGE_TYPE` | `local` 或 `oss` |
| `OSS_*` | 阿里云 OSS（`oss` 模式必填） |
| `LOG_MONGO_*` | 中央日志 Mongo（可选） |

完整示例见 `server/.env.example`。

---

## 7. 基于模板 fork 新项目

1. 复制仓库，重置 git 历史，填写 `AI_CONTEXT.md`
2. 全局替换包名 / 项目名（见根 `README.md`「初始化新项目时需要改的名字」）
3. 修改 `.env`（数据库、JWT、OSS 等）
4. 启动后端 + 前端，用 `admin/123456` 验证
5. 按业务增量：新增菜单/权限种子 → 后端 Controller/Service → 前端页面

**权限扩展流程**（大多数项目够用）：

1. 在 `sys_permission` 插入 MENU/API 权限码
2. `sys_role_permission` 给 ADMIN 或目标角色授权
3. Controller 加 `@PreAuthorize("@perm.has('xxx')")`
4. 前端用 `<Permission code="xxx">` 控制按钮

---

## 8. 刻意未实现（可按需扩展）

- 微信登录真实对接（接口占位）
- 字典模块、导入导出
- 登录 brute-force 限流
- 后台「文件管理」菜单页
- OSS 前端直传 presign

---

## 9. 相关文档索引

| 文档 | 说明 |
|------|------|
| [docs/README.md](./README.md) | 文档中心 |
| [docs/DB/SCHEMA.md](./DB/SCHEMA.md) | 表结构 + 种子数据 |
| [docs/DB/MIGRATIONS.md](./DB/MIGRATIONS.md) | Flyway 迁移说明 |
| [docs/API/auth-v2.0-rbac.md](./API/auth-v2.0-rbac.md) | RBAC 接口契约 |
| [server/README.md](../server/README.md) | 后端启动与鉴权详解 |
| [web/admin/README.md](../web/admin/README.md) | 前端说明 |

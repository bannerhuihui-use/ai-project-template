# Admin Console（通用后台管理前端）

可复用的中后台壳子，配套本仓库 `server` 的鉴权/RBAC 能力。基于 **React 18 + TS + Vite + Ant Design 5 + React Router 6 + Zustand + Axios**。

设计风格取自 `docs/UI/.../DESIGN.md`（Tech Blue / Inter / 8px 栅格），已映射到 antd 主题 token。

## 已实现（阶段 0~5）

- 登录 / 退出 / 修改密码（对接 `server` 鉴权接口）
- access/refresh 自动续签（401 单飞刷新后重放原请求；刷新失败清会话回登录）
- 动态菜单：侧边栏 + 路由 + 面包屑全部由 `GET /api/v1/auth/menus` 下发
- 按钮级权限：`<Permission code="...">` 组件 + `usePermission()`，`SUPER_ADMIN` 前端短路
- **用户管理页**（`/system/user`）：分页、筛选、新建、编辑、禁用/启用、重置密码、强制下线
- **角色权限页**（`/system/role`）：选角色、勾选权限码、保存覆盖授权
- **Dashboard**（`/dashboard`）：真实统计（用户数/角色数/权限点数/鉴权探活）+ 快捷入口 + 最近审计日志预览
- **审计日志页**（`/system/audit`）：traceId/级别/关键字/时间筛选，对接 `GET /api/v1/auth/admin/audit/logs`
- **用户管理**：支持查看用户「登录设备」记录
- 统一错误处理：401 续签 / 403 无权限 / 5xx 服务异常 / 业务码 toast
- 403 / 404 / 500 异常页、未实现菜单的占位页

## 目录结构

```
src/
├── api/        request 拦截器 + 各业务 api
├── auth/       token 管理、Permission 组件、权限 hook
├── store/      会话 store（user/menus/permissionCodes）
├── layout/     AdminLayout（侧栏/顶栏/面包屑）+ icon 映射
├── router/     路由装配、动态菜单展平、页面注册表
├── pages/      login / dashboard / profile / error / common
├── theme/      antd 主题 token（映射 DESIGN.md）
├── types/      Result/PageResult/Menu 等类型
└── utils/      全局反馈（message/modal）
```

## 环境变量

Vite 按 **mode** 自动加载，**无需**再 `cp .env.example .env`：

| 文件 | 何时加载 | 是否提交 |
|------|----------|----------|
| `.env.development` | `npm run dev` | ✅ 仓库内 |
| `.env.production` | `npm run build` | ✅ 仓库内 |
| `.env.local` | 上述两种都会叠加 | ❌ gitignore |
| `.env.development.local` | 仅 dev | ❌ gitignore |

优先级（高 → 低）：`.env.[mode].local` > `.env.[mode]` > `.env.local`

| 变量 | 说明 |
|------|------|
| `VITE_API_BASE_URL` | Axios 基础路径，默认 `/api`（生产走 Nginx 同源反代） |
| `VITE_PROXY_TARGET` | **仅开发**：Vite 把 `/api` 代理到此地址，默认 `http://localhost:8080` |
| `VITE_TOKEN_STORAGE` | `local` / `session` |

后端不在本机 8080 时，新建 `.env.local` 覆盖即可，例如：

```bash
echo 'VITE_PROXY_TARGET=http://192.168.1.10:8080' > .env.local
```

## 快速开始

```bash
cd web/admin
npm install
npm run dev            # http://localhost:5173，自动读 .env.development
```

生产构建（自动读 `.env.production`）：

```bash
npm run build          # 产物在 dist/，由 Nginx 托管
npm run preview        # 本地预览生产包
```

后端默认 `http://localhost:8080`，登录账号见 `server` 种子数据（`admin / 123456`）。

## 与后端接口对应

| 功能 | 接口 |
|---|---|
| 登录 | `POST /api/v1/auth/admin/login` |
| 刷新 | `POST /api/v1/auth/token/refresh` |
| 当前用户 | `GET /api/v1/auth/me` |
| 菜单树 | `GET /api/v1/auth/menus` |
| 按钮权限码 | `GET /api/v1/auth/permissions/buttons` |
| 改密 | `POST /api/v1/auth/password/change` |
| 退出 | `POST /api/v1/auth/logout` |
| 用户分页 | `GET /api/v1/auth/admin/users` |
| 新建/编辑用户 | `POST/PUT /api/v1/auth/admin/users` |
| 重置密码 | `POST /api/v1/auth/admin/users/{id}/reset-password` |
| 启用/禁用/强制下线 | `POST .../enable`、`.../disable`、`.../force-logout` |
| 登录设备 | `GET /api/v1/auth/admin/users/{id}/login-devices` |
| 角色下拉 | `GET /api/v1/auth/admin/roles` |
| 审计日志 | `GET /api/v1/auth/admin/audit/logs` |

## 审计与集中日志

- 后端 `GET /api/v1/auth/admin/audit/logs` 查询 Mongo `central_logs.app_logs`（字段见 `docs/LOGGING/README.md`）
- 需开启 `LOG_MONGO_ENABLED=true` 并配置 `LOG_MONGO_URI`；TTL 由 `LOG_MONGO_TTL_DAYS` 控制，建议与系统配置「审计保留天数」一致
- 配置/RBAC 变更日志关键字：`配置变更审计`、`RBAC变更审计`

## 复用到新项目

1. 复制 `web/admin` 到新仓库（或新建 `web/<app>`）。
2. 按需改 `.env.development` / `.env.production`；个人差异用 `.env.local`。
3. 后端配好菜单与权限码种子数据，前端无需改路由即可动态出菜单。
4. 新增页面：在 `src/pages` 下按菜单 `component` 路径建文件即可被自动加载。

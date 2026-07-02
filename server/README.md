# template-server 后端服务

AI 项目初始化模板的后端服务，基于 **Java 17 + Spring Boot 3.3.5 + Maven**。

## 技术栈

- Java 17 / Spring Boot 3.3.5 / Maven
- JDBC（MySQL、PostgreSQL 驱动常驻）、MongoDB、Redis、RabbitMQ（按需启用）
- springdoc-openapi（Swagger UI 在线文档与调试）

## 环境准备

- JDK 17+
- Maven 3.8+
- 敏感配置统一放在 `server/.env`，`application.yml` 通过 `${KEY}` 引用。
  `.env` 已在 `.gitignore` 中，**不要提交**。

## 如何启动

在 `server` 目录下执行：

```bash
# 方式一：Maven 插件启动（开发推荐）
mvn spring-boot:run

# 方式二：打包后运行
mvn clean package
java -jar target/template-server-0.0.1-SNAPSHOT.jar
```

默认端口 `8080`（可在 `.env` 的 `SERVER_PORT` 修改），默认 profile 为 `local`。

> 提示：未启用的中间件（如数据库未就绪）可能影响启动，可在 `.env`/`application.yml` 中按需调整数据源配置。

## 接口文档（Swagger / OpenAPI）

服务启动后访问：

| 用途 | 地址 |
|------|------|
| Swagger UI（可视化 + 在线调试） | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| OpenAPI YAML | http://localhost:8080/v3/api-docs.yaml |

### 如何用 Swagger 测试接口

1. 浏览器打开 http://localhost:8080/swagger-ui.html
2. 在顶部 **过滤框** 输入关键字可快速定位接口（已开启 filter）。
3. 展开某个接口（如「示例接口」下的 `GET /api/v1/hello`），查看参数说明、示例值与响应示例。
4. 点击 **Try it out**（已默认开启），填写/修改参数，点击 **Execute** 发送真实请求。
5. 在 **Responses** 区查看返回结果、HTTP 状态码与 **请求耗时**（已开启 request duration）。
6. 接口与标签均按字母排序（alpha），便于查找。

### 自带示例接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/hello` | GET | 问候接口，参数 `name`（可选，默认 `world`） |
| `/api/v1/health` | GET | 健康检查，返回 `app` / `profile` / `status` / `time` |

## 生产环境安全建议

Swagger UI 会暴露全部接口结构与在线调试能力，**生产环境必须关闭或加鉴权**，建议任选其一：

- **关闭文档**：在生产 profile（如 `application-prod.yml`）中设置：

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

- **加访问鉴权**：通过 Spring Security / 网关对 `/swagger-ui/**`、`/v3/api-docs/**` 做 IP 白名单或登录鉴权后再放行。

## JWT 鉴权（auth-core）

统一鉴权能力：统一用户主体 `sys_user` + 统一登录身份 `user_identity`，登录签发 JWT，接口按 `user_type`（ADMIN/MEMBER）保护。表设计见 [`docs/DB/SCHEMA.md`](../docs/DB/SCHEMA.md)。

### 准备：数据库迁移（Flyway）

**新环境**配置好 `.env` 中的 PostgreSQL 后，直接 `mvn spring-boot:run`，Flyway 会自动执行 `src/main/resources/db/migration/`（V1~V8）。

详细说明与「已手动执行过旧 SQL」的 baseline 方案见 [`docs/DB/MIGRATIONS.md`](../docs/DB/MIGRATIONS.md)。

全量 SQL 快照：[`docs/DB/template-full.sql`](../docs/DB/template-full.sql)（与 Flyway V1~V8 等价）。勿与 Flyway 重复手动执行。

初始管理员示例口令为 `123456`（BCrypt 密文，见 V1 种子）。**生产务必重置**：

```java
// 任意 Java 环境执行，拿到密文后 UPDATE user_identity.credential
new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("你的新密码");
```

### 登录接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/auth/admin/login` | POST | 后台账号密码登录，返回 access + refresh 双令牌 |
| `/api/v1/auth/wechat/login` | POST | 微信登录（v1 占位，返回 50010） |
| `/api/v1/auth/token/refresh` | POST | 用 refresh 换新双令牌（轮换并拉黑旧 refresh），免登录头 |
| `/api/v1/auth/me` | GET | 获取当前登录用户（需 access token） |
| `/api/v1/auth/password/change` | POST | 修改当前用户密码（需 access token），成功后旧令牌全失效并续签返回新双令牌 |
| `/api/v1/auth/logout` | POST | 退出登录（拉黑当前 access；可选体携带 refreshToken 一并拉黑） |
| `/api/v1/auth/admin/ping` | GET | 管理员示例受保护接口（仅 ROLE_ADMIN） |
| `/api/v1/auth/admin/users` | GET/POST | 用户分页 / 新建（`auth:user:read` / `auth:user:create`） |
| `/api/v1/auth/admin/users/{id}` | GET/PUT | 用户详情 / 编辑（`auth:user:read` / `auth:user:update`） |
| `/api/v1/auth/admin/users/{id}/reset-password` | POST | 重置密码（`auth:user:reset_password`） |
| `/api/v1/auth/admin/users/{id}/enable` | POST | 启用账号（`auth:user:update`） |
| `/api/v1/auth/admin/roles` | GET | 角色下拉列表（`auth:user:read`） |
| `/api/v1/auth/admin/users/{id}/disable` | POST | 禁用账号（`auth:user:disable`），账号停用且旧令牌全失效；不能禁用自己 |
| `/api/v1/auth/admin/users/{id}/force-logout` | POST | 强制下线（`auth:user:force_logout`），仅使旧令牌全失效，不改账号状态 |
| `/api/v1/auth/menus` | GET | 当前用户菜单树（需 access token，前端动态路由/面包屑） |
| `/api/v1/auth/permissions/buttons` | GET | 当前用户按钮/接口权限码（前端按钮显隐） |
| `/api/v1/auth/admin/permissions` | GET | 权限点分页（`auth:permission:read`） |
| `/api/v1/auth/admin/roles/{roleId}/permissions` | GET/PUT | 查询/覆盖设置角色权限（`auth:role:read` / `auth:role:grant_permission`） |
| `/api/v1/auth/admin/users/{userId}/roles` | PUT | 覆盖设置用户角色（`auth:user:grant_role`） |
| `/api/v1/auth/admin/rbac/ping` | GET | RBAC 探活（`auth:rbac:ping`） |

登录示例：

```bash
curl -X POST http://localhost:8080/api/v1/auth/admin/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

### 访问受保护接口（Authorization 头）

登录拿到 `accessToken` 后，后续请求带上请求头：

```
Authorization: Bearer <accessToken>
```

```bash
curl http://localhost:8080/api/v1/auth/me -H "Authorization: Bearer <accessToken>"
```

刷新令牌（免 Authorization 头）：

```bash
curl -X POST http://localhost:8080/api/v1/auth/token/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'
```

退出登录（同时拉黑 access 与 refresh）：

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'
```

修改密码（成功后旧令牌全失效，响应直接返回续签的新双令牌）：

```bash
curl -X POST http://localhost:8080/api/v1/auth/password/change \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"oldPassword":"123456","newPassword":"NewPass@2026"}'
```

管理员禁用账号 / 强制下线（仅 ROLE_ADMIN）：

```bash
curl -X POST http://localhost:8080/api/v1/auth/admin/users/2/disable \
  -H "Authorization: Bearer <adminAccessToken>"

curl -X POST http://localhost:8080/api/v1/auth/admin/users/2/force-logout \
  -H "Authorization: Bearer <adminAccessToken>"
```

在 Swagger UI 点右上角 **Authorize**，填入 access token 即可在线调试受保护接口。

### Token 生命周期（v1.2）

- **access vs refresh**：
  - **access token**（短，默认 2h）：访问业务接口，放在 `Authorization: Bearer <accessToken>`。
  - **refresh token**（长，默认 7d）：**只能**调用 `POST /api/v1/auth/token/refresh` 换新令牌，不能访问其它接口（拿它访问普通接口返回 40106）。
- **刷新流程**：用 refresh 调刷新接口 → 校验通过后签发新的 access + refresh，并**拉黑旧 refresh**（轮换防重放）。
- **Redis 黑名单**：按 `jti` 让单个令牌立即失效，key 形如 `auth:token:blacklist:{jti}`，TTL=令牌剩余有效期。
- **logout 为什么可带 refreshToken**：access 通过头携带必被拉黑；但 refresh 在客户端另存，需在请求体显式带上 `{"refreshToken":"..."}` 才能一并拉黑，确保退出后两者都不可用。
- **token_version 让旧 token 全失效**：每请求比对 JWT 与 `sys_user.token_version`，不一致即拒（40107）。
  改密 / 禁用 / 强制下线只需：`UPDATE sys_user SET token_version = token_version + 1 WHERE id = ?`，该用户所有未过期令牌立即作废。
- **登录设备表**：`auth_login_device` 记录设备 / IP / UA（防御式，失败不影响登录），为后续多设备管理、踢下线打基础。

### Redis 不可用时的策略（重要）

- 鉴权查黑名单时 Redis 异常 → **fail-closed**：拒绝访问，返回 HTTP 503 `AUTH_SERVICE_UNAVAILABLE(50301)`，避免被拉黑令牌因 Redis 故障而"复活"。
- **刷新轮换的强一致**：黑名单启用时，刷新流程会先拉黑旧 refresh 再签发新令牌；若此时拉黑失败（Redis 异常），刷新会直接返回 50301 **拒绝刷新**（不签发新令牌），避免旧 refresh 仍可重放。
- 因此默认 `AUTH_TOKEN_ENABLE_BLACKLIST=true` 时**应保证 Redis 可用**；若环境不想依赖 Redis，可设 `AUTH_TOKEN_ENABLE_BLACKLIST=false`：
  - 关闭后鉴权不查黑名单、拉黑为 no-op；
  - `logout` 自动回退为 `token_version` 自增（整账号失效，而非单令牌）；
  - **refresh 轮换的防重放能力退化**：旧 refresh 不会被拉黑，仅依赖**较短的 refresh/access TTL** + `token_version` 来限制重放风险；
  - 此时整体安全性依赖**较短的 access TTL** + `token_version`。

### 账号安全闭环（v1.2.1）

基于 `token_version` 把高风险动作统一收敛为「整账号令牌失效」：

- **修改密码** `POST /api/v1/auth/password/change`：
  - userId 取自登录上下文（**不接受**请求体传 userId，防越权改他人密码）。
  - 校验原密码 → 写入新密码（BCrypt）→ `password_updated_at` 刷新 → `token_version + 1`。
  - 新密码规则：至少 8 位（`@Size`），且不得与原密码相同（40109）；原密码错误返回 40108。
  - **改密即续签**：成功后此前所有令牌失效，响应直接返回新的 access + refresh，无需重新登录。
- **禁用账号** `POST /api/v1/auth/admin/users/{id}/disable`：置 `status=DISABLED` + `token_version + 1`；**不能禁用自己**（40302），目标不存在返回 40400；已禁用幂等。
- **强制下线** `POST /api/v1/auth/admin/users/{id}/force-logout`：仅 `token_version + 1`（不改状态），用于强制重新登录；幂等。
- **admin 接口授权**：`SecurityConfig` 对 `/api/v1/auth/admin/**` 统一要求 `ROLE_ADMIN`（`admin/login` 在白名单且匹配在前，仍放行）；非管理员访问返回 40300。
- **refresh 重放处置（B2）**：刷新时若 refresh 的 `jti` 已在黑名单（即已被轮换/登出），视为疑似重放/盗用，**强制 `token_version + 1`** 让整账号失效后再拒绝（返回 40105）。

### 菜单权限 + RBAC（v2.0）

统一权限中心 `sys_permission`（`MENU/BUTTON/API/DATA`）+ `sys_role_permission`，设计详见
[`docs/DB/SCHEMA.md`](../docs/DB/SCHEMA.md)、[`docs/API/auth-v2.0-rbac.md`](../docs/API/auth-v2.0-rbac.md)。

- **三类权限协同**：
  - `MENU`：菜单/路由元数据（`path/name/component/redirect/icon/breadcrumb/clickable/...`），前端动态路由、侧边栏、面包屑直接用。
  - `BUTTON`/`API`：按钮显隐与接口动作权限，编码前后端共用一套（如 `auth:user:disable`）。
  - `DATA`：数据范围权限（`data_scope_code` 预留，v2.1 落地）。
- **前端取数**：登录后 `GET /api/v1/auth/menus` 取菜单树、`GET /api/v1/auth/permissions/buttons` 取按钮权限码（均后端按角色过滤）。
- **双层鉴权**：路径级 `/api/v1/auth/admin/**` 要求 `ROLE_ADMIN`；方法级 `@PreAuthorize("@perm.has('权限码')")` 校验具体权限。
- **权限码来源（C2）**：access token **只放 roles**，按钮/接口权限码鉴权时按 `userId` 查库注入 authorities（不放 JWT，避免请求头膨胀、避免微调即强制全员重登）。
- **SUPER_ADMIN 短路**：拥有 `ROLE_SUPER_ADMIN` 视为具备全部权限码（仍受登录态/账号状态/`token_version` 约束），用于解决“配权限的人自己没权限”的引导死锁。
- **授权失败口径**：`hasRole/hasAuthority` 拒绝统一 `40300`；业务校验类用 `40401`（角色不存在）/`40402`（权限编码非法）/`40010`（参数非法）。
- **变更即时生效**：角色权限覆盖 → 该角色下所有用户 `token_version + 1`；用户角色覆盖 → 该用户 `token_version + 1`。覆盖式更新在**单事务**内“先软删后插入”。
- **防引导死锁**：Flyway V1 已种子化 `SUPER_ADMIN` 角色（绑定初始 admin）、权限点、ADMIN 默认授权、基础菜单；**种子先行，`@PreAuthorize` 最后开启**。

RBAC 管理示例（需管理员令牌）：

```bash
# 权限点分页
curl "http://localhost:8080/api/v1/auth/admin/permissions?pageNum=1&pageSize=10&type=MENU" \
  -H "Authorization: Bearer <adminAccessToken>"

# 覆盖设置角色权限（空数组=清空）
curl -X PUT http://localhost:8080/api/v1/auth/admin/roles/1/permissions \
  -H "Authorization: Bearer <adminAccessToken>" -H "Content-Type: application/json" \
  -d '{"permissionCodes":["menu:auth:user","auth:user:disable"]}'

# 覆盖设置用户角色
curl -X PUT http://localhost:8080/api/v1/auth/admin/users/2/roles \
  -H "Authorization: Bearer <adminAccessToken>" -H "Content-Type: application/json" \
  -d '{"roleCodes":["ADMIN","OPERATOR"]}'
```

### 设计与安全要点

- **JWT 内容**：含 `jti / userId / userType / identityType / tokenType / tokenVersion`（access 另含 `roles`）；**不放**密码、openid、session_key、手机号明文。
- **密钥**：`JWT_SECRET` 经 SHA-256 派生为 32 字节满足 HS256；生产必须用足够复杂的密钥并通过 `.env`/环境变量注入，`prod` 启动会强制校验（空/默认/过短直接失败）。
- **禁用即失效**：过滤器每次请求查库校验用户状态 + token 版本，被禁用 / 改密 / 强制下线的用户立即无法通过鉴权。
- **不打印敏感信息**：任何日志都不打印 token / password / session_key。
- **统一错误**：Token 无效/未认证/无权限/类型错误/已失效均返回统一 `Result`（40100/40103/40104/40105/40106/40107/40300/50301 等）。
- **微信登录为何是占位**：v1 不真实请求微信接口，仅先建好 DTO/接口/表结构/Swagger；后续在 `wechat-miniapp` 模块实现 `code2session`，登录身份写入 `user_identity(WECHAT_MINIAPP)`，复用同一 `sys_user` 与令牌签发逻辑。

## 日志与链路追踪

统一日志能力已内置：`logback-spring.xml` + `TraceIdFilter` + `AccessLogFilter`。

### 日志文件位置

默认输出到 `logs/` 目录（可通过 `.env` 的 `LOG_DIR` 覆盖）：

| 文件 | 内容 |
|------|------|
| `logs/app.log` | 全量日志（INFO 及以上，业务包 DEBUG） |
| `logs/error.log` | 仅 ERROR 级别，便于错误排查 |
| 控制台 | 同步输出，开发期直接查看 |

滚动策略：按天 + 单文件 100MB 切割，压缩归档（`.gz`），保留 30 天，总量上限 20GB。

日志格式：`时间 级别 [应用名] [环境] [traceId] [线程] logger - message` + 异常堆栈。

### traceId 使用方式

- 每个请求自动生成 traceId 并写入 MDC，贯穿该请求的所有日志。
- 客户端可通过请求头 `X-Trace-Id` 传入自定义 traceId（仅允许字母、数字、连字符，最长 64 位；非法值会被服务端忽略并重新生成，防止日志注入）。
- 响应头会回写 `X-Trace-Id`，前端 / 调用方可据此关联后端日志。

```bash
# 携带自定义 traceId 调用
curl -H "X-Trace-Id: my-trace-001" http://localhost:8080/api/v1/hello
# 响应头将包含 X-Trace-Id: my-trace-001
```

### 如何根据 X-Trace-Id 排查问题

1. 从前端报错、网关日志或响应头拿到 `X-Trace-Id`。
2. 在日志中检索该 traceId，即可串起这次请求的访问日志、业务日志与异常堆栈：

```bash
grep "my-trace-001" logs/app.log
grep "my-trace-001" logs/error.log
```

### 日志级别约定

| 级别 | 使用场景 |
|------|----------|
| `info` | 关键业务流程节点、访问日志 |
| `warn` | 可预期 / 可恢复异常（参数校验失败、业务异常） |
| `error` | 系统异常，需要告警与排查 |

> 禁止 `System.out.println` 与 `e.printStackTrace()`，禁止打印 password / token / authorization 等敏感信息（详见根目录 `.cursorrules`「日志规范」）。

### 集中式日志（中央 MongoDB，可选）

模板内置将 **WARN+ERROR** 日志写入中央 MongoDB 的能力，多项目按 `service` 集中查询，**默认关闭**。
设计与字段/索引详见 [`docs/logging/README.md`](../docs/logging/README.md)。

开启方式（在 `.env` 配置**独立的中央日志库**，勿复用业务库）：

```bash
LOG_MONGO_ENABLED=true
LOG_MONGO_URI=mongodb://user:pwd@log-host:27017/?authSource=admin
# 其余 LOG_MONGO_DB / COLLECTION / TTL_DAYS / QUEUE_SIZE 有默认值，可选
```

特性：仅 WARN+ERROR 入库；异步写入、队列满即丢弃（不阻塞业务）；文件日志仍为兜底；
`timestamp` TTL 索引 30 天自动过期；忽略 Mongo 驱动自身日志防递归。查询示例：

```js
db.app_logs.find({ traceId: "a1b2c3" }).sort({ timestamp: 1 })   // 按链路串联（跨项目）
db.app_logs.find({ service: "order-service", level: "ERROR" }).sort({ timestamp: -1 })
```

> 数据量增长后，建议单独建一个后台管理项目对 `app_logs` 做统一查询页（本期不做）。

### 生产环境日志采集建议

- **方案一（轻量）**：Promtail 采集 `logs/*.log` → Loki 存储 → Grafana 查询/告警。
- **方案二（重型）**：Filebeat 采集 → Logstash 解析 → Elasticsearch 存储 → Kibana 检索（ELK）。
- **方案三（已内置）**：中央 MongoDB（见上），适合快速落地、多项目集中查询。
- 建议生产将日志格式切换为 JSON（便于结构化采集），并按 traceId 建立检索字段；业务包级别在 prod profile 收紧为 INFO。

## 文件上传（file-core）

- 模块包路径：`com.example.template.file`
- 存储：`file.storage-type=local`（默认，本地 `./data/uploads`）或 `oss`（阿里云 OSS，需配置 `OSS_*`）
- 启用 OSS 时在 `.env` 增加：`FILE_STORAGE_TYPE=oss`，并填写 `OSS_ENDPOINT`、`OSS_ACCESS_KEY_ID`、`OSS_ACCESS_KEY_SECRET`、`OSS_BUCKET`；可选 `OSS_PUBLIC_BASE_URL` 自定义 CDN/域名
- 主要 API：
  - `POST /api/v1/files/upload`（需 `file:upload`）
  - `GET /api/v1/public/files/{fileKey}/download`（仅 `access=public` 文件，已在白名单）
- `avatar` / `image` 默认 `public`；`document` / `attachment` 默认 `private`
- Flyway：`V8__file_core.sql`（`sys_file` 表 + 权限种子）

## 测试

```bash
mvn test
```

## 接口规范

新增接口的设计与 Swagger 注解约定见根目录 `.cursorrules` 与 `docs/API/README.md`，
所有 Controller 接口必须补充完整的 Swagger/OpenAPI 注解（参数、响应、错误码、示例值）。

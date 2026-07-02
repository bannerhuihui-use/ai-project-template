# API 接口设计文档

> 接口规范与 Swagger 约定。模板已落地的 RBAC 契约见 [`auth-v2.0-rbac.md`](./auth-v2.0-rbac.md)。

## 0. 接口设计规范（必读）

1. **新增接口前先设计**：在本目录按模板补充 Method/Path、参数、响应、错误码。
2. **编码后同步 Swagger**：`@Tag`、`@Operation`、`@Schema`、`@ApiResponse` 等。
3. **Swagger 与文档一致**：在线文档为联调真源。

> Swagger UI：`http://localhost:8080/swagger-ui.html`  
> OpenAPI JSON：`http://localhost:8080/v3/api-docs`

## 1. 接口规范

- 统一返回：`Result<T>`；分页：`PageResult<T>`
- RESTful 路径：`/api/v1/...`
- 鉴权：`Authorization: Bearer <accessToken>`
- 受保护接口 Swagger 标注 `@SecurityRequirement(name="bearerAuth")`

### 通用响应码

| code | 含义 |
|------|------|
| 0 | 成功 |
| 400xx | 参数 / 业务校验失败 |
| 401xx | 未认证 / 令牌无效 |
| 40300 | 无权限 |
| 500xx | 服务端异常 |
| 50301 | Redis 不可用（鉴权 fail-closed） |

## 2. 已落地接口索引

### 鉴权（auth-core）

详见 [`../server/README.md`](../server/README.md)「JWT 鉴权」章节，包括：

- 登录 / 刷新 / 退出 / 改密 / me
- 用户 CRUD、禁用、强制下线、重置密码
- 菜单树、按钮权限、RBAC 管理

### RBAC 契约

[`auth-v2.0-rbac.md`](./auth-v2.0-rbac.md) — 菜单树、权限分页、角色/用户授权等请求响应格式。

### 文件（file-core）

| 方法 | 路径 | 权限 |
|------|------|------|
| POST | `/api/v1/files/upload` | file:upload |
| GET | `/api/v1/files/{fileKey}/download` | file:read |
| DELETE | `/api/v1/files/{fileKey}` | file:delete |
| GET | `/api/v1/public/files/{fileKey}/download` | 公开（白名单） |

### 系统配置

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/api/v1/auth/admin/configs` | auth:config:read |
| PUT | `/api/v1/auth/admin/configs/{key}` | auth:config:update |
| POST | `/api/v1/auth/admin/configs` | auth:config:create |
| DELETE | `/api/v1/auth/admin/configs/{key}` | auth:config:delete |

## 3. 新业务接口模板

### API-XXX `<接口名>`

- **Method / Path**：`POST /api/v1/<...>`
- **说明**：`<功能描述>`
- **请求头**：`Authorization: Bearer <token>`

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `<field>` | String | 是 | `<说明>` |

**错误码**

| code | 说明 |
|------|------|
| 40001 | `<参数错误>` |

## 4. Swagger 导出

- Swagger UI：http://localhost:8080/swagger-ui.html
- OpenAPI JSON：http://localhost:8080/v3/api-docs
- OpenAPI YAML：http://localhost:8080/v3/api-docs.yaml

> 生产环境需关闭或加鉴权，见 `server/README.md`。

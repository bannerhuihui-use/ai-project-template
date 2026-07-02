# auth v2.0 API 设计（RBAC）

> 📌 定位：本文件是 **RBAC 接口契约真源**（端点、字段、分页、错误码）。  
> 表结构与种子见 [`../DB/SCHEMA.md`](../DB/SCHEMA.md)；模板总览见 [`../TEMPLATE.md`](../TEMPLATE.md)。
>
> 状态：✅ 已落地

## 1. 设计目标

- 在现有鉴权接口基础上补齐“菜单 + 角色-权限管理”API。
- 让权限从“仅角色”升级为“角色 + 权限码 + 菜单元数据”三层协同。
- 保持统一响应 `Result<T>`，分页用 `PageResult<T>`，并同步 Swagger。

## 1.1 鉴权与口径约定（先读）

- access token 只携带 `roles`，**不放权限码**；权限码由后端按 `userId` 查库 + 缓存加载（决策 C2）。
- `SUPER_ADMIN` 角色短路所有 `hasAuthority` 校验（仍需登录态/账号状态/`token_version` 通过）。
- 授权失败口径统一：`hasRole/hasAuthority` 拒绝 → `RestAccessDeniedHandler` 返回 **`40300`（HTTP 403）**；
  未认证 → `40100/40103/40107`（HTTP 401）。**不另设“缺少权限”错误码**。

## 2. 接口清单

| 编号 | 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|------|
| API-RBAC-001 | 我的菜单树 | GET | `/api/v1/auth/menus` | 当前登录用户可访问菜单树（含路由元数据） |
| API-RBAC-002 | 我的按钮权限 | GET | `/api/v1/auth/permissions/buttons` | 当前用户按钮/API 权限码列表 |
| API-RBAC-003 | 权限列表 | GET | `/api/v1/auth/admin/permissions` | 分页查询权限点（`PageResult<T>`，支持类型筛选） |
| API-RBAC-004 | 角色权限查询 | GET | `/api/v1/auth/admin/roles/{roleId}/permissions` | 查询角色当前权限 |
| API-RBAC-005 | 角色权限覆盖设置 | PUT | `/api/v1/auth/admin/roles/{roleId}/permissions` | 用权限编码列表覆盖角色权限 |
| API-RBAC-006 | 用户角色设置 | PUT | `/api/v1/auth/admin/users/{userId}/roles` | 设置用户角色（覆盖） |
| API-RBAC-007 | 权限探活接口 | GET | `/api/v1/auth/admin/rbac/ping` | 用于验证 `hasAuthority` 保护链路 |

> 所有 `/api/v1/auth/admin/**` 默认要求 `ROLE_ADMIN`，并在关键接口上增加 `hasAuthority(...)`。

## 3. 接口详情

### API-RBAC-001 我的菜单树

- **Method / Path**：`GET /api/v1/auth/menus`
- **鉴权**：已登录（access token）
- **说明**：返回当前用户可访问菜单树，后端已按角色过滤。前端直接用于动态路由、侧边栏、面包屑。

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "permissionCode": "menu:auth:user",
      "permissionType": "MENU",
      "title": "用户管理",
      "path": "/auth/user",
      "name": "AuthUser",
      "component": "/pages/auth/user/index",
      "redirect": "/auth/user/list",
      "icon": "icon-user",
      "breadcrumb": true,
      "clickable": true,
      "alwaysShow": false,
      "visible": true,
      "children": []
    }
  ]
}
```

### API-RBAC-002 我的按钮权限

- **Method / Path**：`GET /api/v1/auth/permissions/buttons`
- **鉴权**：已登录（access token）
- **说明**：返回按钮/API 权限编码数组，前端用于按钮显隐与禁用控制。

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    "auth:user:create",
    "auth:user:disable",
    "auth:user:force_logout"
  ]
}
```

### API-RBAC-003 权限列表（分页）

- **Method / Path**：`GET /api/v1/auth/admin/permissions`
- **鉴权**：`ROLE_ADMIN` + `hasAuthority('auth:permission:read')`
- **Query 参数**：`pageNum`(默认1)、`pageSize`(默认10)、`type`(可选：MENU/BUTTON/API/DATA)、`keyword`(可选，按 code/name 模糊)
- **响应**：`Result<PageResult<PermissionItem>>`

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 120,
    "pageNum": 1,
    "pageSize": 10,
    "list": [
      { "permissionCode": "auth:user:disable", "permissionName": "禁用用户", "permissionType": "API" }
    ]
  }
}
```

### API-RBAC-004 角色权限查询

- **Method / Path**：`GET /api/v1/auth/admin/roles/{roleId}/permissions`
- **鉴权**：`ROLE_ADMIN` + `hasAuthority('auth:role:read')`

### API-RBAC-005 角色权限覆盖设置

- **Method / Path**：`PUT /api/v1/auth/admin/roles/{roleId}/permissions`
- **鉴权**：`ROLE_ADMIN` + `hasAuthority('auth:role:grant_permission')`
- **说明**：采用“覆盖式”更新（幂等），提交空数组代表清空角色权限。可同时包含 MENU/BUTTON/API 类型权限码。
- **事务与校验**：整段在**单事务**内“先软删旧关联再插入新关联”；提交的 `permissionCodes` 必须全部存在且
  `status=NORMAL/deleted=0`，任一非法 → 回滚并返回 `40402 AUTH_PERMISSION_NOT_FOUND`；`roleId` 不存在 → `40401`。
- **生效**：成功后对该角色下所有用户 `token_version + 1`（量大可异步/分批），使旧令牌权限失效。

**请求示例**

```json
{
  "permissionCodes": [
    "menu:auth:user",
    "auth:user:disable",
    "auth:user:force_logout"
  ]
}
```

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

### API-RBAC-006 用户角色设置

- **Method / Path**：`PUT /api/v1/auth/admin/users/{userId}/roles`
- **鉴权**：`ROLE_ADMIN` + `hasAuthority('auth:user:grant_role')`
- **说明**：覆盖式更新用户角色，成功后建议触发 `token_version + 1`。

**请求示例**

```json
{
  "roleCodes": [
    "ADMIN",
    "OPERATOR"
  ]
}
```

### API-RBAC-007 权限探活接口

- **Method / Path**：`GET /api/v1/auth/admin/rbac/ping`
- **鉴权**：`ROLE_ADMIN` + `hasAuthority('auth:rbac:ping')`
- **说明**：用于端到端联调权限链路。

## 4. 错误码约定（在现有基础上扩展）

**授权不足统一复用 `40300(FORBIDDEN)`**：`hasRole/hasAuthority` 拒绝由 `RestAccessDeniedHandler` 处理，
不新增“缺少权限/菜单无权限”错误码（新增了也走不到，反而割裂）。

仅新增**业务校验类**错误码（这些在 Service 显式抛出，能正常返回）：

| code | 枚举建议 | 区间 | 说明 |
|------|----------|------|------|
| 40401 | AUTH_ROLE_NOT_FOUND | 404xx | 角色不存在 |
| 40402 | AUTH_PERMISSION_NOT_FOUND | 404xx | 权限（编码）不存在或已禁用 |
| 40010 | AUTH_ROLE_PERMISSION_INVALID | 400xx | 角色权限参数非法（如重复/空编码） |

> 说明：`@PreAuthorize` 失败抛 `AccessDeniedException` → 走 handler → `40300`，无法被这些业务码覆盖，故不重复定义。

## 5. Swagger 同步要求

- `Controller`：补齐 `@Tag`、`@Operation`、`@ApiResponses`。
- `DTO`：补齐 `@Schema` 示例值，禁止示例里放真实 token。
- 对关键错误码给出 `ExampleObject`，确保前后端联调一致。
- 菜单返回模型需明确字段：`path/name/component/redirect/icon/breadcrumb/clickable/children`。

## 6. 风险与控制

- **风险**：一次性给太多接口上权限校验，可能导致大量 403。
  - **控制**：灰度方式先覆盖 admin 关键接口，再逐步扩展。
- **风险**：角色权限变更后旧 access 仍可用到过期。
  - **控制**：角色/权限变更触发 `token_version + 1`。
- **风险**：权限命名不一致导致治理困难。
  - **控制**：强制执行 `{module}:{resource}:{action}` 命名规范。
- **风险**：菜单元数据与前端路由约定不一致，导致页面空白或跳转异常。
  - **控制**：统一菜单字段契约，先做一版前后端联调样例菜单。
- **风险（高）**：先开 `hasAuthority` 后补种子数据 → 管理员自锁，无人能配权限。
  - **控制**：种子数据先行 + `SUPER_ADMIN` 代码层短路（详见 DB 文档第 6 节）。
- **风险**：权限码放进 token 导致请求头膨胀、权限微调即强制全员重登。
  - **控制**：采用 C2，token 不放权限码，按 `userId` 查库 + 缓存加载。

# API 接口设计文档

> 用途：记录接口设计与接口规范，作为前后端联调与 Swagger 导出的依据。

## 1. 接口规范（约定）

- 统一返回结构：`Result<T>`
- 分页返回结构：`PageResult<T>`
- 风格：RESTful，路径小写中划线，例如 `/api/v1/user-center/users`
- 鉴权：`<如 JWT，放在 Authorization 头>`
- 时间格式：`yyyy-MM-dd HH:mm:ss`（统一时区 `<如 Asia/Shanghai>`）

### 统一返回示例

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### 通用响应码

| code | 含义 |
|------|------|
| 0 | 成功 |
| 400xx | 参数 / 业务校验失败 |
| 401xx | 未认证 / 未授权 |
| 500xx | 服务端异常 |

## 2. 接口清单

| 编号 | 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|------|
| API-001 | `<接口名>` | POST | `/api/v1/<...>` | `<一句话描述>` |

## 3. 接口详情（按此模板逐个填写）

### API-001 `<接口名>`

- **Method / Path**：`POST /api/v1/<...>`
- **说明**：`<功能描述>`
- **请求头**：`Authorization: Bearer <token>`

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `<field>` | String | 是 | `<说明>` |

**请求示例**

```json
{
  "field": "value"
}
```

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

**错误码**

| code | 说明 |
|------|------|
| 40001 | `<参数错误说明>` |

## 4. Swagger / OpenAPI 导出

`<导出文件位置或在线地址，如 /v3/api-docs 或 openapi.json>`

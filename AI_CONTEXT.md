# AI 协作上下文（AI_CONTEXT）

> 用途：这是 AI 协作的「项目说明书」，每次让 AI 介入前，请先把本文件补充完整。
> 它与根目录 `.cursorrules`（角色与编码规范）、`docs/`（PRD/API/DB/DEPLOY 文档）配合使用。
> 模板中所有「待填写」「<...>」均为占位符，正式使用时请替换或删除。

---

## 1. 项目名称

`<待填写，如 xxx 管理平台>`

---

## 2. 项目简介

`<一句话说明：这个项目是做什么的、给谁用、解决什么问题>`

---

## 3. 技术栈

后端：

- Java 17
- SpringBoot 3
- SpringCloud（如使用，否则删除）

数据库：

- MySQL
- MongoDB（如使用，否则删除）

缓存：

- Redis

MQ：

- RabbitMQ（如使用，否则删除）

---

## 4. 前端

`<待填写>`

例如：

- Vue3
- UniApp
- 微信小程序

---

## 5. 模块

> 列出本项目的业务模块，每个模块对应一套 PRD/API/DB 设计（详见 `docs/`）。

`<待填写>`

例如：

- user-center（用户中心）
- payment-center（支付中心）
- game-center（游戏中心）

---

## 6. 核心业务流程

`<用 3-5 句话或流程描述项目最核心的业务链路，帮助 AI 快速建立全局认知>`

---

## 7. AI 协作要求

- 优先分析需求
- 优先输出设计方案
- 确认后再生成代码
- 新增模块按 `.cursorrules` 流程：需求分析 → 模块设计 → 表设计 → API 设计 → 风险分析 → 确认 → 编码

---

## 8. 编码规范（要点）

- 统一返回：`Result<T>`；分页：`PageResult<T>`
- 统一异常处理、统一日志规范
- 禁止硬编码、禁止 `System.out.println`
- Controller 只做参数接收与校验，业务逻辑在 Service，数据访问在 Repository
- 遵循 SOLID、高内聚低耦合
- 详细规范见根目录 `.cursorrules`

---

## 9. 相关文档

| 文档 | 说明 |
|------|------|
| [docs/README.md](./docs/README.md) | 文档总索引 |
| [docs/PRD](./docs/PRD/README.md) | 产品需求 |
| [docs/API](./docs/API/README.md) | 接口设计 |
| [docs/DB](./docs/DB/README.md) | 数据库设计 |
| [docs/DEPLOY](./docs/DEPLOY/README.md) | 部署文档 |

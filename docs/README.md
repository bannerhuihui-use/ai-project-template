# 项目文档中心

> 本目录是 **AI 协作初始化模板** 的文档骨架。基于本模板新建项目后，请按各子目录的 `README.md` 模板逐项填写。

## 文档导航

| 模块 | 说明 | 入口 |
|------|------|------|
| PRD | 产品需求、业务流程、页面原型 | [PRD/README.md](./PRD/README.md) |
| API | 接口设计、接口规范、Swagger 导出 | [API/README.md](./API/README.md) |
| DB | 数据库设计、表结构、索引、ER 图 | [DB/README.md](./DB/README.md) |
| DEPLOY | 部署文档、服务器/Nginx/Docker 配置 | [DEPLOY/README.md](./DEPLOY/README.md) |

## 协作流程

本项目遵循「先分析、再设计、最后编码」的协作约定（详见根目录 `.cursorrules`）：

1. **需求分析** → 填写 `PRD`
2. **接口设计** → 填写 `API`
3. **表设计** → 填写 `DB`
4. **部署方案** → 填写 `DEPLOY`
5. 确认设计后再进入编码

## 技术栈速览

- 后端：Java 17 / SpringBoot 3 / SpringCloud
- 存储：MySQL / MongoDB / Redis
- 消息：RabbitMQ
- 详见根目录 `AI_CONTEXT.md`

---

> 填写约定：模板中所有 `<...>` 与「待填写」均为占位符，正式编写时请替换或删除。

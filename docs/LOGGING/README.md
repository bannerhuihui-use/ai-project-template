# 集中式日志（MongoDB）设计文档

> 状态：**已实现**
> 路线决策：路线 A（应用直连 Mongo 异步 appender）｜入库级别 WARN+ERROR｜TTL 30 天｜共享库按 `service` 区分。
> 落地：`MongoLogAppender` + `logback-spring.xml`(MONGO/MONGO_ASYNC) + `application.yml`(logging.mongo.*)，默认关闭。

---

## 1. 需求分析

### 1.1 背景

当前模板已具备：统一日志格式、traceId 链路、访问日志、`app.log` / `error.log` 文件滚动。
痛点：**出问题要登录每台机器翻日志文件**，多项目时无法集中查询。

### 1.2 目标

- 所有基于模板的项目，把 **WARN/ERROR** 日志写入**同一个共享 MongoDB**。
- 通过 `service`（项目名）+ `traceId` + `level` + 时间范围 **一站式查询**，不再翻文件。
- 对业务**零侵入、零阻塞**：日志能力是横切关注点，业务代码无感知。

### 1.3 非目标（本期不做）

- 不替代文件日志：`app.log`/`error.log` 仍是**事实来源与兜底**，Mongo 只是可查询副本。
- 不做全量 INFO/DEBUG 入库（量太大，省存储，聚焦问题排查）。
- 不在每个业务应用内做日志查询 UI（建议用 Compass / Grafana / 独立日志中心，见 §5）。

---

## 2. 模块设计

### 2.1 总体数据流

```
业务代码 log.warn/error
        │
        ▼
   logback root
        │  (ThresholdFilter: 仅 WARN+ERROR)
        ├──────────────► CONSOLE / app.log / error.log   （现有，事实来源/兜底）
        │
        ▼
   MONGO_ASYNC (AsyncAppender, neverBlock=true, 有界队列)
        │   队列满 → 直接丢弃，绝不阻塞业务线程
        ▼
   MongoLogAppender  ──► 共享 MongoDB（central_logs.app_logs）
        │
        └── 跳过 org.mongodb.* 的日志，防止写库动作自我放大（递归）
```

### 2.2 关键设计决策

| 决策点 | 方案 | 理由 |
|--------|------|------|
| 接入方式 | 自定义 logback appender（非第三方库） | 第三方 Mongo appender 对 Spring Boot 3 支持差、维护弱；自研可控 |
| 连接获取 | appender **自持** MongoClient（由 `logback-spring.xml` 经 `springProperty` 注入 URI） | logback 初始化早于 Spring 容器，不能依赖注入 MongoTemplate |
| 异步 | 包一层 logback `AsyncAppender`，`neverBlock=true`、有界 `queueSize` | 保证写库慢/挂时**丢日志而非拖垮业务** |
| 入库级别 | appender 内 `ThresholdFilter` = WARN | 只存需要排查的日志，省存储 |
| 防递归 | appender 内**忽略 `org.mongodb` 开头的 logger** | 写 Mongo 本身会产生驱动日志，否则死循环放大 |
| 开关 | `enabled` 属性默认 `false`，inert（不连接、不写库） | “默认关闭、配置开启”，避免引入额外 janino 依赖做条件装配 |
| 多项目 | 共享库，文档带 `service` 字段（=`spring.application.name`） | 一个集合存多项目，按 service 过滤 |
| 生命周期 | `start()` 懒连接 + 建索引；`stop()` 关闭 client | 资源正确释放（呼应 cursorrules “资源是否关闭”） |

### 2.3 目录结构（拟新增/改动）

```
server/
├── pom.xml                         # 新增 mongodb-driver-sync（appender 自持 client 用）
├── src/main/resources/
│   └── logback-spring.xml          # 改：新增 MONGO / MONGO_ASYNC appender（默认 inert）
└── src/main/java/com/example/template/common/logging/
    ├── MongoLogAppender.java       # 新增：核心 appender（异步队列后端 + 建索引 + 防递归）
    └── LogDocumentFactory.java     # 新增：ILoggingEvent → Document 映射（可选拆分，便于测试）
```

> 说明：`spring-boot-starter-data-mongodb` 已在 pom 中，`mongodb-driver-sync` 多为其传递依赖；
> 若 appender 自持 client，显式声明 `mongodb-driver-sync` 更稳妥（版本由 Spring Boot BOM 管理，不写死）。

---

## 3. 存储设计（MongoDB Collection）

### 3.1 集合：`central_logs.app_logs`

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `service` | String | 项目名（`spring.application.name`），多项目区分 | `order-service` |
| `env` | String | 环境（`spring.profiles.active`） | `prod` |
| `level` | String | 日志级别（仅 WARN/ERROR 入库） | `ERROR` |
| `traceId` | String | 链路 ID，串联整条请求 | `a1b2c3...` |
| `logger` | String | logger 名 | `c.e.t.controller.HelloController` |
| `thread` | String | 线程名 | `http-nio-8080-exec-1` |
| `message` | String | 日志正文（已换行清洗） | `系统异常` |
| `stackTrace` | String | 异常堆栈（无异常则空） | `java.lang.NullPointer...` |
| `host` | String | 实例主机名/IP | `node-1` |
| `timestamp` | Date | 事件时间（用于排序与 TTL） | `2026-06-30T12:26:00Z` |

### 3.2 索引

| 索引 | 用途 |
|------|------|
| `{ service: 1, timestamp: -1 }` | 按项目+时间倒序查询（最常用） |
| `{ traceId: 1 }` | 按链路精确定位 |
| `{ level: 1, timestamp: -1 }` | 按级别筛选 |
| `{ timestamp: 1 }` **TTL，expireAfterSeconds=2592000** | **30 天自动过期**，防存储爆炸 |

> 索引由 appender 启动时 `createIndex`（幂等）创建；TTL 字段必须是 `Date` 类型。

### 3.3 典型查询（Mongo shell 示例）

```js
// 某项目最近的错误
db.app_logs.find({ service: "order-service", level: "ERROR" }).sort({ timestamp: -1 }).limit(50)

// 按 traceId 串联整条链路（跨服务也能查）
db.app_logs.find({ traceId: "a1b2c3" }).sort({ timestamp: 1 })
```

---

## 4. 配置设计

### 4.1 `application.yml`（新增段，默认关闭）

```yaml
logging:
  mongo:
    enabled: ${LOG_MONGO_ENABLED:false}     # 默认关闭，配置/环境开启
    uri: ${LOG_MONGO_URI:}                   # 中央日志库连接串，开启时必填
    database: ${LOG_MONGO_DB:central_logs}
    collection: ${LOG_MONGO_COLLECTION:app_logs}
    ttl-days: ${LOG_MONGO_TTL_DAYS:30}
    queue-size: ${LOG_MONGO_QUEUE_SIZE:2048} # 异步队列容量，满则丢弃
```

> 敏感连接串放 `.env` 的 `LOG_MONGO_URI`，不入库、不提交（呼应现有规范）。

### 4.2 `logback-spring.xml`（新增 appender，骨架）

```xml
<springProperty name="MONGO_LOG_ENABLED"    source="logging.mongo.enabled"    defaultValue="false"/>
<springProperty name="MONGO_LOG_URI"        source="logging.mongo.uri"        defaultValue=""/>
<springProperty name="MONGO_LOG_DB"         source="logging.mongo.database"   defaultValue="central_logs"/>
<springProperty name="MONGO_LOG_COLLECTION" source="logging.mongo.collection" defaultValue="app_logs"/>
<springProperty name="MONGO_LOG_TTL_DAYS"   source="logging.mongo.ttl-days"   defaultValue="30"/>

<appender name="MONGO" class="com.example.template.common.logging.MongoLogAppender">
    <enabled>${MONGO_LOG_ENABLED}</enabled>
    <uri>${MONGO_LOG_URI}</uri>
    <database>${MONGO_LOG_DB}</database>
    <collection>${MONGO_LOG_COLLECTION}</collection>
    <ttlDays>${MONGO_LOG_TTL_DAYS}</ttlDays>
    <service>${APP_NAME}</service>
    <env>${APP_PROFILE}</env>
    <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
        <level>WARN</level>
    </filter>
</appender>

<appender name="MONGO_ASYNC" class="ch.qos.logback.classic.AsyncAppender">
    <neverBlock>true</neverBlock>      <!-- 队列满丢弃，绝不阻塞业务 -->
    <queueSize>2048</queueSize>
    <appender-ref ref="MONGO"/>
</appender>

<root level="INFO">
    <!-- 现有 CONSOLE / APP_FILE / ERROR_FILE 保留 -->
    <appender-ref ref="MONGO_ASYNC"/>  <!-- enabled=false 时 inert，不连库不写入 -->
</root>
```

---

## 5. 查询/可视化方案（不在应用内）

集中式日志的查询**不放在业务应用里**，推荐三选一：

1. **MongoDB Compass**：连中央库，按 `service/level/traceId/timestamp` 过滤，最省事。
2. **Grafana + MongoDB 数据源插件**：做看板、错误率告警，运维友好。
3. **独立"日志中心"服务**（后续可选）：单独一个 Spring 服务对 `app_logs` 暴露
   `GET /api/v1/logs?service=&level=&traceId=&from=&to=&page=&size=`，返回 `PageResult<LogItem>`。
   —— 仅当确有内部查询页需求时再做，避免每个业务应用重复造。

> 本期模板**只做写入**；查询 API 列为后续可选项。

---

## 6. 风险分析

| # | 风险 | 等级 | 缓解 |
|---|------|------|------|
| 1 | 写库慢/挂拖垮业务 | 高 | AsyncAppender `neverBlock=true` + 有界队列，满即丢；文件日志兜底 |
| 2 | 日志递归放大 | 高 | appender 忽略 `org.mongodb.*` logger |
| 3 | 存储爆炸 | 高 | TTL 索引 30 天自动过期；仅 WARN+ERROR 入库 |
| 4 | 集中后敏感信息集中泄漏 | 高 | 中央库独立鉴权/网络隔离；复用现有脱敏规范，禁止打印敏感字段 |
| 5 | 早期日志丢失（容器启动期 Spring 未就绪） | 中 | 可接受：此阶段日志仍写文件；appender 懒连接 |
| 6 | 多实例时间不一致 | 中 | 统一 UTC/带时区存储；主机名字段区分实例 |
| 7 | 连接串泄漏 | 中 | `LOG_MONGO_URI` 放 `.env`，不提交 |
| 8 | 写入放大影响中央库 | 中 | 仅 WARN+ERROR 量小；必要时后续加 `insertMany` 批量 |

---

## 7. 实施计划（确认后执行）

1. `pom.xml`：显式声明 `mongodb-driver-sync`（版本由 BOM 管理）。
2. 新增 `MongoLogAppender`（懒连接、建索引/TTL、防递归、`stop()` 释放）。
3. 改 `logback-spring.xml`：加 `MONGO` + `MONGO_ASYNC`，默认 inert。
4. 改 `application.yml`：加 `logging.mongo.*`（默认关闭）。
5. 更新 `.env.example`（如有）与 `server/README.md`：开启方式、查询方式、注意事项。
6. 验证：`mvn test`；本地起一个 Mongo，开 `enabled=true` 跑通写入与 TTL 索引。

---

## 8. 决策结论（已确认）

1. **单独一套中央 Mongo**：与业务库隔离，连接串经 `.env` 的 `LOG_MONGO_URI` 配置，禁止复用业务 MongoDB。
2. **非请求日志也入库**：定时任务等无 traceId 的日志同样写入，`traceId` 字段置 `-`。
3. **本期不做查询 API**：先用 Compass / Grafana 查询；待数据量增长后，单独建一个**后台管理项目**统一查询。

# 本地多项目完全隔离指南

> 从 `ai-project-template` 初始化新项目（如 `ander`、`like-weixin`）后，**不要直接复制 template 的 `.env` 就启动**。  
> 多个 Spring Boot 项目若共用同一 PostgreSQL 库、同一 Redis DB、同一 JWT Secret，会导致：
>
> - **Flyway 校验失败**（迁移脚本 checksum 与 `flyway_schema_history` 不一致）
> - **Token / 黑名单串项目**（Redis key 前缀相同）
> - **端口冲突**（两个服务都占 8080）
>
> 本文说明：**每个项目应改哪些 `.env` 项**，以及如何用 **Docker** 搭建/隔离本地中间件。

---

## 1. 隔离原则（必改 vs 建议改）

| 类别 | 配置项 | 是否必改 | 说明 |
|------|--------|----------|------|
| 应用 | `SERVER_PORT` | ✅ 必改 | 每项目不同端口，如 template=8080、ander=8081 |
| 应用 | `APP_NAME` | ✅ 必改 | 日志、Actuator、中央日志 `service` 字段区分 |
| PostgreSQL | `POSTGRES_DATABASE` | ✅ 必改 | **一项目一库**；共用库会 Flyway 冲突 |
| PostgreSQL | `POSTGRES_USERNAME/PASSWORD` | 建议 | 可共用实例，也可每库独立用户 |
| Redis | `REDIS_DATABASE` | ✅ 必改 | 0~15，每项目不同 index |
| Redis | `AUTH_TOKEN_REDIS_PREFIX` | 建议 | 默认 `auth:token`，多项目建议加项目前缀 |
| JWT | `JWT_SECRET` | ✅ 必改 | 每项目独立密钥，禁止用模板默认值 |
| JWT | `JWT_ISSUER` | 建议 | 与 `APP_NAME` 一致即可 |
| MongoDB 业务 | `MONGODB_DATABASE` | ✅ 必改 | 业务库名独立，如 `template_db`、`ander_db` |
| MongoDB 日志 | `LOG_MONGO_DB` | 建议 | 中央日志库可共用实例，库名按项目区分 |
| RabbitMQ | `RABBITMQ_VHOST` | 建议 | 完全隔离时为每项目建独立 vhost |
| 文件本地存储 | `FILE_LOCAL_DIR` | 建议 | 本地模式时目录按项目分开 |
| OSS | `OSS_BUCKET` 或路径前缀 | 建议 | 生产/联调时避免多项目写同一 bucket 根目录 |

**最小可行隔离（本地开发）：** 改 `SERVER_PORT`、`APP_NAME`、`POSTGRES_DATABASE`、`REDIS_DATABASE`、`JWT_SECRET`、`MONGODB_DATABASE`。

---

## 2. 推荐配置示例

### 2.1 template（模板本体）

```env
SPRING_PROFILES_ACTIVE=local
SERVER_PORT=8080
APP_NAME=template-server

POSTGRES_HOST=127.0.0.1
POSTGRES_PORT=5432
POSTGRES_DATABASE=template_dev
POSTGRES_USERNAME=devuser
POSTGRES_PASSWORD="Dev@123456"

MONGODB_HOST=127.0.0.1
MONGODB_PORT=27017
MONGODB_DATABASE=template_mongo
MONGODB_USERNAME=admin
MONGODB_PASSWORD="your-mongo-password"
MONGODB_AUTH_DB=admin

REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0

JWT_SECRET="template-请换成32字符以上随机串"
JWT_ISSUER=template-server
AUTH_TOKEN_REDIS_PREFIX=auth:token:template

RABBITMQ_HOST=127.0.0.1
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VHOST=/template

LOG_MONGO_ENABLED=false
# 若开启：
# LOG_MONGO_DB=central_logs_template
```

### 2.2 ander（从模板 init 的新项目）

```env
SPRING_PROFILES_ACTIVE=local
SERVER_PORT=8081
APP_NAME=ander-server

POSTGRES_HOST=127.0.0.1
POSTGRES_PORT=5432
POSTGRES_DATABASE=ander_dev
POSTGRES_USERNAME=devuser
POSTGRES_PASSWORD="Dev@123456"

MONGODB_HOST=127.0.0.1
MONGODB_PORT=27017
MONGODB_DATABASE=ander_mongo
MONGODB_USERNAME=admin
MONGODB_PASSWORD="your-mongo-password"
MONGODB_AUTH_DB=admin

REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=1

JWT_SECRET="ander-请换成与template不同的随机串"
JWT_ISSUER=ander-server
AUTH_TOKEN_REDIS_PREFIX=auth:token:ander

RABBITMQ_HOST=127.0.0.1
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VHOST=/ander

LOG_MONGO_ENABLED=true
LOG_MONGO_URI=mongodb://admin:your-mongo-password@127.0.0.1:27017/?authSource=admin
LOG_MONGO_DB=central_logs_ander
LOG_MONGO_COLLECTION=app_logs
```

### 2.3 第三个项目（示意）

| 项目 | PORT | POSTGRES_DATABASE | REDIS_DATABASE | MONGODB_DATABASE |
|------|------|-------------------|----------------|------------------|
| template | 8080 | `template_dev` | 0 | `template_mongo` |
| ander | 8081 | `ander_dev` | 1 | `ander_mongo` |
| like-weixin | 8082 | `like_weixin_dev` | 2 | `like_weixin_mongo` |

---

## 3. Docker：方案 A（推荐）共享中间件 + 配置隔离

一台机器跑 **一套** PostgreSQL / Redis / MongoDB / RabbitMQ，通过 **不同库名 / DB index / vhost** 隔离各项目。资源占用小，适合日常开发。

### 3.1 一键启动中间件（docker-compose）

在任意目录（如 `~/dev-infra`）创建 `docker-compose.yml`：

```yaml
services:
  postgres:
    image: postgres:16
    container_name: dev-postgres
    restart: unless-stopped
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: devuser
      POSTGRES_PASSWORD: Dev@123456
    volumes:
      - dev_pg_data:/var/lib/postgresql/data

  redis:
    image: redis:7
    container_name: dev-redis
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - dev_redis_data:/data

  mongodb:
    image: mongo:7
    container_name: dev-mongodb
    restart: unless-stopped
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: C2k9hxZw1yeFC2w
    volumes:
      - dev_mongo_data:/data/db

  rabbitmq:
    image: rabbitmq:3-management
    container_name: dev-rabbitmq
    restart: unless-stopped
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    volumes:
      - dev_rabbit_data:/var/lib/rabbitmq

volumes:
  dev_pg_data:
  dev_redis_data:
  dev_mongo_data:
  dev_rabbit_data:
```

启动 / 停止：

```bash
cd ~/dev-infra
docker compose up -d
docker compose ps
docker compose logs -f postgres   # 按需查看日志
docker compose down               # 停止（数据在 volume 里保留）
docker compose down -v            # 停止并清空数据（慎用）
```

### 3.2 为每个项目创建 PostgreSQL 库

```bash
# 进入 postgres 容器
docker exec -it dev-postgres psql -U devuser -d postgres

# 在 psql 里执行（每个项目一条 CREATE DATABASE）
CREATE DATABASE template_dev OWNER devuser;
CREATE DATABASE ander_dev OWNER devuser;
CREATE DATABASE like_weixin_dev OWNER devuser;

\q
```

或一条命令：

```bash
docker exec dev-postgres psql -U devuser -d postgres -c "CREATE DATABASE ander_dev OWNER devuser;"
docker exec dev-postgres psql -U devuser -d postgres -c "CREATE DATABASE template_dev OWNER devuser;"
```

### 3.3 为每个项目创建 MongoDB 业务库（可选）

Mongo 会在首次写入时自动建库；也可手动创建：

```bash
docker exec -it dev-mongodb mongosh -u admin -p 'C2k9hxZw1yeFC2w' --authenticationDatabase admin

use template_mongo
use ander_mongo
use central_logs_template
use central_logs_ander
exit
```

### 3.4 为每个项目创建 RabbitMQ vhost（可选）

管理台：`http://localhost:15672`（guest/guest）

```bash
docker exec dev-rabbitmq rabbitmqctl add_vhost /template
docker exec dev-rabbitmq rabbitmqctl add_vhost /ander
docker exec dev-rabbitmq rabbitmqctl set_permissions -p /template guest ".*" ".*" ".*"
docker exec dev-rabbitmq rabbitmqctl set_permissions -p /ander guest ".*" ".*" ".*"
```

`.env` 中对应：

```env
RABBITMQ_VHOST=/template   # 或 /ander
```

---

## 4. Docker：方案 B 完全独立中间件（每项目一套端口）

若希望 **网络与数据完全隔离**（例如同时跑两套不同版本 PostgreSQL），可为每个项目单独起 compose，**映射不同宿主机端口**。

### 4.1 ander 专用栈示例

在 `ander/deploy/docker-compose.local.yml`（路径自定）：

```yaml
services:
  postgres:
    image: postgres:16
    container_name: ander-postgres
    ports:
      - "5433:5432"
    environment:
      POSTGRES_USER: ander
      POSTGRES_PASSWORD: Ander@123456
      POSTGRES_DB: ander_dev

  redis:
    image: redis:7
    container_name: ander-redis
    ports:
      - "6380:6379"

  mongodb:
    image: mongo:7
    container_name: ander-mongodb
    ports:
      - "27018:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: ander
      MONGO_INITDB_ROOT_PASSWORD: AnderMongo@123

  rabbitmq:
    image: rabbitmq:3-management
    container_name: ander-rabbitmq
    ports:
      - "5673:5672"
      - "15673:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
```

启动：

```bash
docker compose -f deploy/docker-compose.local.yml up -d
```

对应 `ander/server/.env`：

```env
SERVER_PORT=8081
POSTGRES_HOST=127.0.0.1
POSTGRES_PORT=5433
POSTGRES_DATABASE=ander_dev
POSTGRES_USERNAME=ander
POSTGRES_PASSWORD="Ander@123456"

REDIS_HOST=127.0.0.1
REDIS_PORT=6380
REDIS_DATABASE=0

MONGODB_HOST=127.0.0.1
MONGODB_PORT=27018
MONGODB_USERNAME=ander
MONGODB_PASSWORD="AnderMongo@123"
MONGODB_DATABASE=ander_mongo
```

template 仍用方案 A 的 `5432/6379/27017`，两者 **零冲突**。

---

## 5. 新项目初始化 Checklist

从模板 `new-project.sh` 生成新项目后，在 `server/.env` 中逐项确认：

```text
□ SERVER_PORT          → 与已有项目不重复
□ APP_NAME             → {项目名}-server
□ POSTGRES_DATABASE    → 新库名（先在 Postgres 里 CREATE DATABASE）
□ REDIS_DATABASE       → 未占用的 0~15
□ JWT_SECRET           → 新随机串（≥32 字符）
□ MONGODB_DATABASE     → 新库名
□ AUTH_TOKEN_REDIS_PREFIX → 建议带项目名
□ RABBITMQ_VHOST       → 可选独立 vhost
□ LOG_MONGO_DB         → 若开启中央日志，按项目区分
□ FILE_LOCAL_DIR       → 若 FILE_STORAGE_TYPE=local，独立目录
```

生成 JWT 随机串（示例）：

```bash
openssl rand -base64 48
```

---

## 6. 启动方式（`.env` 生效前提）

`spring-dotenv` 默认从 **进程工作目录** 读取 `.env`，请务必在 `server/` 下启动：

```bash
cd /path/to/your-project/server
mvn spring-boot:run
```

或在 IDE 中将 **Working Directory** 设为 `{project}/server`。

验证：

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8081/api/v1/hello
```

---

## 7. 常见问题

### Q1：Flyway checksum mismatch

**原因：** 两个项目用了同一个 `POSTGRES_DATABASE`，且迁移脚本版本不一致。

**解决：** 为新项目使用 **空的新库**（见 §3.2），不要共用 `devdb`。

### Q2：改了 `.env` 仍连旧库

**原因：** 未在 `server/` 目录启动，`.env` 未加载。

**解决：** `cd server && mvn spring-boot:run`。

### Q3：端口被占用

```bash
lsof -i :8081
kill <PID>
```

或改 `SERVER_PORT`。

### Q4：想清空某项目数据重来

```bash
# 仅删 ander 的库（PostgreSQL）
docker exec dev-postgres psql -U devuser -d postgres -c "DROP DATABASE IF EXISTS ander_dev;"
docker exec dev-postgres psql -U devuser -d postgres -c "CREATE DATABASE ander_dev OWNER devuser;"
```

重启应用后 Flyway 会重新迁移。

---

## 8. 快速对照：template vs ander

| 配置 | template | ander |
|------|----------|-------|
| `SERVER_PORT` | 8080 | 8081 |
| `APP_NAME` | template-server | ander-server |
| `POSTGRES_DATABASE` | template_dev | ander_dev |
| `REDIS_DATABASE` | 0 | 1 |
| `MONGODB_DATABASE` | template_mongo | ander_mongo |
| `JWT_SECRET` | （独立随机串 A） | （独立随机串 B） |
| `RABBITMQ_VHOST` | /template | /ander |
| `LOG_MONGO_DB` | central_logs_template | central_logs_ander |

---

## 9. 相关文档

- [DB/MIGRATIONS.md](../DB/MIGRATIONS.md) — Flyway 与 baseline
- [server/README.md](../../server/README.md) — 后端启动说明
- [server/.env.example](../../server/.env.example) — 环境变量模板

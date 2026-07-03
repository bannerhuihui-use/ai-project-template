# 新项目本地环境隔离（精简）

从模板初始化新项目后，在 `server/.env` 里改几项即可。**不要和 template 共用同一个 PostgreSQL 库名**，否则会 Flyway 冲突。

---

## 1. 新项目要改什么

| 中间件 | 要不要手动建 | 新项目做什么 |
|--------|--------------|--------------|
| **PostgreSQL** | 要建 **库**（用户可共用） | `.env` 改 `POSTGRES_DATABASE`；**表由 Flyway 启动时自动建** |
| **MongoDB** | **不用建** | `.env` 改 `MONGODB_DATABASE`；**库在首次写入时自动创建** |
| **Redis** | **不用建** | `.env` 改 `REDIS_DATABASE`（0、1、2… 每个项目用一个） |

另外建议改：`SERVER_PORT`（避免端口冲突）、`APP_NAME`、`JWT_SECRET`（与 template 不同）。

---

## 2. 命名约定

项目名 `my-app` → 连字符换成下划线，库名 **`app_db_<项目名>`**：

| 配置项 | 示例（项目 `my-app`） |
|--------|----------------------|
| `POSTGRES_DATABASE` | `app_db_my_app` |
| `MYSQL_DATABASE` | `app_db_my_app` |
| `MONGODB_DATABASE` | `app_db_my_app` |
| `REDIS_DATABASE` | `1`（template 用 `0`，第二个项目用 `1`，以此类推） |

### 2.1 一键建三库（推荐）

在 **local-db-stack** 或 **server-db-stack** 目录下（五库容器已运行）：

```bash
./scripts/create-project-db.sh my-app
```

会自动创建 `app_db_my_app`（PostgreSQL / MySQL / MongoDB），共用 `api_rw` 用户。  
脚本结束会打印要在 `server/.env` 里填的配置项。

---

## 3. PostgreSQL（Docker）

### 3.1 查容器名

```bash
docker ps | grep -i postgres
```

下面把 `dev-postgres` 换成你的容器名。

### 3.2 方式 A：共用已有用户（推荐，最简单）

本地已有 `devuser` 时，**只需新建一个空库**：

```bash
docker exec dev-postgres psql -U devuser -d postgres -c \
  "CREATE DATABASE my_app_dev OWNER devuser;"
```

`.env`：

```env
POSTGRES_DATABASE=my_app_dev
POSTGRES_USERNAME=devuser
POSTGRES_PASSWORD="与现有 devuser 密码一致"
```

启动后 Flyway 自动执行 `V1~V8` 建表，**无需手跑 SQL**。

### 3.3 方式 B：每项目独立用户（可选）

需要完全隔离登录账号时使用（用超级用户进容器，常见为 `postgres` 或 `devuser`）：

```bash
# 1. 建用户
docker exec dev-postgres psql -U postgres -d postgres -c \
  "CREATE USER my_app WITH PASSWORD 'MyApp@123456';"

# 2. 建库并指定 owner
docker exec dev-postgres psql -U postgres -d postgres -c \
  "CREATE DATABASE my_app_dev OWNER my_app;"

# 3. 赋权（owner 已具备库内全部权限；若需显式授权可执行）
docker exec dev-postgres psql -U postgres -d postgres -c \
  "GRANT ALL PRIVILEGES ON DATABASE my_app_dev TO my_app;"
```

`.env`：

```env
POSTGRES_DATABASE=my_app_dev
POSTGRES_USERNAME=my_app
POSTGRES_PASSWORD="MyApp@123456"
```

### 3.4 重建空库（会删光该库数据）

```bash
docker exec dev-postgres psql -U devuser -d postgres -c "DROP DATABASE IF EXISTS my_app_dev;"
docker exec dev-postgres psql -U devuser -d postgres -c "CREATE DATABASE my_app_dev OWNER devuser;"
```

---

## 4. MongoDB

**不用手动建库。** 在 `.env` 里改库名即可：

```env
MONGODB_DATABASE=my_app_mongo
```

应用第一次连 Mongo 写入数据时，库会自动出现。

---

## 5. Redis

**不用建库。** 每个项目换一个 **DB 编号**（0~15）：

```env
REDIS_DATABASE=1
```

| 项目 | `REDIS_DATABASE` |
|------|------------------|
| template | 0 |
| 第二个项目 | 1 |
| 第三个项目 | 2 |

---

## 6. 新项目 `.env` 最小清单

```env
SERVER_PORT=8081
APP_NAME=my-app-server

POSTGRES_DATABASE=my_app_dev
POSTGRES_USERNAME=devuser
POSTGRES_PASSWORD="你的密码"

MONGODB_DATABASE=my_app_mongo

REDIS_DATABASE=1

JWT_SECRET="用 openssl rand -base64 48 生成，勿与 template 相同"
```

---

## 7. 启动

```bash
cd server
mvn spring-boot:run
```

必须在 **`server/` 目录**启动，`.env` 才会被正确加载。

---

## 8. 示例：ander

```bash
docker exec dev-postgres psql -U devuser -d postgres -c "CREATE DATABASE ander_dev OWNER devuser;"
```

```env
SERVER_PORT=8081
APP_NAME=ander-server
POSTGRES_DATABASE=ander_dev
MONGODB_DATABASE=ander_mongo
REDIS_DATABASE=1
```

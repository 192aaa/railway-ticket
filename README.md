# 铁路购票系统（Railway Ticket）

一个前后端分离的铁路购票演示项目：Spring Boot 后端提供车票查询、购票退票、订单、消息、数据看板等接口，Vue 3 前端提供用户界面，数据存储在 MySQL，首次启动会自动初始化站点、车次、余票等基础数据。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 4.1.1、Spring Data JPA、Spring Cache + Caffeine、Validation、Actuator + Prometheus、springdoc-openapi、JJWT、BCrypt、Lombok |
| 前端 | Vue 3、Vue Router、Pinia、Element Plus、Axios、Vite 8 |
| 数据库 | MySQL 8.0（本地 dev）/ 环境变量注入的 MySQL（prod）；测试用 H2 内存库 |
| 部署 | Docker Compose、Nginx、Render（`render.yaml`） |

## 主要功能

- 用户注册/登录（JWT + BCrypt）、个人信息与密码修改
- 车站搜索、车次经停站查询、按出发/到达站与日期查询余票
- 购票、我的订单、退票；定时任务每分钟将已发车订单自动置为完成并发送系统消息
- 用户间私信、会话列表、未读数、系统通知；页面内置 AI 助手（兼容 OpenAI/DeepSeek 格式，未配置密钥时为模拟模式，配置仅保存在浏览器 `localStorage`）
- 数据看板：概览、热门线路、趋势统计
- 接口文档（dev 开启 Swagger）、健康检查与监控端点

## 目录结构

```
.
├── src/main/java/com/example/demo/
│   ├── controller/        # REST 接口
│   ├── service/           # 业务逻辑
│   ├── repository/        # JPA 数据访问
│   ├── entity/ dto/       # 实体与请求/响应对象
│   ├── security/          # JWT 过滤器与工具类
│   ├── config/            # CORS、密码编码器、数据初始化
│   ├── task/              # 订单自动完成定时任务
│   └── common/            # 统一响应与全局异常处理
├── src/main/resources/    # application*.properties（dev / prod）
├── src/test/              # 单元测试与 H2 测试配置
├── frontend/              # Vue 3 前端（.env.* 环境变量）
├── docker/                # 后端/前端 Dockerfile + 容器内 Nginx 配置
├── deploy/nginx/          # 裸机部署用 Nginx 配置
├── deploy/scripts/        # 打包与进程管理脚本
├── .github/workflows/     # CI 流水线
├── docker-compose.yml     # MySQL + 后端 + 前端
├── render.yaml            # Render 部署配置
├── .env.example           # Compose 环境变量模板
└── .dockerignore          # 容器构建上下文排除规则
```

> 注意：`docker/`、`deploy/`、`.mvn/wrapper/`、`.vscode/`、`README.md` 都是构建与运行必需文件，均已纳入版本控制，请勿再从 `.gitignore` 中排除。

## 环境要求

- JDK 17（项目自带 Maven Wrapper，无需单独安装 Maven）
- MySQL 8.0，本地端口 `3306`
- Node.js 20.19+ 或 22.12+（Vite 8 要求），npm
- Docker / Docker Compose（容器部署时）

## 本地运行

### 1. 准备数据库

创建 `application-dev.properties` 中约定的库与账号：

```sql
CREATE DATABASE IF NOT EXISTS railway_ticket DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'railway'@'%' IDENTIFIED BY 'railway123';
GRANT ALL PRIVILEGES ON railway_ticket.* TO 'railway'@'%';
FLUSH PRIVILEGES;
```

也可以只启动 Compose 中的 MySQL 服务（用户名/密码与上面一致）。注意 Compose 把容器内的 `3306` 映射到宿主机 **3307**，避免与本机 MySQL 服务冲突；从宿主机连接时用 `localhost:3307`：

```powershell
docker compose up -d mysql
```

### 2. 启动后端

```powershell
# Windows PowerShell
.\mvnw.cmd spring-boot:run
```

```bash
# macOS / Linux
./mvnw spring-boot:run
```

默认使用 `dev` profile，监听 `http://localhost:8080`。首次启动由 `DataInitializer` 自动建表并初始化站点、车次、未来 7 天余票；dev 环境还会生成测试账号与模拟历史订单。

### 3. 启动前端

```powershell
cd frontend
npm ci
npm run dev
```

Vite 开发服务器为 `http://localhost:5173`，`/api` 请求默认代理到 `http://localhost:8080`。

### 4. 访问与登录

| 地址 | 说明 |
| --- | --- |
| `http://localhost:5173` | 前端页面 |
| `http://localhost:8080` | 后端服务 |
| `http://localhost:8080/swagger-ui.html` | 接口文档（仅 dev，prod 已关闭） |
| `http://localhost:8080/actuator/health` | 健康检查 |

演示账号：`admin`、`zhangsan`、`lisi`、`wangwu`，密码统一为 `123456`。**仅在 dev 环境生成**，prod 已通过 `app.demo-data.enabled=false` 关闭。

## 在 CodeBuddy / VS Code 中运行

项目提供 `.vscode/`（`launch.json`、`tasks.json`、`settings.json`、`extensions.json`）：

- 按 `extensions.json` 的推荐安装扩展：Java 扩展包、Spring Boot Dashboard / Spring Boot Tools、Vue - Official。
- **Java 语言服务需要 JDK 25+**（`redhat.java` 1.58 的 universal 版不带内嵌 JRE），而项目仍按 JDK 17 编译。`settings.json` 中 `java.jdt.ls.java.home` 指向语言服务用的 JDK 25，`java.configuration.runtimes` 指向项目 JDK 17；换机器时按实际安装路径修改。
- `F5` 选择「后端: Spring Boot (dev)」「前端: Vite dev server」或「全栈: 后端 + 前端」。
- 数据库仍需按上文准备；工作区需处于「信任」状态，否则 `java.jdt.ls.java.home` 等受限设置不生效。

## 配置项

后端 `application.properties` 默认 `SPRING_PROFILES_ACTIVE=dev`；`prod` 下数据源必须由环境变量提供：

| 变量 / 配置项 | 用途 | 默认值 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | 启用 `dev` / `prod` | `dev` |
| `SPRING_DATASOURCE_URL` / `USERNAME` / `PASSWORD` | prod 数据库连接 | 无（prod 必填） |
| `JWT_SECRET` | JWT 签名密钥 | 内置开发密钥，生产必须覆盖 |
| `JWT_EXPIRATION` | Token 有效期（毫秒） | `86400000` |
| `app.demo-data.enabled` | 生成弱口令测试账号与模拟订单 | dev `true` / prod `false` |
| `app.data-init.enabled` | 是否执行 `DataInitializer` | `true`（测试环境 `false`） |
| `DB_ROOT_PWD` / `DB_USER_PWD` | Compose 中 MySQL 密码 | `root123` / `railway123` |
| `APP_CORS_ALLOWED_ORIGINS` | 后端跨域来源白名单（逗号分隔） | `*`（同源反代） |
| `VITE_API_TARGET` | 开发环境代理目标 | `http://localhost:8080` |
| `VITE_API_BASE_URL` | 生产环境 axios 基础路径 | `/api`（同源反代） |

Compose 使用根目录 `.env`，模板见 `.env.example`。

## 测试与 CI

```powershell
# 后端：20 个测试，使用 H2 内存库，无需本机 MySQL
.\mvnw.cmd -B verify

# 前端
cd frontend
npm ci
npm run build
```

- 后端覆盖上下文加载、注册/登录/改密、JWT 生成与失效、购票原子扣减、退票回补与越权校验、异常分类与 CORS 白名单解析。
- `.github/workflows/ci.yml` 在 push / PR 时执行后端 `verify`、前端 `npm ci + build`，以及 `docker compose config` 校验。

## 构建与部署

### 容器部署（推荐）

```powershell
Copy-Item .env.example .env   # 按需修改 DB_* 与 JWT_SECRET
docker compose up -d --build
```

- `docker/Dockerfile.backend` 为**多阶段构建**（镜像内 Maven 打包），无需预先生成 `target/*.jar`；运行阶段由 Spring Boot 直接读取 `SPRING_DATASOURCE_*`、`JWT_SECRET`、`SPRING_PROFILES_ACTIVE`、`APP_CORS_ALLOWED_ORIGINS` 环境变量。
- `docker/Dockerfile.frontend` 使用 `npm ci` 构建，产物放入 `/usr/share/nginx/html`。
- **两套 Nginx 配置不要混用**：`docker/nginx.conf` 用于容器（root `/usr/share/nginx/html`，`/api` 反代到 Compose 服务 `backend`）；`deploy/nginx/railway.conf` 用于裸机（root `/var/www/railway-frontend`，`/api` 反代到 `127.0.0.1:8080`）。
- Compose 为 MySQL 与后端配置了 healthcheck，按依赖顺序启动。
- 启动后访问 `http://localhost/`（前端）、`http://localhost:8080/actuator/health`（后端）；容器 MySQL 映射到宿主机 `3307`。
- `frontend/.env.production` 默认 `/api`，即**同源部署**：浏览器请求前端域名的 `/api`，由 Nginx 反代到后端，不产生跨域，`APP_CORS_ALLOWED_ORIGINS` 保持 `*` 即可。

### 前后端分域名部署

前端与后端不在同一域名时，需要两处配置，均无需改代码：

1. **前端构建时**指定后端绝对地址：
   ```powershell
   $env:VITE_API_BASE_URL = "https://your-backend.example.com/api"
   cd frontend
   npm run build
   ```
   Docker 镜像构建可传 `--build-arg VITE_API_BASE_URL=https://your-backend.example.com/api`。
2. **后端运行时**在 `.env` 中设置来源白名单：
   ```
   APP_CORS_ALLOWED_ORIGINS=https://your-frontend.example.com
   ```
   多个来源用逗号分隔，支持通配符（如 `https://*.example.com`）；由 `app.cors.allowed-origins` 读取，未配置时回退 `*`。同源部署不要设置具体域名，避免把 Nginx 反代请求挡掉。

部署后用浏览器预检（OPTIONS）确认响应头 `Access-Control-Allow-Origin` 为配置的前端域名。

### Render

`render.yaml` 通过 `dockerfilePath: ./docker/Dockerfile.backend` 构建后端，需在平台配置三个 `SPRING_DATASOURCE_*` 变量，`JWT_SECRET` 自动生成，另建议设置 `APP_CORS_ALLOWED_ORIGINS`。前端是静态站点，需另行部署并按上一条配置 `VITE_API_BASE_URL`。

### 裸机部署

`deploy/scripts/build-all.bat`（Windows）/ `build-all.sh`（Linux）完成后端打包与前端构建，`deploy/scripts/app.sh` 管理后端进程；`app.sh` 启动 prod 时会校验数据源环境变量。

## 接口概览

统一响应格式为 `Result`（`code` / `message` / `data`）；需要登录的接口通过 `Authorization: Bearer <token>` 传递 JWT。

| 前缀 | 说明 | 鉴权 |
| --- | --- | --- |
| `/api/user/register`、`/api/user/login` | 注册、登录 | 公开 |
| `/api/user/info`、`/profile`、`/change-password` | 当前用户信息与维护 | 需要登录 |
| `/api/ticket/query`、`/api/station/*`、`/api/train/{id}/stops` | 余票、车站、经停站查询 | 公开 |
| `/api/order/buy`、`/my`、`/refund/{id}` | 购票、订单、退票 | 需要登录 |
| `/api/message/*` | 私信与系统通知 | 需要登录 |
| `/api/dashboard/*` | 看板统计 | 公开 |

## 注意事项

- `DataInitializer` 在 prod 仍会幂等补齐站点/车次/余票，但不创建演示账号与模拟订单；生产账号请自行创建。
- 内置 JWT 密钥与数据库密码仅供本地演示，公网部署前必须替换；CORS 默认 `*` 仅适用于同源部署，分域名部署必须通过 `APP_CORS_ALLOWED_ORIGINS` 收紧白名单。
- AI 助手的 `endpoint` / `model` / `apiKey` 在页面设置中填写，密钥保存在浏览器本地并由前端直连模型服务，请自行评估是否适合公网暴露。
- 当前仍使用 `ddl-auto=update` 自动建表，尚未提供 Flyway/Liquibase 迁移脚本，生产建议补齐 schema 版本管理。

## 尚未包含

- `LICENSE`（授权协议需项目所有者确定）
- 数据库迁移脚本
- 前端 E2E 测试、ESLint / Prettier 等代码规范配置

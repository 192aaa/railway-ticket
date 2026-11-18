# 铁路购票系统（Railway Ticket）

一个前后端分离的铁路购票演示项目：Spring Boot 后端提供车票查询、购票退票、订单、消息、数据看板等接口，Vue 3 前端提供用户界面，数据存储在 MySQL（prod schema 由 Flyway 管理），缓存默认使用本地 Caffeine、生产环境切换为 Redis，首次启动会自动初始化站点、车次、余票等基础数据。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 4.1.1、Spring Data JPA、Spring Cache + Caffeine / Redis（Spring Data Redis + Lettuce）、Flyway、Validation、Actuator + Prometheus、springdoc-openapi、JJWT、BCrypt、Micrometer Tracing + Brave（Span 上报 Zipkin）、MDC traceId、Lombok |
| 前端 | Vue 3、Vue Router、Pinia、Element Plus、Axios、Vite 8、ESLint 9、Prettier |
| 数据库 | MySQL 8.0（本地 dev）/ 环境变量注入的 MySQL（prod，Flyway 迁移）；测试用 H2 内存库 |
| 缓存 | dev / 测试用 Caffeine 或 `none`；prod 默认 Redis 7（JSON 序列化，余票 30s、站点/车次 600s TTL） |
| 部署 | Docker Compose（MySQL + Redis + 后端 + 前端，可选 Zipkin profile）、Nginx、Render（`render.yaml`） |
| 测试 | 后端 JUnit 5、Mockito、AssertJ、`@SpringBootTest`、Testcontainers（真实 Redis）；前端 Vitest 单测 + Playwright E2E（mock API） |

## 主要功能

- 用户注册/登录（JWT + BCrypt）、access/refresh token 自动续期、服务端登出黑名单、个人信息与密码修改
- 车站搜索、车次经停站查询、按出发/到达站与日期查询余票；余票缓存版本化失效，站点/车次查询走 Redis 缓存
- 购票、我的订单、退票；定时任务每分钟将已发车订单自动置为完成并发送系统消息（Redis 分布式锁保证多实例只跑一次）
- 鉴权默认拦截 + 显式放行；登录/注册/余票查询按 IP 限流（Redis 固定窗口，超限返回 429）
- 用户间私信、会话列表、未读数、系统通知；页面内置 AI 助手（兼容 OpenAI/DeepSeek 格式，密钥仅存于服务端环境变量、由后端代理调用，未配置时降级为模拟模式）
- 数据看板：概览、热门线路、趋势统计
- 接口文档（dev 开启 Swagger）、健康检查、监控端点与 Redis 缓存命中率指标
- 全请求 traceId（生成/透传/响应头回写）与结构化日志；Micrometer + Brave 生成 SERVER span，可选上报 Zipkin（日志 traceId 与 Zipkin 一致）；prod 按天滚动归档、gzip 压缩、保留 30 天

## 目录结构

```
.
├── src/main/java/com/example/demo/
│   ├── controller/        # REST 接口
│   ├── service/           # 业务逻辑、缓存版本、站点/车次查询
│   ├── repository/        # JPA 数据访问
│   ├── entity/ dto/       # 实体与请求/响应对象
│   ├── security/          # JWT、默认拦截过滤器、令牌服务、限流过滤器
│   ├── config/            # CORS、数据初始化、Redis、traceId/Brave/Zipkin、prod 密钥校验
│   ├── task/              # 订单自动完成定时任务 + Redis 分布式锁
│   └── common/            # 统一响应与全局异常处理
├── src/main/resources/    # application*.properties、logback-spring.xml、db/migration（Flyway）
├── src/test/              # 单元测试、H2 配置、Testcontainers Redis 集成测试
├── frontend/              # Vue 3 前端（.env.* 环境变量）
│   ├── src/**/*.test.js   # Vitest 单测（auth、request）
│   └── tests/e2e/         # Playwright E2E（登录/登出/401 刷新）
├── docker/                # 后端/前端 Dockerfile + 容器内 Nginx 配置
├── deploy/nginx/          # 裸机部署用 Nginx 配置
├── deploy/scripts/        # 打包与进程管理脚本
├── .github/workflows/     # CI 流水线
├── docker-compose.yml     # MySQL + Redis + 后端 + 前端
├── render.yaml            # Render 部署配置
├── .env.example           # Compose 环境变量模板
└── .dockerignore          # 容器构建上下文排除规则
```

> 注意：`docker/`、`deploy/`、`.mvn/wrapper/`、`.vscode/`、`README.md` 都是构建与运行必需文件，均已纳入版本控制，请勿再从 `.gitignore` 中排除。

## 环境要求

- JDK 17（项目自带 Maven Wrapper，无需单独安装 Maven）
- MySQL 8.0，本地端口 `3306`
- Redis 7：仅 prod / 主动启用 Redis 缓存时需要；Compose 部署会自动启动，本地 dev 默认用 Caffeine，无需安装
- Node.js 20.19+ 或 22.12+（Vite 8 要求），npm
- Docker / Docker Compose（容器部署；Testcontainers 真实 Redis 集成测试也需要，无 Docker 时自动跳过）

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

### 可选：本地启用 Redis 缓存

dev 默认用 Caffeine，无需安装 Redis。需要调试 Redis 缓存时，先启动 Compose 中的 Redis（默认映射宿主机 `6379`，可用 `REDIS_HOST_PORT` 改端口），再带环境变量启动后端：

```powershell
docker compose up -d redis
docker compose ps redis                    # STATUS 应为 healthy
docker compose exec redis redis-cli ping   # 应返回 PONG（密码由容器 REDISCLI_AUTH 提供）
$env:SPRING_CACHE_TYPE = "redis"
$env:SPRING_DATA_REDIS_PASSWORD = "railway_redis_123"   # 与 REDIS_PWD 一致
.\mvnw.cmd spring-boot:run
```

缓存值的 JSON 序列化与 TTL 由 `RedisConfig` 配置（`tickets` 30 秒，`stations` / `trains` 600 秒）；业务代码也可直接注入 `RedisTemplate<String, Object>` 读写 Redis。CI 另有 Testcontainers 集成测试覆盖真实 Redis 的读写、缓存失效、任务锁、令牌与限流。

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
| `SPRING_DATASOURCE_URL` / `USERNAME` / `PASSWORD` | prod 数据库连接（启动期校验） | 无（prod 必填） |
| `JWT_SECRET` | JWT 签名密钥 | dev 内置开发密钥；prod 必须由环境变量显式提供（≥32 字符） |
| `JWT_EXPIRATION` | access token 有效期（毫秒） | `86400000` |
| `JWT_REFRESH_EXPIRATION` | refresh token 有效期（毫秒，Redis 模式） | `604800000`（7 天） |
| `app.rate-limit.login-max` / `register-max` / `query-max` | 每 IP 每窗口允许的最大请求数 | `10` / `10` / `60` |
| `app.rate-limit.window-seconds` | 限流窗口长度（秒） | `60` |
| `APP_REQUIRE_STRONG_SECRETS` | prod 启动是否强制校验密钥与口令 | `true` |
| `spring.flyway.enabled` / `baseline-on-migrate` | prod 由 Flyway 建表、兼容存量库 | prod `true` / dev·test `false` |
| `app.demo-data.enabled` | 生成弱口令测试账号与模拟订单 | dev `true` / prod `false` |
| `app.data-init.enabled` | 是否执行 `DataInitializer` | `true`（测试环境 `false`） |
| `DB_ROOT_PWD` / `DB_USER_PWD` | Compose 中 MySQL 密码 | `root123` / `railway123` |
| `SPRING_CACHE_TYPE` | 缓存实现 `redis` / `caffeine` / `none` | dev `caffeine` / prod `redis` |
| `SPRING_DATA_REDIS_HOST` / `PORT` / `PASSWORD` / `DATABASE` | Redis 连接参数 | `localhost` / `6379` / 空 / `0` |
| `app.cache.redis.*-ttl-seconds` | Redis 各缓存 TTL（秒） | `tickets` `30`、`stations` / `trains` `600` |
| `REDIS_PWD` / `REDIS_HOST_PORT` | Compose 中 Redis 密码与宿主机映射端口（仅绑定 `127.0.0.1`） | `railway_redis_123` / `6379` |
| `REDIS_HEALTH_ENABLED` | 是否把 Redis 纳入 `/actuator/health` | 默认 `false`，Compose `true` |
| `APP_CORS_ALLOWED_ORIGINS` | 后端跨域来源白名单（逗号分隔） | `*`（同源反代） |
| `VITE_API_TARGET` | 开发环境代理目标 | `http://localhost:8080` |
| `VITE_API_BASE_URL` | 生产环境 axios 基础路径 | `/api`（同源反代） |
| `AI_API_KEY` / `AI_ENDPOINT` / `AI_MODEL` / `AI_TEMPERATURE` | AI 助手服务端模型配置（密钥仅服务端持有） | 空（未配置则模拟模式）/ DeepSeek 默认 |
| `LOG_DIR` / `LOG_RETAIN_DAYS` / `LOG_MAX_SIZE` / `LOG_TOTAL_SIZE` | prod 日志归档目录 / 保留天数 / 单文件大小 / 总大小上限 | `logs` / `30` / `100MB` / `3GB` |
| `TRACING_ENABLED` / `TRACING_SAMPLING_PROBABILITY` | 启用链路追踪 / 采样率 | `true` / `1.0` |
| `ZIPKIN_ENDPOINT` | Zipkin span 上报地址；空则只记日志 traceId 不上报 | 空 |
| `ZIPKIN_CONNECT_TIMEOUT` / `ZIPKIN_READ_TIMEOUT` | Zipkin 上报连接/读取超时 | `1s` / `1s` |

Compose 使用根目录 `.env`，模板见 `.env.example`。

## 测试与 CI

```powershell
# 后端：57 个测试（其中 6 个 Testcontainers 真实 Redis 用例需 Docker，就绪时全部执行，无 Docker 时自动跳过）；
# 其余测试用 H2 内存库，无需本机 MySQL / Redis
.\mvnw.cmd -B verify

# 前端（本地 E2E 用系统 Edge，CI 用 Playwright 自带 chromium）
cd frontend
npm ci
npm run lint          # ESLint 代码规范检查
npm run format:check  # Prettier 格式检查
npm run test          # Vitest 单测（auth 存储 + request 401 刷新/并发队列，11 例）
npm run build
npm run test:e2e      # Playwright E2E（登录/登出、401 自动刷新重放，2 例；mock API 不依赖后端）
```

- 后端覆盖上下文加载、注册/登录/改密、JWT 生成与失效、购票原子扣减、座位唯一约束冲突重试、退票回补与座位释放、越权校验、异常分类、CORS 白名单解析、默认拦截与黑名单、限流降级、缓存版本化失效、分布式锁、prod 密钥校验、traceId 生成/透传/清洗与 MDC 清理、Brave Tracer 与 span 生成。
- `RedisCacheIntegrationTest` 在 CI 用 Testcontainers 起真实 Redis（带密码），验证缓存序列化 / TTL / 版本化失效、任务锁互斥、refresh token 轮换与限流 429。
- 前端 E2E 全部用 `page.route` mock 后端响应，断言令牌存储、401→refresh→重放、登出清理；不依赖真实后端，CI 通过 `npx playwright install chromium` 后直接运行。
- `.github/workflows/ci.yml` 在 push / PR 时先用 `docker info` 确认 Docker 可用，再执行后端 `verify`、前端 `npm ci + lint + format:check + test + build + test:e2e` 与 `docker compose config` 校验。

## 构建与部署

### 容器部署（推荐）

```powershell
Copy-Item .env.example .env   # 按需修改 DB_*、REDIS_PWD 与 JWT_SECRET
docker compose up -d --build
```

- `docker/Dockerfile.backend` 默认直接复制宿主机 `.\mvnw.cmd -B -DskipTests package` 生成的 `target/demo2-0.0.1-SNAPSHOT.jar`（本机 Docker Desktop 代理会导致镜像内 Maven 下载出现 `bad_record_mac`）；若构建环境可稳定访问 Maven 仓库，可改用 `docker/Dockerfile.backend.maven` 在镜像内打包（Render 即使用该版本）。运行阶段由 Spring Boot 直接读取 `SPRING_DATASOURCE_*`、`SPRING_DATA_REDIS_*`、`JWT_SECRET`、`SPRING_PROFILES_ACTIVE`、`APP_CORS_ALLOWED_ORIGINS` 环境变量。
- `docker/Dockerfile.frontend` 使用 `npm ci` 构建，产物放入 `/usr/share/nginx/html`。
- **两套 Nginx 配置不要混用**：`docker/nginx.conf` 用于容器（root `/usr/share/nginx/html`，`/api` 反代到 Compose 服务 `backend`）；`deploy/nginx/railway.conf` 用于裸机（root `/var/www/railway-frontend`，`/api` 反代到 `127.0.0.1:8080`）。
- Compose 为 MySQL、Redis 与后端配置了 healthcheck，backend 等 MySQL / Redis 健康后再启动；Redis 开启 `appendonly` 持久化，数据卷为 `redis-data`，缓存默认走 Redis（设 `SPRING_CACHE_TYPE=caffeine` 可回退本地缓存）。
- prod 由 Flyway 执行 `db/migration/V1__init.sql` 建表、`V2__orders_seat_unique.sql` 建座位唯一约束、`V3__user_role.sql` 增加用户角色（存量库自动 baseline），Hibernate 改为 `ddl-auto=none`；启动期还会校验 `JWT_SECRET`、`SPRING_DATASOURCE_PASSWORD`（Redis 模式下另有 Redis 口令）确由环境变量提供，否则拒绝启动。
- 启动后访问 `http://localhost/`（前端）、`http://localhost:8080/actuator/health`（后端）；容器 MySQL 映射到宿主机 `3307`，Redis 只绑定宿主机 `127.0.0.1:6379`（可用 `REDIS_HOST_PORT` 改），避免暴露公网。
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

`render.yaml` 通过 `dockerfilePath: ./docker/Dockerfile.backend.maven` 构建后端（平台构建上下文不含本地 jar，使用镜像内 Maven 打包版本），需在平台配置三个 `SPRING_DATASOURCE_*` 变量，`JWT_SECRET` 自动生成，另建议设置 `APP_CORS_ALLOWED_ORIGINS`。前端是静态站点，需另行部署并按上一条配置 `VITE_API_BASE_URL`。

免费方案没有托管 Redis，`render.yaml` 已将 `SPRING_CACHE_TYPE` 设为 `caffeine`，并默认关闭 Redis 健康检查；此时 refresh token / 黑名单 / 限流等 Redis 能力不启用，但 `JWT_SECRET`（平台 `generateValue`）与数据源口令满足 prod 启动校验。接入外部 Redis 时把该值改为 `redis`，在平台补充 `SPRING_DATA_REDIS_HOST` / `PORT` / `PASSWORD`（口令为 prod 校验必填），需要监控时再设 `REDIS_HEALTH_ENABLED=true`。prod 首次启动由 Flyway 建表。

### 裸机部署

`deploy/scripts/build-all.bat`（Windows）/ `build-all.sh`（Linux）完成后端打包与前端构建，`deploy/scripts/app.sh` 管理后端进程。`app.sh` 启动 prod 时会校验数据源、`JWT_SECRET`，以及 Redis 模式下的 `SPRING_DATA_REDIS_HOST` / `PASSWORD`；无 Redis 的机器需显式设置 `SPRING_CACHE_TYPE=caffeine`。prod schema 由 Flyway 管理，请勿依赖 `ddl-auto=update`。

## 接口概览

统一响应格式为 `Result`（`code` / `message` / `data`）。除下表标注「公开」的 `/api` 路径外，其余 `/api` 路径**默认要求** `Authorization: Bearer <token>`（Actuator、Swagger 等非 `/api` 路径不受拦截）；`/api/admin/**` 还要求令牌中的角色为 `ADMIN`，否则返回 403。因此不存在的 `/api/**` 在未登录时先返回 401，带有效令牌才由异常处理器返回 404 `{"code":404,...}`，非 `/api` 未知路径直接 404。登录 / 注册 / 余票查询超限返回 429。

| 前缀 | 说明 | 鉴权 |
| --- | --- | --- |
| `/api/user/register`、`/api/user/login`、`/api/user/refresh` | 注册、登录、刷新令牌 | 公开 |
| `/api/user/logout` | 登出（access token 加入黑名单、撤销 refresh token） | 需要登录 |
| `/api/user/info`、`/profile`、`/change-password` | 当前用户信息与维护 | 需要登录 |
| `/api/ticket/query`、`/api/station/*`、`/api/train/{id}/stops` | 余票、车站、经停站查询 | 公开 |
| `/api/order/buy`、`/my`、`/refund/{id}` | 购票、订单、退票 | 需要登录 |
| `/api/message/*` | 私信与系统通知 | 需要登录 |
| `/api/dashboard/*` | 看板统计 | 公开 |
| `/api/ai/status`、`/api/ai/chat` | AI 助手配置状态与对话代理 | 需要登录 |
| `/api/admin/users`、`/api/admin/orders`、`/api/admin/stations`、`/api/admin/trains`、`/api/admin/tickets` | 管理后台（角色/订单查询、站点车次 CRUD、余票调整） | 需要 ADMIN |

## 注意事项

- `DataInitializer` 在 prod 仍会幂等补齐站点/车次/余票，但不创建演示账号与模拟订单；生产账号请自行创建。
- 内置 JWT 密钥、数据库与 Redis 密码仅供本地演示，公网部署前必须替换 `JWT_SECRET`、`DB_*`、`REDIS_PWD`；prod 启动会拒绝缺失这些环境变量的部署。CORS 默认 `*` 仅适用于同源部署，分域名部署必须通过 `APP_CORS_ALLOWED_ORIGINS` 收紧白名单。
- AI 助手的模型密钥由服务端通过 `AI_API_KEY` 环境变量持有，浏览器只与 `/api/ai/**` 代理交互，不接触密钥；未配置时自动降级为模拟回复。可用 `AI_ENDPOINT` / `AI_MODEL` / `AI_TEMPERATURE` 调整服务端模型参数。
- prod schema 由 Flyway 管理（Hibernate `ddl-auto=none`）；后续表结构变更请新增 `V4__*.sql` 等迁移脚本，不要直接改库或改回 `update`。dev / test 仍由 Hibernate 自动建表。
- 角色写在 JWT 声明中：注册用户默认 `USER`，内置 `admin` 账号（仅 dev 演示）为 `ADMIN`；prod 需用户自注册后由管理员调用 `/api/admin/users/{id}/role` 提升。角色变更后旧令牌保持原角色直到刷新/过期。前端管理后台入口仅 `ADMIN` 可见。
- Redis 共享缓存、版本化失效、令牌黑名单、refresh token、限流与任务锁仅在 `SPRING_CACHE_TYPE=redis` 时启用；无 Redis 或 Redis 故障时降级为本地缓存 / 直接放行，不会导致启动失败。
- 每个 HTTP 请求都会生成或透传 `X-Trace-Id`（写入 MDC 与响应头，非法字符清洗），日志格式含 `[app,traceId,thread]` 可按请求检索关联；prod 日志落到 `logs/` 并按天+大小滚动归档（可用 `LOG_DIR` / `LOG_RETAIN_DAYS` 等环境变量调整）。
- 链路追踪默认开启（写日志 traceId + 内存 span），不配置 `ZIPKIN_ENDPOINT` 时不产生外部网络调用。需要可视化时，在 `.env` 设置 `ZIPKIN_ENDPOINT=http://zipkin:9411/api/v2/spans` 后执行 `docker compose --profile tracing up -d`，访问 `http://localhost:9411` 查看 span（日志 traceId 与 Zipkin traceId 一致）。Spring Boot 4 无 tracing 自动配置，本项目用 `TracingConfig` 手动装配 Brave Tracer 与 reporter。

## 可选增强（非阻塞）

- 前端测试已固化：`npm run test`（Vitest 11 例）+ `npm run test:e2e`（Playwright 2 例，mock API），并已接入 CI。
- 链路追踪已接入 Zipkin（可选 profile）；如需 Jaeger/OTLP，可再加对应 reporter 替换 `ZIPKIN_ENDPOINT`。
- `LICENSE`、ESLint / Prettier 均已完成（MIT；lint / format 已接入 CI）。

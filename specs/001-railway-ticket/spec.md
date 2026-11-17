# Feature Specification: 铁路购票系统（Railway Ticket）

**Feature Branch**: `001-railway-ticket`
**Created**: 2026-11-14
**Status**: Implemented & Verified（既有系统补规格，并回填已完成的工程化改造）
**Input**: 用户描述「Specify → Plan → Task 写出应该写的文件，补充上之前完成的」
**Source**: 由原 `docs/SDD.md`（软件设计文档 v1.0）整合改写，该旧文件已在本次整合后删除；详细设计见 `./plan.md`，进度见 `./tasks.md`

> 本文档只回答 **WHAT / WHY**（需求、验收、边界），技术方案与决策记录在 `plan.md`。
> 状态说明：本特性已实现并实测通过，因此所有功能需求标注「已实现」，验收记录为真实执行结果。

## 1. 背景与目标

铁路购票演示系统提供车票查询、购票退票、订单管理、私信与运营看板。系统已实现且可运行，但此前缺少工程化外壳（部署文件未入库、无测试、无 CI）与规格基线。

本次规格化的目标：

1. 把**已实现的系统**固化为需求基线，使后续改动有可验收的依据；
2. 把这几轮已完成的修复、测试与实测结果**回填**为已交付项（见 `tasks.md`）；
3. 把审计中发现的**遗留风险**转成可执行待办，并按宪法原则登记优先级。

## 2. 用户角色

| 角色 | 描述 | 主要诉求 |
| --- | --- | --- |
| 访客 | 未登录的浏览器用户 | 浏览首页、查询余票与站点、查看运营看板 |
| 注册用户 | 持有有效 JWT 的用户 | 购票、退票、查看订单、管理资料、收发消息 |
| 系统 | 后台定时任务与业务事件 | 自动完成已发车订单、发送系统通知 |
| 运维 / 开发 | 部署与维护者 | 一键启动、健康检查、可重复的测试与文档 |

## 3. 用户故事

### US1: 查询余票与车次 (Priority: P1)

作为访客，我希望按出发站、到达站和日期查询车次与四类席别余票，以便决定购买哪趟车。

**Why this priority**: 购票链路的入口，无此能力其它故事无法独立验收。
**Independent Test**: 不登录调用 `/api/ticket/query`、`/api/station/list` 能返回结果。

**Acceptance Scenarios**:

1. **Given** 系统中已有站点与车次数据，**When** 输入合法出发站/到达站/日期，**Then** 返回匹配车次及各席别余票与票价。
2. **Given** 某车次某日期无余票记录，**When** 查询该日期，**Then** 四类余票显示 0 且不报错。
3. **Given** 输入不存在的站名，**When** 查询，**Then** 返回明确的「未找到出发站/到达站」错误。

### US2: 注册与登录 (Priority: P1)

作为访客，我希望注册账号并登录获得令牌，以便使用需要身份的功能。

**Independent Test**: 注册后可用同一账号登录，返回 token 与用户信息。

**Acceptance Scenarios**:

1. **Given** 用户名未被占用，**When** 提交合法注册信息，**Then** 账号创建成功且密码以不可逆形式存储。
2. **Given** 用户名已存在，**When** 再次注册，**Then** 返回「用户名已存在」且不写入数据。
3. **Given** 账号密码正确，**When** 登录，**Then** 返回 JWT；**Given** 密码错误，**Then** 返回「用户名或密码错误」且不签发令牌。

### US3: 购票（并发安全） (Priority: P1)

作为注册用户，我希望选定车次、席别与日期下单，以便获得订单与座位号。

**Why this priority**: 核心价值路径，且是最容易出现数据不一致的环节。
**Independent Test**: 构造余票为 0 的车次，下单必须失败且库存/订单/消息均无副作用。

**Acceptance Scenarios**:

1. **Given** 目标席别有余票，**When** 提交有效购票请求，**Then** 余票减 1、生成 `PAID` 订单（含座位号与票价）、并收到系统通知。
2. **Given** 目标席别余票为 0，**When** 并发多个购票请求，**Then** 仅成功与余票数相同的请求，其余返回「余票不足」，**且不产生超卖**。
3. **Given** 座位类型非法或车次不存在，**When** 下单，**Then** 返回明确错误且不扣减余票。

### US4: 我的订单与退票 (Priority: P1)

作为注册用户，我希望查看自己的订单并在发车前退票，以便资金与余票正确回补。

**Independent Test**: 对他人订单发起退票必须被拒绝，且余票不回补。

**Acceptance Scenarios**:

1. **Given** 用户有订单，**When** 请求 `/api/order/my`，**Then** 按创建时间倒序返回本人订单。
2. **Given** 订单属于本人且状态为 `PAID`/`COMPLETED`、未发车，**When** 退票，**Then** 状态变 `REFUNDED`、对应席别余票 +1、收到系统通知。
3. **Given** 订单属于他人 / 已退票 / 列车已发车，**When** 退票，**Then** 返回对应错误且余票不变。

### US5: 发车后订单自动完成 (Priority: P2)

作为系统，我希望定时把已发车订单置为完成，以便统计与通知保持准确。

**Independent Test**: 构造一条发车时刻已过 10 分钟以上的 `PAID` 订单，等待一个调度周期后状态变 `COMPLETED`。

**Acceptance Scenarios**:

1. **Given** 存在已发车超过 10 分钟的 `PAID` 订单，**When** 定时任务执行，**Then** 状态变 `COMPLETED` 并发送「乘车完成」通知。
2. **Given** 单条订单处理异常，**When** 定时任务执行，**Then** 不影响其它订单处理。

### US6: 私信与系统通知 (Priority: P2)

作为注册用户，我希望与其他用户聊天并接收系统通知，以便获知购票/退票/乘车完成事件。

**Independent Test**: 用户 A 向 B 发消息后，B 的未读数与会话列表出现该会话。

**Acceptance Scenarios**:

1. **Given** 收件人存在且内容合法，**When** 发送消息，**Then** 消息落库且收件人未读数增加。
2. **Given** 内容为空 / 超过 500 字 / 发给自己，**When** 发送，**Then** 返回对应错误且不落库。
3. **Given** 用户有未读消息，**When** 标记会话或全部已读，**Then** 未读数正确归零。
4. **Given** 查看用户搜索结果，**Then** 手机号以「前 3 + `****` + 后 4」脱敏展示。

### US7: 运营数据看板 (Priority: P3)

作为访客，我希望看到订单概览、热门线路与近 7 日趋势，以便了解系统运营情况。

**Independent Test**: 三个看板接口无需登录即可返回结构化数据。

**Acceptance Scenarios**:

1. **Given** 系统中有订单，**When** 请求概览，**Then** 返回总订单数、今日订单/营收、车次数、用户数与状态分布。
2. **Given** 任意数据状态，**When** 请求趋势，**Then** 返回近 7 日按日的订单数与营收（无数据日为 0）。

### US8: 一键部署与可验证交付 (Priority: P1)

作为运维/开发，我希望从仓库克隆后能构建、启动并验证系统，以便交付与维护。

**Why this priority**: 本轮审计发现部署必需文件未入库、无测试、无 CI，直接阻断交付。
**Independent Test**: 执行 `Copy-Item .env.example .env; docker compose up -d --build` 后三个容器启动且健康检查通过。

**Acceptance Scenarios**:

1. **Given** 全新克隆的仓库（无 `target/*.jar`），**When** 执行 Compose 构建，**Then** 后端镜像能自行完成编译打包，前端镜像能自行安装依赖并构建。
2. **Given** 容器启动，**When** 访问 `http://localhost/` 与公开 `/api/*`，**Then** 静态资源与接口均可达；未登录访问未知 `/api/**` 返回 401（默认拦截），带令牌访问同一路径返回 404，非 `/api` 未知路径返回 404。
3. **Given** prod 环境启动，**Then** 不写入弱口令测试账号与模拟订单。
4. **Given** 无本机 MySQL，**When** 运行测试，**Then** 使用 H2 内存库且全部通过。

## 4. 功能需求

> 状态说明：全部「已实现」；编号供 `plan.md` 与 `tasks.md` 引用。

### 用户与鉴权

- **FR-001**: 系统必须提供注册，校验用户名唯一，密码以 BCrypt 存储。✅
- **FR-002**: 系统必须提供登录，签发含 `userId` 与 `username` 的 JWT。✅
- **FR-003**: 系统必须提供当前用户信息、修改密码（校验原密码、新密码 ≥6 位、不得与原密码相同）。✅
- **FR-004**: 系统必须支持修改资料，校验身份证（15/18 位）与手机号（`^1[3-9]\d{9}$`）。✅
- **FR-005**: 需要登录的接口必须校验 `Authorization: Bearer <token>`，失败返回 401。✅
- **FR-006**: `/api/user/login`、`/api/user/register`、`/api/ticket/`、`/api/station/` 等前缀必须公开可访问。✅

### 票务查询

- **FR-010**: 系统必须提供站点列表与按关键字搜索。✅
- **FR-011**: 系统必须提供车次经停站（含到/发时间、站序）。✅
- **FR-012**: 系统必须按出发站、到达站、日期查询余票与票价；站名支持模糊匹配，缺失余票记录按 0 处理。✅
- **FR-013**: 余票查询结果必须缓存，且购票/退票后缓存失效。✅

### 订单

- **FR-020**: 购票必须原子扣减对应席别余票，失败不得产生订单或消息。✅
- **FR-021**: 购票必须生成唯一订单号、`PAID` 状态、按席别规则分配座位号、写入创建/支付时间。✅
- **FR-022**: 购票与退票必须发送系统通知。✅
- **FR-023**: 系统必须提供「我的订单」列表（按时间倒序）。✅
- **FR-024**: 退票必须校验归属、状态与发车时间，成功后回补余票并置 `REFUNDED`。✅
- **FR-025**: 定时任务必须每 60 秒把发车超过 10 分钟的 `PAID` 订单置 `COMPLETED`。✅

### 消息与看板

- **FR-030**: 系统必须支持搜索用户（手机号脱敏）、发送消息、会话列表、聊天记录、系统通知列表。✅
- **FR-031**: 系统必须支持按会话标记已读、全部已读、未读总数。✅
- **FR-032**: 系统必须提供概览、热门线路 Top10、近 7 日趋势三个看板接口。✅

### 基础设施

- **FR-040**: 所有接口必须返回统一 `Result<T>{code,message,data}`。✅
- **FR-041**: 参数校验失败返回 400，业务异常返回对应 code；鉴权过滤器放行后仍无匹配路由时返回 **HTTP 404 + code 404**（未登录的 `/api/**` 先由过滤器返回 401），未预期异常返回 500「系统繁忙」。✅
- **FR-042**: 系统必须提供 dev 环境的 Swagger 文档与 Actuator 健康检查/监控端点。✅
- **FR-043**: 系统必须在首次启动幂等初始化站点、车次、经停站与未来 7 天余票。✅
- **FR-044**: 演示账号与模拟订单必须只在 dev 生成，prod 与 test 必须关闭。✅
- **FR-045**: 前端必须提供首页、登录、注册、订单、资料、消息、看板、404 共 8 个视图，并对订单/资料/消息页做登录守卫。✅
- **FR-046**: 前端必须统一注入 Bearer、自动解包 `data`、处理 401 强制登出。✅
- **FR-047**: 前端必须提供 AI 助手浮窗；未配置密钥时以模拟模式工作。✅
- **FR-048**: 系统必须提供 Docker Compose 一键部署、健康检查与 CI 流水线；跨域来源必须可通过配置收紧，以支撑前后端分域名部署。✅

## 5. 非功能需求

- **NFR-001（并发）**: 并发购票不得超卖；判定依据是数据库条件更新的受影响行数。✅
- **NFR-002（隔离）**: prod 的 `DataInitializer` 禁止写入弱口令账号与模拟订单（首次启动实测 `用户数: 0`、`历史订单: 0`）；持久化卷中后续的手工验证数据不属于初始化器写入。✅
- **NFR-003（安全）**: 密钥/口令由环境变量注入，仓库只保留 `.env.example`；dev/Compose 保留演示默认值以便一键启动，prod 由 `ProdSecurityValidator` 强制校验，缺失即拒绝启动（T104）。✅
- **NFR-004（可测性）**: 测试不得依赖本机 MySQL，使用 H2 内存库。✅
- **NFR-005（可部署）**: 默认镜像使用宿主机打包的 jar（另一份 `Dockerfile.backend.maven` 在镜像内分层构建）；容器按依赖顺序启动并具备健康检查。✅
- **NFR-006（正确性）**: 路由不存在必须返回 404 而非 500；鉴权默认拦截下，未登录的未知 `/api/**` 先返回 401，带令牌或非 `/api` 路径返回 404。✅（2026-11-14 修复，2026-11-17 复验边界）
- **NFR-007（一致性）**: 全系统时区 `Asia/Shanghai`、Jackson 时间格式统一。✅
- **NFR-008（性能）**: 余票缓存 30s、站点/车次 600s，Redis 版本化失效；定时任务批量查询避免 N+1、前端代码分块。✅
- **NFR-009（可观测）**: Actuator 暴露 `health,info,metrics,prometheus,caches`；MDC traceId + 分级日志 + prod 滚动归档，Micrometer/Brave 支持可选 Zipkin 导出。✅
- **NFR-010（文档）**: 文档中端点数、测试数、端口等数字必须能与代码反向核对。✅
- **NFR-011（权限）**: `/api` 默认拦截，仅显式放行查询类接口；refresh token 轮换、登出黑名单与按 IP 限流由 Redis 支撑（T101/T103）。✅
- **NFR-012（CORS）**: 同源部署保持 `*`；分域名部署必须能通过 `APP_CORS_ALLOWED_ORIGINS` 收紧来源白名单。✅ 已配置化（2026-11-14），具体域名由部署时提供。

## 6. 成功标准

| ID | 标准 | 结果 |
| --- | --- | --- |
| SC-001 | `.\mvnw.cmd -B verify` 全部测试通过 | ✅ 49 tests / 0 failures；Docker 就绪时 0 skipped（6 个 Testcontainers 真实执行），无 Docker 时自动跳过 |
| SC-002 | `npm ci && npm run build` 成功 | ✅ 11 个 Vitest 单测、2 个 Playwright E2E，lint/format/build 均通过 |
| SC-003 | `docker compose config --quiet` 通过 | ✅ |
| SC-004 | MySQL / Redis / 后端 / 前端四服务启动并具备健康检查 | ✅（2026-11-17 复验，含 `--profile tracing` 的 zipkin 共 5 容器 healthy） |
| SC-005 | 首页、静态资源、公开 `/api/*` 可达；未知 `/api` 路径未登录 401、带令牌 404 | ✅ |
| SC-006 | prod 不写入演示账号与模拟订单 | ✅ |
| SC-007 | 文档数字与代码一致（27 端点 / 7 实体 / 8 条原子 SQL / 49 测试） | ✅ |
| SC-008 | CORS 白名单可配置，默认同源部署行为不变 | ✅ `WebConfigTest` 2 个测试 |
| SC-009 | 日志可按 traceId 检索，可选上报 Zipkin | ✅ `TraceIdFilterTest` 4 例、`TracingConfigTest` 2 例 |

## 7. 范围边界

| 在范围内 | 不在范围内 |
| --- | --- |
| 用户、站点、车次、余票、订单、消息、看板 | 真实支付、发票、实名制核验接口 |
| JWT 鉴权、refresh token 轮换、登出黑名单、限流、参数校验、统一响应与异常 | 完整 Spring Security 权限模型、RBAC |
| Docker Compose 部署（含 Redis、可选 Zipkin）、Render 后端部署、CI | 生产级高可用、多副本、分库分表 |
| 演示数据初始化、Flyway 迁移、单元/集成测试 | 真实外部日志平台与独立采集链路 |
| 容器与裸机两套 Nginx 配置 | 自动化备份 |
| AI 助手服务端代理（密钥不下发浏览器，未配置时模拟回复） | 自训练/自建大模型服务 |

## 8. 假设与依赖

- JDK 17；Node.js 20.19+ / 22.12+（Vite 8 要求）；Docker / Docker Compose。
- MySQL 8.0：dev 使用本机 `3306`；Compose 将容器 `3306` 映射到宿主机 `3307` 以避开本机服务冲突。
- prod 数据源由 `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` 注入，缺失即启动失败。
- 浏览器需支持 `sessionStorage`（会话）；AI 助手配置与模型密钥由服务端持有，不依赖浏览器本地存储。
- Redis 7 仅 prod / 主动启用 Redis 缓存时需要；不部署 Redis 时可用 `SPRING_CACHE_TYPE=caffeine` 回退（refresh token、黑名单与限流随之不启用）。
- Zipkin 为可选外部 collector：未配置 `ZIPKIN_ENDPOINT` 时只写日志 traceId 与内存 span，不产生外部网络调用。
- 镜像构建需要能访问基础镜像仓库；本机已配置 registry mirror（属环境配置，不写入仓库）。
- 分域名部署时，前端来源与后端地址由部署方通过 `APP_CORS_ALLOWED_ORIGINS`、`VITE_API_BASE_URL` 提供；**不要求现在确定域名**，也不写入仓库（见 ADR-011）。

## 9. 待澄清问题 / 决策记录

- **[已决策: prod 账号体系]** 保持开放自注册；prod 不预置管理员账号。需管理员时由部署方在注册后手动提升或从数据库指派。
- **[已决策: 鉴权边界]** 采用「默认拦截 + 显式放行」（T103）。`/api/ticket/**`、`/api/station/**`、`/api/train/**`、`/api/dashboard/**` 为有意公开的查询类接口；`/api/order/**`、`/api/user/**`（除 login/register/refresh）、`/api/message/**`、`/api/ai/**` 均需登录。
- **[已决策: 授权协议]** 采用 MIT License（教学/展示项目通用、宽松），已创建根目录 `LICENSE` 并补齐 `pom.xml` 元数据（T107）。
- **[已决策: 演示默认值]** Compose 保留 `JWT_SECRET`/DB 口令的内置默认值以支持「一键启动」；但 prod 启动时由 `ProdSecurityValidator` 强制校验环境变量，未显式提供则拒绝启动（T104）。两种场景互不冲突。

> 原第 3 项「分域名部署」已于 2026-11-14 通过配置化解决（T057 / ADR-011）。第 9 章待澄清问题已全部决策完毕（T113）。

## 10. 验收记录（2026-11-13 ~ 17 实测）

| 检查项 | 结果 | 证据 |
| --- | --- | --- |
| 后端测试 | PASS | `.\mvnw.cmd -B verify` → `BUILD SUCCESS`，49 tests / 0 failures；Docker 就绪时 0 skipped（2026-11-17 复验），无 Docker 时 6 个 Testcontainers 用例自动跳过 |
| 前端单测 / 规范 / 格式 / 构建 | PASS | `npm test` 11 例、`npm run lint` 0 errors 0 warnings、`npm run format:check` 通过、`npm run build` 1.46s、`npm audit` 0 vulnerabilities |
| 前端 E2E | PASS | `npm run test:e2e` 2 例（登录存储/登出清理、401→refresh→重放），`page.route` mock API |
| Compose 配置 | PASS | `docker compose config --quiet`（2026-11-17 复验仍通过） |
| 容器启动与健康 | PASS | 2026-11-17 复验 `docker compose up -d --build`：mysql / redis / backend / frontend / zipkin 均 `healthy` |
| 前端首页 / 入口 JS | PASS | HTTP 200，`application/javascript` |
| 后端健康检查（直连与反代） | PASS | HTTP 200 `{"status":"UP"}` |
| `/api/station/list`、`/api/station/search?keyword=...`、`/api/dashboard/overview` | PASS | `code:200` |
| 未知路径（直连与经 Nginx） | PASS | 未登录的 `/api/**` → HTTP 401；带有效令牌 → HTTP 404 `{"code":404,"message":"请求的资源不存在"}`；非 `/api` 路径 → 404 |
| 前端产物 | PASS | 无旧外部域名，走同源 `/api`；入口 JS HTTP 200 |
| prod 数据隔离与 Flyway | PASS | 初始化器不写入演示账号/订单（首次启动日志 `用户数: 0`、`历史订单: 0`）；当前持久化卷中 2 用户 / 7 订单来自后续手工验证，非初始化器写入；车次 179；`flyway_schema_history` 含 v1/v2 成功记录 |
| CORS 白名单配置化 | PASS | `WebConfigTest` 2 个测试；默认 `*` 不改变同源部署行为 |
| traceId / Zipkin | PASS | `TraceIdFilterTest` 4 例、`TracingConfigTest` 2 例；实测 `/api/v2/services` 返回 `railway-ticket` 且 span 与日志 traceId 一致 |

## 11. 已完成改造回填

以下几轮已完成的工程化改造在 `./tasks.md` 中作为已完成项（`- [x]`）逐条登记，并附验收证据：版本控制修复、容器部署链路、配置一致性、演示数据隔离、49 个后端测试与 CI、前端 Vitest/Playwright/ESLint/Prettier、文档、Docker 实测、404 处理器、分域名 CORS 配置化、Spec Kit 三件套、Redis 缓存与令牌/限流/任务锁、Flyway 迁移、默认拦截鉴权、prod 密钥校验、座位唯一约束、AI 服务端代理、日志追踪与 Zipkin。原遗留风险 `T1xx` 已全部完成并与 `.specify/memory/constitution.md` 的原则逐条对应。

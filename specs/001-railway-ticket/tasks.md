# Tasks: 铁路购票系统（Railway Ticket）

**Input**: `./spec.md`、`./plan.md`
**Prerequisites**: `../../.specify/memory/constitution.md`
**Organization**: 按 Phase 分组；Phase 1–6 为**已完成回填**（按时间顺序），Phase 8 为遗留风险待办

## 格式约定

- 状态：`[x]` 已完成（附验收与证据）；`[ ]` 待办
- 标记：`[P]` 可并行、`[US#]` 对应 `spec.md` 用户故事、`[高/中/低]` 优先级、`[宪法 N]` 对应宪法原则
- 每项的「验收」是可执行判定条件，「证据」是命令输出 / 日志 / 文件路径
- 已完成项保留，形成完整进度记录，不得删除

---

## Phase 1: 版本控制与工程化外壳（已完成）

- [x] T001 [P] 只读审计项目缺失项，形成按优先级排序的清单
  > 验收: 区分「已确认存在 / 缺失 / 不一致」，含路径与行号证据
  > 证据: 首轮盘点结论——45 个 Java 类、14 个 Vue 文件功能闭环，但部署文件被 `.gitignore` 忽略、仅 1 个空测试

- [x] T002 [P] 重写 `.gitignore`，放行 `docker/`、`deploy/`、`.mvn/`、`*.jar`、`.gitignore` 自身
  > 验收: `git status` 中这些路径显示为可跟踪
  > 证据: `git status --short` 出现 `?? docker/`、`?? deploy/`、`?? .mvn/`、`?? .gitignore`

- [x] T003 [P] 新增 `.dockerignore`，排除 `node_modules`/`target`/`.git`/日志/`.env`
  > 验收: 容器构建上下文不含前端依赖与构建产物
  > 证据: `d:/demo2/.dockerignore`，后续两个镜像构建成功

- [x] T004 [P] 新增 `.env.example`（`DB_ROOT_PWD` / `DB_USER_PWD` / `JWT_SECRET`）
  > 验收: 模板入库、真实 `.env` 被忽略
  > 证据: `git status` 显示 `?? .env.example`；`.gitignore` 含 `.env` 规则

- [x] T005 [P] 重写 `README.md`，同步目录结构、配置项、测试、部署与注意事项
  > 验收: 文档中不再有与代码矛盾的 `.gitignore`/Nginx 路径/生产域名说明
  > 证据: `README.md` 已改；实测后追加 3307 端口与访问地址

- [x] T006 [P] 把 `HELP.md` 从 Initializr 模板改为项目专属常用命令
  > 验收: 指向 README 且列出后端/前端/容器命令
  > 证据: `HELP.md`

## Phase 2: 容器部署链路（已完成）

- [x] T010 `docker/Dockerfile.backend` 改为多阶段构建，修复 exec-form `${...}` 不展开
  > 验收: 无需宿主机 `target/*.jar` 即可构建；prod 能连上数据库
  > 证据: `Image demo2-backend Built`；backend `healthy`；日志 `Started Demo2Application`

- [x] T011 `docker/Dockerfile.frontend` 改用 `npm ci`，产物路径与 Nginx root 对齐，支持 `VITE_API_BASE_URL` 构建参数
  > 验收: 镜像内完成前端构建，静态资源可访问
  > 证据: `Image demo2-frontend Built`；入口 JS HTTP 200

- [x] T012 新增 `docker/nginx.conf`：root `/usr/share/nginx/html`、Docker DNS resolver、`$request_uri`
  > 验收: `/api/station/list` 完整路径透传，未知路径 404
  > 证据: 实测 `/api/station/list` → `code:200`

- [x] T013 `render.yaml` 补 `dockerfilePath` / `dockerContext`
  > 验收: Render Blueprint 能定位到后端 Dockerfile
  > 证据: `render.yaml:7-8`

- [x] T014 `docker-compose.yml` 增加 mysql/backend healthcheck 与 `depends_on: service_healthy`
  > 验收: 按依赖顺序启动，无「数据库未就绪」竞态
  > 证据: `Container railway-mysql Healthy` → `railway-backend Healthy`

- [x] T015 把 mysql 宿主机端口由 3306 改为 3307（容器内仍 3306）
  > 验收: 避开本机 `MySQL80` 服务占用，容器可启动
  > 证据: `docker compose ps` → `0.0.0.0:3307->3306/tcp`

- [x] T016 修 `deploy/scripts/build-all.sh`：`mvnw.cmd` → `mvnw`、移除不存在的 `-Pprod`、`npm install` → `npm ci`
  > 验收: Linux 下可执行且不产生 Maven 警告
  > 证据: `deploy/scripts/build-all.sh`

- [x] T017 `deploy/scripts/app.sh` 启动前校验 `SPRING_DATASOURCE_*`
  > 验收: 缺少变量时直接失败，而非「启动成功但连不上库」
  > 证据: `deploy/scripts/app.sh` start 分支

- [x] T018 `frontend/.env.production` 改为同源 `/api`，去掉硬编码的外部 Railway 域名
  > 验收: 生产产物不含旧域名，走 Nginx 反代
  > 证据: 容器内 `grep -rl 'up.railway.app'` → 未发现旧域名

## Phase 3: 配置一致性与数据隔离（已完成）

- [x] T020 修复 `application-dev.properties` 中文注释乱码，显式声明演示开关
  > 验收: 注释可读且 `app.demo-data.enabled=true`
  > 证据: `src/main/resources/application-dev.properties`

- [x] T021 `application.properties` 增加双开关、方言自动识别、关闭 `open-in-view`
  > 验收: 无 H2Dialect 与 open-in-view 警告
  > 证据: 第二轮 `mvn test` 输出无相关 WARN

- [x] T022 `application-prod.properties` 设 `app.demo-data.enabled=false`
  > 验收: prod 不写入弱口令账号与模拟订单
  > 证据: prod 日志 `用户数: 0`、`历史订单: 0`

- [x] T023 `DataInitializer` 增加 `@ConditionalOnProperty` + `@Value` 双开关，`System.out.println` → SLF4J
  > 验收: test profile 不含 `dataInitializer` Bean；prod 仍补齐基础数据
  > 证据: 单测 `dataInitializerShouldBeDisabledInTestProfile`；prod 日志 `补齐未来7天余票 1253 条`

- [x] T024 `GlobalExceptionHandler` 补 `ConstraintViolationException` → 400
  > 验收: 方法级参数校验返回首条错误信息
  > 证据: `common/GlobalExceptionHandler.java`

- [x] T025 `pom.xml` 补 `micrometer-registry-prometheus` 与项目元数据
  > 验收: `prometheus` 端点有对应注册表实现
  > 证据: `pom.xml`；`docker info` 无关的 Actuator 端点数为 5

- [x] T026 新增 `src/test/resources/application-test.properties`（H2 内存库）
  > 验收: 测试无需本机 MySQL
  > 证据: `.\mvnw.cmd -B test` 在无 MySQL 连接配置下 `BUILD SUCCESS`

## Phase 4: 测试、CI 与文档（已完成）

- [x] T030 把空的 `contextLoads()` 改为有效断言，并启用 `@ActiveProfiles("test")`
  > 验收: 断言上下文与关键 Bean 非空、验证 Bean 条件开关
  > 证据: `Demo2ApplicationTests` 2 个测试

- [x] T031 新增 `JwtUtilTest`（生成/篡改/过期）
  > 验收: 3 个测试通过
  > 证据: `Tests run: 3 -- JwtUtilTest`

- [x] T032 新增 `UserServiceTest`（重名、加密、登录、改密、资料校验）
  > 验收: 7 个测试通过
  > 证据: `Tests run: 7 -- UserServiceTest`

- [x] T033 新增 `OrderServiceTest`（余票不足、下单、越权、回补）
  > 验收: 4 个测试通过
  > 证据: `Tests run: 4 -- OrderServiceTest`

- [x] T034 新增 `.github/workflows/ci.yml`（backend / frontend / compose 三个 job）
  > 验收: push 与 PR 触发，命令与本地一致
  > 证据: `.github/workflows/ci.yml`

- [x] T035 撰写 `docs/SDD.md`（软件设计文档 v1.0）
  > 验收: 498 行，3 张 Mermaid 图，数字与代码一致
  > 证据: 一致性校验命令（23 端点 / 18 测试 / 7 实体 / 8 nativeQuery）
  > 说明: 本次规格化已把内容整合进 `spec.md` + `plan.md`，原文件由 T055 删除

- [x] T036 第一轮本地验证：后端测试、前端构建、Compose 配置校验
  > 验收: 三项全绿
  > 证据: `16 tests / BUILD SUCCESS`、`built in 3.77s`、`compose config OK`

## Phase 5: 运行实测与缺陷修复（已完成）

- [x] T040 按用户指定路径执行 `git add`
  > 验收: 部署必需文件进入暂存区
  > 证据: `git diff --cached --name-status` 含 `.gitignore`、`.mvn/`、`.vscode/`、`docker/`、`deploy/`、`.github/`、`src/test/`

- [x] T041 启动 Docker Desktop，并为 daemon 配置可达的 registry mirror
  > 验收: `docker info` 就绪且 Registry Mirrors 生效
  > 证据: `docker daemon READY`；`docker info` 显示两个 mirror
  > 说明: 用户级环境改动 `~/.docker/daemon.json`，已备份 `daemon.json.bak`，不写入仓库

- [x] T042 执行 `Copy-Item .env.example .env`
  > 验收: 生成 `.env` 且被 gitignore 排除
  > 证据: 根目录 `.env` 存在，`git status` 不显示

- [x] T043 执行 `docker compose up -d --build`（含 3306 冲突后的重试）
  > 验收: 两个镜像构建成功、三个容器启动
  > 证据: `Image demo2-backend Built` / `demo2-frontend Built` / 三个 `Started`

- [x] T044 首轮容器健康与接口冒烟，暴露 Nginx 路径丢失缺陷
  > 验收: 记录失败现象与后端异常栈
  > 证据: `/api/station/list` 返回 `code:500`；日志 `NoResourceFoundException: No static resource api for request '/api/'`

- [x] T045 修复 `docker/nginx.conf` 的 `proxy_pass`（固定 URI → `$request_uri`）并重建前端镜像
  > 验收: 站点列表、带查询串搜索、看板概览均 `code:200`
  > 证据: 复验 4 条链路全部 HTTP 200 / `code:200`

- [x] T046 补 `NoResourceFoundException` → HTTP 404 处理器与 2 个单测，重建 backend 镜像
  > 验收: 未知路径直连与经 Nginx 均返回 404，正常接口不受影响
  > 证据: `{"code":404,...}`；`18 tests / BUILD SUCCESS`；backend `healthy`

- [x] T047 全链路复验（含静态资源、健康检查、prod 数据隔离）
  > 验收: 首页/入口 JS/健康检查 200，`/api` 200，未知路径 404，prod 用户与订单为 0
  > 证据: Phase 5 复验输出；prod 日志 `用户数: 0`、`历史订单: 0`、车次 179

## Phase 6: 规格化（本次，已完成）

- [x] T050 创建 `.specify/memory/constitution.md`（5 条原则 + 技术基线 + 质量门禁）
  > 验收: 每条原则可对应到待办任务 ID
  > 证据: 本文件 Phase 8 待办的「宪法」列

- [x] T051 创建 `.specify/templates/` 下 `spec-template.md`、`plan-template.md`、`tasks-template.md`
  > 验收: 模板与三件套章节结构一致，可被后续特性复用
  > 证据: 三个模板文件

- [x] T052 把 `docs/SDD.md` 整合改写为 `specs/001-railway-ticket/spec.md`
  > 验收: 8 个用户故事、28 条 FR、12 条 NFR、8 条成功标准、真实验收记录
  > 证据: `spec.md`

- [x] T053 编写 `plan.md`（技术上下文、宪法检查、架构、11 条 ADR、数据模型、23 端点契约、部署与验证）
  > 验收: 每条 ADR 关联 FR/NFR，含备选与否决原因、证据
  > 证据: `plan.md`

- [x] T054 编写 `tasks.md`（已完成回填 + 遗留待办 + 统计）
  > 验收: 已完成项均附验收与证据，待办项标注优先级/依赖/宪法原则
  > 证据: 本文件

- [x] T055 删除 `docs/SDD.md`，避免两份事实来源
  > 验收: 全仓库无指向 `docs/SDD.md` 的有效链接（仅保留历史说明）
  > 证据: 删除前的引用扫描结果为空

- [x] T056 一致性校验：文件清单、数字、链接与代码反向核对
  > 验收: 端点 23 / `@Test` 20 / `@Entity` 7 / `nativeQuery` 8，且引用路径全部存在
  > 证据: 校验命令输出

- [x] T057 分域名部署配置化：CORS 白名单 + 前端 API 地址（对应 spec 待澄清问题 3）
  > 验收: 不改代码即可通过 `APP_CORS_ALLOWED_ORIGINS` / `VITE_API_BASE_URL` 完成分域名部署；同源部署默认行为不变
  > 证据: `WebConfigTest` 2 个测试；`20 tests / BUILD SUCCESS`；ADR-011；README 分域名部署小节

- [x] T058 后端镜像构建提速：`pom.xml` → `dependency:go-offline` 独立层，再 `COPY src/` + 打包
  > 验收: 改 Java 源码不再触发 Maven 依赖下载；Dockerfile 语法校验通过
  > 证据: `docker build --check -f docker/Dockerfile.backend .` → `Check complete, no warnings found`；原重建实测 5:11

- [x] T059 为 `frontend` 增加 healthcheck，三服务健康状态可观测
  > 验收: `docker compose ps` 中 mysql / backend / frontend 均 healthy
  > 证据: nginx 镜像内 `/usr/bin/wget` 可用；recreate 后 frontend `healthy`，首页与 `/api` 经反代仍 200

## Phase 7: 质量门禁（复核清单）

| 门禁 | 命令 | 当前状态 |
| --- | --- | --- |
| 后端测试 | `.\mvnw.cmd -B verify` | PASS（2026-11-18：57 tests / 0 failures / 6 skipped；Docker 就绪时 0 skipped） |
| 前端单测 | `cd frontend && npm test` | PASS（11 tests） |
| 前端规范 / 格式 | `npm run lint` / `npm run format:check` | PASS（0 errors / 0 warnings；全部文件符合 Prettier） |
| 前端 E2E | `npm run test:e2e` | PASS（2 tests，mock API） |
| 前端构建 | `cd frontend && npm ci && npm run build` | PASS（最新 1.46s） |
| 依赖安全 | `npm audit --audit-level=moderate` | PASS（0 vulnerabilities；移除未使用的 `@vue/test-utils`，`happy-dom` 升到 21.0.4） |
| Compose 语法 | `docker compose config --quiet` | PASS（2026-11-17 复验） |
| Dockerfile 语法 | `docker build --check -f docker/Dockerfile.backend .` | PASS（no warnings） |
| 容器健康 | `docker compose ps` | PASS（2026-11-17 复验：mysql/redis/backend/frontend/zipkin 均 healthy） |
| 接口冒烟 | 首页、`/api/station/list`、未知路径 | PASS（200 / 200 / 401 未登录或 404 带令牌；均非 500） |
| CORS 配置 | `WebConfigTest` | PASS（白名单解析 / 空值兜底） |
| 文档一致性 | 数字与引用反向核对 | PASS（T056；2026-11-17 再次对齐 spec/plan/README） |

## Phase 8: 待办与风险登记

- [x] T101 [高] [宪法 III] CORS 收紧并引入完整安全体系（Spring Security、refresh token、登出黑名单）
  > 验收: prod 仅允许配置的来源；令牌可刷新、登出后旧令牌失效；`/api` 端点权限模型有测试覆盖
  > 依赖: T103
  > 证据: `TokenService`（黑名单/一次性 refresh）、`JwtAuthFilter`、`WebConfig` CORS 白名单；`JwtAuthFilterTest`/`TokenServiceTest`；浏览器 E2E 验证 401→refresh→重放、登出后旧令牌 401/旧 refresh 400

- [x] T102 [高] [宪法 I] 引入 Flyway/Liquibase 迁移脚本，替代 `ddl-auto=update`
  > 验收: 有版本化 SQL、可回滚、CI 验证迁移；prod 不再自动改表
  > 证据: `db/migration/V1__init.sql`、`V2__orders_seat_unique.sql`；prod `ddl-auto=none` + `spring-boot-flyway`；实测 `flyway_schema_history` 含 baseline v1 与 v2（success=1）

- [x] T103 [高] [宪法 III] 鉴权改为「默认拦截 + 显式放行」，修正白名单外免鉴权
  > 验收: `/api/train/**`、`/api/dashboard/**` 的鉴权策略有明确结论并写入 `spec.md`；新增端点默认需登录
  > 依赖: spec.md 待澄清问题 2
  > 证据: `JwtAuthFilter.PUBLIC_API_PREFIXES` 显式放行查询类接口；`/api/ai/**` 等新增端点默认拦截；结论写入 `spec.md` 第 9 章

- [x] T104 [中] [宪法 III] 密钥与数据库口令取消内置默认值，生产强制校验
  > 验收: 缺少 `JWT_SECRET`/`DB_*` 时 prod 启动失败且提示明确；`.env.example` 同步更新
  > 证据: `ProdSecurityValidator` + `ProdSecurityValidatorTest`（4 例）；compose 保留一键启动默认值，prod 启动强制校验（详见 `spec.md` 第 9 章决策）

- [x] T105 [中] [宪法 I] 为座位号加数据库唯一约束或分布式分配机制
  > 验收: 并发下单不会产生相同 `(train_id, train_date, seat_type, seat_no)`
  > 证据: `Order` 实体 `@UniqueConstraint(uk_orders_seat)` + Flyway V2（清洗历史重复/失效号后建约束）；`OrderService` 用 `TransactionTemplate` + `saveAndFlush` 做冲突回滚重试，退票加 `#F` 释放标记；`OrderServiceTest` 6 例（含重试成功/耗尽）；真实 MySQL 6 路并发下单座位号全不同，退票后复购正常

- [x] T106 [中] [宪法 I/IV] 清理未使用的 `stations`/`trains` 缓存名；定时任务单条异常记录日志而非吞掉
  > 验收: 配置与代码一致；异常有可检索日志
  > 证据: `spring.cache.cache-names=tickets,stations,trains` 与 `StationService`/`TrainService` 的 `@Cacheable` 一致；`OrderAutoCompleteTask` 单条异常 `log.warn` 记录

- [x] T107 [中] [宪法 V] 选定协议并创建 `LICENSE`，补齐 `pom.xml` 元数据
  > 验收: 根目录有 LICENSE；`pom.xml` 的 `<licenses>` 不再是空标签
  > 证据: 选定 MIT，创建根目录 `LICENSE`（Copyright 2026 Railway Ticket contributors）；`pom.xml` 补齐 `<licenses>`/`<developers>`/`<scm>`/`<url>`；授权协议结论写入 `spec.md` 第 9 章

- [x] T108 [中] [宪法 IV] 补前端 E2E 测试与 ESLint/Prettier 配置
  > 验收: `npm run test`/`npm run lint` 脚本可用且接入 CI
  > 证据: 新增 `eslint.config.js`（ESLint 9 flat config + eslint-plugin-vue，兼容自动导入）、`.prettierrc.json`、`.prettierignore`；`package.json` 增加 `lint`/`lint:fix`/`format`/`format:check`；CI 前端 job 接入 lint + format:check；浏览器 E2E 验证（刷新令牌并发重放、主动登出、服务端黑名单/撤销）已通过；修掉 Home.vue 无用 catch-rethrow 与 Prettier 拆坏的多语句模板处理器；2026-11-17 清理 75 条 lint 警告至 0 errors/0 warnings；`npm run lint`/`format:check`/`build` 均 exit=0

- [x] T109 [低] [宪法 IV] 实测依赖分层后的重建耗时
  > 验收: 仅改 Java 源码时后端镜像重建时间降到 1 分钟内
  > 证据: 首次构建（建立 `dependency:go-offline` 依赖层）474.0s；模拟仅改源码重建 **16.0s**；恢复源码后 1.8s，三次均 `exit=0`

- [x] T110 [低] [宪法 III] 清理孤立的 `Confetti.vue`；AI 助手密钥改由后端代理，避免存 `localStorage`
  > 验收: 无未引用组件；浏览器不长期持有模型密钥
  > 证据: 删除无引用的 `frontend/src/components/Confetti.vue`；新增 `AiProperties`/`AiService`/`AiController`，密钥仅由 `AI_API_KEY` 环境变量注入服务端；`application.properties` 增加 `ai.*` 配置；前端 `AIChat.vue` 删除 `localStorage` 密钥读写与密钥/端点输入，改调 `/api/ai/status`、`/api/ai/chat`；实测 `/ai/status` 不泄露密钥、未配置时前端显示「模拟模式」并降级

- [x] T111 [低] [宪法 IV] 补日志归档与链路追踪配置
  > 验收: 生产日志可检索、请求可关联
  > 证据: 新增 `TraceIdFilter`（最高优先级，生成/透传/清洗 `X-Trace-Id`，写 MDC 与响应头，异常时清理）+ `TraceIdFilterTest` 4 例；`logback-spring.xml` 日志格式含 `[app,traceId,thread]`，prod 按天+大小滚动归档（保留 30 天 / 3GB 上限 / gzip），dev 仅控制台；实测响应头生成/透传/清洗正常，业务异常日志 `[railway-ticket,trace-log-demo-001,...]` 带 traceId；Micrometer/Prometheus 指标（`cache_gets_total` 等）早已接入

- [x] T112 [高] [宪法 V] 暂存全部源码/配置修复与本次 Spec Kit 文件
  > 验收: `git status` 无遗漏的修复文件；`git diff --cached` 包含 `pom.xml`、`DataInitializer.java`、`GlobalExceptionHandler.java`、`application*.properties`、`specs/`、`.specify/`
  > 证据: `git add -u` 后暂存区 65 个文件，`git diff --cached --check` 无输出

- [x] T113 [中] [宪法 V] 决策 `spec.md` 第 9 章剩余 `[NEEDS CLARIFICATION]` 并更新三件套
  > 验收: 待澄清问题清零；结论同步到 `plan.md` 与 `tasks.md`
  > 说明: 原第 3 项「分域名部署」已由 T057 配置化解决
  > 证据: 4 项全部决策并写入 `spec.md` 第 9 章——prod 账号=开放自注册；鉴权边界=默认拦截+显式放行；演示默认值=compose 保留默认、prod 强制校验；授权协议=MIT（已创建 LICENSE）

## Phase 9: 可选增强闭环（已完成）

- [x] T114 [中] [宪法 IV] 固化前端自动化测试（Vitest 单测 + Playwright E2E）
  > 证据: `vitest` 4 + `happy-dom` 21（`@vue/test-utils` 未被使用已移除），`src/**/*.test.js` 共 11 例（auth 存储/事件、request 401 刷新/并发队列/登出跳过）；`@playwright/test` + `tests/e2e/auth.spec.js` 2 例（登录存储+登出清理、401 自动刷新重放），全部用 `page.route` mock API 不依赖后端；`npm run test`/`test:e2e` 可用；本地用系统 Edge、CI 用 chromium；CI 接入 `npm test` + `playwright install` + `test:e2e`；2026-11-17 升级 `happy-dom` 至 21.0.4 后 `npm audit` 为 0 vulnerabilities

- [x] T115 [中] [宪法 IV] 接入 Micrometer Tracing + Brave + 可选 Zipkin collector
  > 证据: Boot 4.1.1 无 tracing 自动配置，`TracingConfig` 手动装配 `BraveTracer`（`localServiceName`、Sampler、`AsyncReporter`+`URLConnectionSender`→`ZipkinSpanHandler`）；`TraceIdFilter` 注入 Tracer 创建 SERVER span，日志/响应头/Zipkin 共用同一 traceId，入站 `X-Trace-Id` 作为 span tag；`TracingConfigTest` 2 例；Compose 新增 `zipkin` profile 服务（`--profile tracing`）；实测 `GET /api/v2/services` 返回 `railway-ticket`，span name=`get /actuator/health`、traceId=`37772fe82f26c2eb`；`ZIPKIN_ENDPOINT` 默认空不上报

## Phase 10: 单体项目 deepening（RBAC 与管理后台）

- [x] T116 [高] [宪法 III] 引入 USER/ADMIN 角色与管理端接口，前端增加管理后台
  > 验收: JWT 携带角色；`/api/admin/**` 非管理员 403；注册不可自我提权；管理端可维护用户角色、订单查询、站点/车次 CRUD、余票调整；被引用站点与有订单车次不可删除；余票调整后缓存失效；前端管理页仅管理员可见且有路由守卫
  > 证据: `entity/UserRole.java`、`db/migration/V3__user_role.sql`、`JwtUtil` 3 参 generateToken + getRole、`JwtAuthFilter` ADMIN 前缀 403、`AdminService`/`AdminController` 13 端点、`views/Admin.vue`；`AdminServiceTest` 5 例、`JwtAuthFilterTest` 7 例、`JwtUtilTest` 4 例；`.\mvnw.cmd -B test` → 57 tests / 0 failures / 6 skipped；前端 lint/format/test/build/E2E 全绿

## 统计

| 状态 | 数量 |
| --- | --- |
| 已完成 | 63 |
| 待办 | 0 |
| 合计 | 63 |

Phase 8/9/10 全部完成。第二批新增完成：T107、T108、T111、T113；第三批新增完成：T116。

> 2026-11-18 复核：63 项全部完成；`spec.md`、`plan.md`、`README.md` 同步到当前代码（40 端点 / 57 测试 / Redis + Flyway V3 + Zipkin + RBAC 管理后台）。

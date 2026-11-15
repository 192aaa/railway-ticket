# Railway Ticket Constitution

**Version**: 1.0.0
**Ratified**: 2026-11-14
**Status**: Active
**Scope**: 本仓库全部源码、配置、部署产物与文档

本宪法定义不可协商的工程原则。实现与本文冲突时，应先修订本文并说明理由，再改实现。所有违反项必须登记到 `tasks.md`，不得隐藏。

## I. 数据库是并发真相

- 余票扣减/回补必须使用带条件的原子 `UPDATE`（`WHERE count > 0`），禁止「先查后改」。
- 库存与订单的写操作必须在同一 `@Transactional` 边界内，并在写后失效余票缓存。
- 表结构变更必须可追溯、可回滚；`ddl-auto=update` 仅为当前过渡手段。

## II. 契约稳定

- HTTP API 统一 `/api` 前缀、`Result<T>{code,message,data}` 包装。
- 前端只依赖 `body.code` 与 `body.data`，不耦合后端实现细节。
- 新增/变更端点必须同步更新 `spec.md` 的功能需求与 `plan.md` 的接口契约。

## III. 环境隔离与最小权限

- prod 禁止写入弱口令账号与模拟订单；演示数据只存在于 dev。
- 密钥与数据库口令由环境变量 / `.env` 注入；仓库只保留 `.env.example`。
- 新增端点默认视为需要鉴权；显式放行必须逐条列出并写入文档。

## IV. 可验证交付

- 每个功能改动必须有对应测试；`.\mvnw.cmd -B verify` 与 `npm run build` 必须全绿。
- 部署链路变更必须通过 `docker compose config --quiet` 与容器健康/接口冒烟验证。
- 文档中的数字（端点数、测试数、端口）必须能与代码反向核对。

## V. 文档与代码同源

- Specify → Plan → Task 三件套是需求、计划与进度的唯一事实来源。
- 任务状态与实现同步：完成即勾选并附验证证据。
- 设计决策以 ADR 形式记录在 `plan.md`，并写明被否定的备选方案。

## 技术基线

| 项 | 基线 |
| --- | --- |
| 后端 | Java 17、Spring Boot 4.1.1、Spring Data JPA、Caffeine、JJWT、BCrypt、Actuator |
| 前端 | Vue 3、Vue Router、Pinia、Element Plus、Axios、Vite 8 |
| 数据 | MySQL 8（dev / prod）、H2 内存库（test） |
| 部署 | Docker Compose + Nginx（容器/裸机两套配置）；Render 仅部署后端 |
| 时区 | `Asia/Shanghai` |

变更基线需新增 ADR，并同步 `spec.md` / `plan.md`。

## 质量门禁

1. `.\mvnw.cmd -B verify`（当前 20 个测试，0 失败）
2. `cd frontend && npm ci && npm run build`
3. `docker compose config --quiet`
4. 容器 healthcheck 与 `/api` 冒烟（含未知路径的 404 校验）
5. 文档无断链、无未填充的占位符

## 治理

- 宪法修订需在 `plan.md` 追加 ADR，注明版本号与日期。
- 演进顺序固定为：更新 `spec.md` → 更新 `plan.md` → 拆分 `tasks.md`。
- 遗留风险按优先级登记在 `tasks.md` 的待办区，与本宪法原则逐条对应。

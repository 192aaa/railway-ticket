# Implementation Plan: [FEATURE NAME]

**Branch**: `[###-feature-name]`
**Date**: [DATE]
**Spec**: [../spec.md 的相对路径]
**Input**: 来自 `spec.md` 的功能需求、验收场景与成功标准

> 本模板用于 Plan 阶段，只回答 **HOW**。每个决策须能追溯到 `spec.md` 的 FR/NFR 编号。

## Summary

[一段话说明技术方案与交付路径]

## Technical Context

| 层 | 选型 | 版本 | 理由 / 约束 |
| --- | --- | --- | --- |
| 后端 | [框架] | [版本] | [理由] |
| 前端 | [框架] | [版本] | [理由] |
| 数据 | [数据库] | [版本] | [理由] |
| 部署 | [方式] | — | [理由] |
| 测试 | [框架] | — | [理由] |

## Constitution Check

| 原则 | 满足 | 说明 / 豁免理由 |
| --- | --- | --- |
| I. 数据库是并发真相 | ☐ | [说明] |
| II. 契约稳定 | ☐ | [说明] |
| III. 环境隔离与最小权限 | ☐ | [说明] |
| IV. 可验证交付 | ☐ | [说明] |
| V. 文档与代码同源 | ☐ | [说明] |

## Architecture

```mermaid
flowchart LR
    A[客户端] --> B[接入层] --> C[服务层] --> D[(数据存储)]
```

## Project Structure

```
[目录树，标注每个目录的职责与关键文件]
```

## Key Design Decisions (ADR)

### ADR-001: [决策标题]

- **关联需求**: FR-xxx / NFR-xxx
- **决策**: [采用了什么]
- **理由**: [为什么]
- **备选与否决原因**: [考虑过什么、为什么不用]
- **证据**: [文件路径 / 实测结果]

## Data Model

| 实体 / 表 | 主要字段 | 约束 |
| --- | --- | --- |
| [实体] | [字段] | [唯一/非空/索引] |

[ER 图或关系说明]

## API Contracts

| 方法 | 路径 | 入参 | 返回 | 鉴权 |
| --- | --- | --- | --- | --- |
| [METHOD] | [/path] | [参数] | [Result<T>] | [公开/需登录] |

## Configuration

| 配置 / 变量 | dev | prod | test |
| --- | --- | --- | --- |
| [项] | [值] | [值] | [值] |

## Deployment & Verification

- 构建与启动步骤：
- 健康检查与端口：
- 冒烟清单（含失败路径）：

## Testing Strategy

| 层级 | 手段 | 覆盖范围 |
| --- | --- | --- |
| 单元 | [框架] | [范围] |
| 集成 | [框架] | [范围] |
| CI | [流水线] | [job 列表] |

## Known Limitations

[逐条列出，并与 `tasks.md` 的待办 ID 对应；不得只写「后续优化」]

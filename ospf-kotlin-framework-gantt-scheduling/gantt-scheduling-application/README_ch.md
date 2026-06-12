# gantt-scheduling-application

:us: [English](README.md) | :cn: 简体中文

求解编排层 — 提供业务入口（APS、MPS、LSP）和算法服务（分支定价、列生成），协调所有领域子模块以生成调度方案。

## 关键类型

| 类型 | 描述 |
|------|------|
| `APS` | 高级计划与排程 — 业务入口 |
| `MPS` | 主生产排程 — 业务入口 |
| `LSP` | 批次排程与计划 — 业务入口 |
| `BranchAndPriceAlgorithm` | 分支定价求解器（束变体与任务变体） |
| `ColumnGenerationAlgorithm` | 列生成求解器（束变体与任务变体） |
| `Iteration` | 求解过程中的单次迭代状态 |
| `IterationSnapshot` | 用于分析的迭代数据快照 |

## 依赖

- `gantt-scheduling-infrastructure`
- `gantt-scheduling-domain-task-context`
- `gantt-scheduling-domain-bunch-generation-context`
- `gantt-scheduling-domain-capacity-scheduling-context`
- `gantt-scheduling-domain-resource-context`
- `gantt-scheduling-domain-produce-context`

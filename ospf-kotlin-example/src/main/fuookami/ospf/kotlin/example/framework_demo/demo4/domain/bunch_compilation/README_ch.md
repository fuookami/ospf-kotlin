# 批次编译（Bunch Compilation）

:us: [English](README.md) | :cn: 简体中文

负责将航班任务束编译到列生成优化模型中的限界上下文。管理任务时间、流量、车队平衡、航班链接和航班容量等约束向线性元模型的注册，并支持分支定价迭代过程中的增量列添加。

## 职责

- 将任务时间、流量、车队平衡、航班链接和航班容量约束注册到优化模型。
- 管理 `Compilation`（列生成决策变量）和 `PipelineList`（约束生成管线）。
- 为定价迭代中发现的新束添加列。
- 基于影子价格为分支定价算法选择空闲执行器。

## 核心类

| 类名 | 说明 |
|---|---|
| `BunchCompilationContext` | 管理聚合和管线注册的入口上下文。 |
| `Aggregation` | 组合 `TaskTime`、`Flow`、`FleetBalance`、`FlightLink` 和 `FlightCapacity` 子聚合。 |
| `Compilation` | 专门用于航班任务类型的 `BunchCompilation` 类型别名。 |
| `FlightLink` | 建模连续航班任务间连接的链接表达式。 |
| `FleetBalance` | 确保飞机在各机场的分布与预期车队组成匹配。 |
| `FlightCapacity` | 跟踪航班任务束的乘客和货物容量。 |

## 依赖

- **task** — 提供 `FlightTask`、`Aircraft`、`FlightTaskBunch`、`FlightTaskAssignment`、`ShadowPriceMap`。
- **rule** — 提供 `Link`、`LinkMap`、`Flow`。
- **framework (gantt_scheduling)** — 提供基础 `BunchCompilationContext`、`BunchCompilationAggregation`、`BunchSchedulingTaskTime`、`BunchSchedulingConnectionResourceUsage`。

# gantt-scheduling-domain-capacity-scheduling-context

:us: [English](README.md) | :cn: 简体中文

基于时间槽的产能调度 — 建模产能列、编译调度约束，并定义主问题的产能相关限制。

## 关键类型

| 类型 | 描述 |
|------|------|
| `Capacity` | 调度实体的产能定义 |
| `CapacityColumn` | 表示时间槽中产能分配的列 |
| `CapacityColumnAggregation` | 产能列的聚合 |
| `CapacityCompilation` | 将产能数据编译到调度模型 |
| `CapacityOrderCompilation` | 编译订单特定的产能约束 |
| `IterativeCapacityCompilation` | 产能精化的迭代编译 |
| `CapacitySchedulingSolution` | 产能调度的解输出 |
| `ProductionAction` | 表示生产决策的动作 |
| `CapacitySolverValue` | 产能变量的求解器层值 |
| `CapacitySchedulingContext` | 产能调度的领域上下文 |
| `CapacitySchedulingAggregation` | 产能调度结果的聚合根 |
| `CapacityCostMinimization` | 限制：最小化总产能成本 |
| `ExecutorCapacityConstraint` | 限制：约束执行者产能使用 |
| `OrderConstraint` | 限制：强制订单满足约束 |

## 依赖

- `gantt-scheduling-infrastructure`
- `gantt-scheduling-domain-task-context`

# gantt-scheduling-domain-produce-context

:us: [English](README.md) | :cn: 简体中文

物料生产与消耗跟踪及需求满足 — 建模生产/消耗流，跟踪跨调度上下文的数量，并强制生产和消耗限制。

## 关键类型

| 类型 | 描述 |
|------|------|
| `Produce` | 物料生产定义 |
| `Consumption` | 物料消耗定义 |
| `ProduceSlack` | 生产约束松弛的松弛变量 |
| `ProduceSolverValue` | 生产变量的求解器层值 |
| `ProductionTask` | 生产或消耗物料的任务 |
| `BunchCapacitySchedulingProduce` | 束产能调度中的生产跟踪 |
| `CapacityActionProduce` | 与产能动作关联的生产 |
| `CapacitySchedulingProduce` | 产能调度中的生产跟踪 |
| `PlanCapacitySchedulingProduce` | 计划级产能调度中的生产跟踪 |
| `ProduceQuantityConstraint` | 限制：强制生产数量边界 |
| `ProduceQuantityMaximization` | 限制：最大化总生产量 |
| `ProduceQuantityMinimization` | 限制：最小化总生产量 |
| `ProduceOverQuantityMinimization` | 限制：最小化过量生产 |
| `ProduceLessQuantityMinimization` | 限制：最小化不足生产 |
| `ConsumptionQuantityConstraint` | 限制：强制消耗数量边界 |
| `ConsumptionQuantityMaximization` | 限制：最大化总消耗量 |
| `ConsumptionQuantityMinimization` | 限制：最小化总消耗量 |
| `ConsumptionOverQuantityMinimization` | 限制：最小化过量消耗 |
| `ConsumptionLessQuantityMinimization` | 限制：最小化不足消耗 |

## 依赖

- `gantt-scheduling-infrastructure`
- `gantt-scheduling-domain-task-context`

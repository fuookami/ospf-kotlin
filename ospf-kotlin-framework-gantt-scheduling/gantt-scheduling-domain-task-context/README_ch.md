# gantt-scheduling-domain-task-context

:us: [English](README.md) | :cn: 简体中文

甘特调度框架的核心任务模型。定义任务、执行器、任务束、分配策略和成本结构，构成编译和生成子模块共享的领域词汇。

## 关键类型

| 类型 | 描述 |
|------|------|
| `AbstractTask` | 所有可调度任务的基类 |
| `AbstractUnplannedTask` | 尚未分配时间槽的任务 |
| `AbstractPlannedTask` | 已确认时间分配的任务 |
| `AbstractTaskBunch` | 作为整体调度的一组任务 |
| `Executor` | 能够执行任务的资源 |
| `AssignmentPolicy` | 管理任务分配给执行器的策略 |
| `TaskStatus` | 表示任务生命周期状态的枚举 |
| `TaskType` | 任务种类的分类 |
| `TaskKey` | 调度上下文中任务的唯一标识符 |
| `Cost` | 调度决策的成本表示 |
| `ShadowPriceMap` | LP 松弛的对偶值，用于列生成 |
| `SchedulingSolverValueAdapter` | 将领域值桥接到求解器表示的适配器 |

## 依赖

- [gantt-scheduling-infrastructure](../gantt-scheduling-infrastructure/) — 时间原语

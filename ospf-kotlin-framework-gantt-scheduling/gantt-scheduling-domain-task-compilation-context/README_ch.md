# gantt-scheduling-domain-task-compilation-context

:us: [English](README.md) | :cn: 简体中文

甘特调度框架的任务级 MILP 编译模块。将任务领域模型转化为混合整数线性规划求解器的决策变量、约束和目标项。

## 关键类型

| 类型 | 描述 |
|------|------|
| `Compilation` | MILP 模型构建的基础编译上下文 |
| `TaskCompilation` | 单个任务决策的编译上下文 |
| `IterativeTaskCompilation` | 支持迭代（列生成）工作流的编译上下文 |
| `TaskAggregation` | 任务级决策变量的聚合 |
| `TaskTime` | 任务开始/结束时间的决策变量 |
| `Switch` | 执行器任务切换的决策变量 |
| `Makespan` | 最小化总调度长度的目标项 |
| `TaskSolution` | 从编译的任务变量中提取解 |

## 限制（约束与目标）

| 限制 | 描述 |
|------|------|
| `ExecutorCompilationConstraint` | 确保执行器容量得到遵守 |
| `ExecutorCostMinimization` | 最小化执行器总成本 |
| `ExecutorLeisureMinimization` | 最小化执行器空闲时间 |
| `SwitchCostMinimization` | 最小化执行器切换成本 |
| `TaskCancelMinimization` | 最小化取消任务数量 |
| `TaskCostMinimization` | 最小化任务总成本 |
| `TaskCompilationConstraint` | 确保任务编译一致性 |
| `TaskConflictConstraint` | 防止任务时间重叠 |
| `TaskTimeConflictConstraint` | 防止任务间时间级冲突 |
| `TaskStepConflictConstraint` | 防止任务间步骤级冲突 |
| `TaskOnTimeMaximization` | 最大化按时完成任务数 |
| `TaskNotOnTimeMinimization` | 最小化延迟完成任务数 |

## 依赖

- [gantt-scheduling-infrastructure](../gantt-scheduling-infrastructure/) — 时间原语
- [gantt-scheduling-domain-task-context](../gantt-scheduling-domain-task-context/) — 任务领域模型

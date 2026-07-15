# gantt-scheduling-domain-resource-context

:us: [English](README.md) | :cn: 简体中文

带松弛变量的可消耗资源产能约束 — 建模执行、连接和存储资源，跟踪跨调度上下文的使用量，并强制资源限制。

## 关键类型

| 类型 | 描述 |
|------|------|
| `Resource` | 基础资源定义 |
| `ExecutionResource` | 任务执行期间消耗的资源 |
| `ConnectionResource` | 任务间转换期间消耗的资源 |
| `StorageResource` | 随时间存储的资源 |
| `ResourceSlack` | 资源约束松弛的松弛变量 |
| `ResourceSolverValue` | 资源变量的求解器层值 |
| `ResourceUsage` | 跟踪时间槽中的资源消耗 |
| `ResourceTimeSlot` | 带有资源使用信息的时间槽 |
| `BunchCapacitySchedulingResourceUsage` | 束产能调度中的资源使用 |
| `CapacitySchedulingResourceUsage` | 产能调度中的资源使用 |
| `CapacityActionResource` | 与产能动作关联的资源 |
| `PlanCapacitySchedulingResourceUsage` | 计划级产能调度中的资源使用 |
| `ResourceCapacityConstraint` | 限制：强制资源产能边界 |
| `ResourceOverQuantityMinimization` | 限制：最小化资源过量使用 |
| `ResourceLessQuantityMinimization` | 限制：最小化资源不足使用 |

## 依赖

- `gantt-scheduling-infrastructure`
- `gantt-scheduling-domain-task-context`

# gantt-scheduling-domain-resource-context

:us: English | :cn: [简体中文](README_ch.md)

Consumable resource capacity constraints with slack variables — models execution, connection, and storage resources, tracks usage across scheduling contexts, and enforces resource limits.

## Key Types

| Type | Description |
|------|-------------|
| `Resource` | Base resource definition |
| `ExecutionResource` | Resource consumed during task execution |
| `ConnectionResource` | Resource consumed during transitions between tasks |
| `StorageResource` | Resource held in storage over time |
| `ResourceSlack` | Slack variable for resource constraint relaxation |
| `ResourceSolverValue` | Solver-level value for resource variables |
| `ResourceUsage` | Tracks resource consumption at a time slot |
| `ResourceTimeSlot` | Time slot with resource usage information |
| `BunchCapacitySchedulingResourceUsage` | Resource usage in bunch capacity scheduling |
| `CapacitySchedulingResourceUsage` | Resource usage in capacity scheduling |
| `CapacityActionResource` | Resource associated with a capacity action |
| `PlanCapacitySchedulingResourceUsage` | Resource usage in plan-level capacity scheduling |
| `ResourceCapacityConstraint` | Limit: enforce resource capacity bounds |
| `ResourceOverQuantityMinimization` | Limit: minimize resource over-usage |
| `ResourceLessQuantityMinimization` | Limit: minimize resource under-usage |

## Dependencies

- `gantt-scheduling-infrastructure`
- `gantt-scheduling-domain-task-context`

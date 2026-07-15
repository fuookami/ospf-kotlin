# gantt-scheduling-domain-task-context

:us: English | :cn: [简体中文](README_ch.md)

Core task model for the gantt-scheduling framework. Defines tasks, executors, task bunches, assignment policies, and cost structures that form the domain vocabulary shared by compilation and generation sub-modules.

## Key Types

| Type | Description |
|------|-------------|
| `AbstractTask` | Base class for all schedulable tasks |
| `AbstractUnplannedTask` | Task that has not yet been assigned a time slot |
| `AbstractPlannedTask` | Task with a confirmed time assignment |
| `AbstractTaskBunch` | Group of tasks scheduled together as a unit |
| `Executor` | Resource capable of performing tasks |
| `AssignmentPolicy` | Policy governing how tasks are assigned to executors |
| `TaskStatus` | Enum representing the lifecycle state of a task |
| `TaskType` | Classification of task kinds |
| `TaskKey` | Unique identifier for tasks within a scheduling context |
| `Cost` | Cost representation for scheduling decisions |
| `ShadowPriceMap` | Dual values from LP relaxation for column generation |
| `SchedulingSolverValueAdapter` | Adapter bridging domain values to solver representations |

## Dependencies

- [gantt-scheduling-infrastructure](../gantt-scheduling-infrastructure/) — time primitives

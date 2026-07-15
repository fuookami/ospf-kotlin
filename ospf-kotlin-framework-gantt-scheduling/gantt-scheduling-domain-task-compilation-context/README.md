# gantt-scheduling-domain-task-compilation-context

:us: English | :cn: [简体中文](README_ch.md)

Task-level MILP compilation for the gantt-scheduling framework. Translates the task domain model into decision variables, constraints, and objective terms for mixed-integer linear programming solvers.

## Key Types

| Type | Description |
|------|-------------|
| `Compilation` | Base compilation context for MILP model building |
| `TaskCompilation` | Compilation context for individual task decisions |
| `IterativeTaskCompilation` | Compilation context supporting iterative (column generation) workflows |
| `TaskAggregation` | Aggregation of task-level decision variables |
| `TaskTime` | Decision variable for task start/end time |
| `Switch` | Decision variable for executor task switches |
| `Makespan` | Objective term for minimizing total schedule length |
| `TaskSolution` | Solution extraction from compiled task variables |

## Limits (Constraints & Objectives)

| Limit | Description |
|-------|-------------|
| `ExecutorCompilationConstraint` | Ensures executor capacity is respected |
| `ExecutorCostMinimization` | Minimizes total executor cost |
| `ExecutorLeisureMinimization` | Minimizes executor idle time |
| `SwitchCostMinimization` | Minimizes cost of executor switches |
| `TaskCancelMinimization` | Minimizes number of cancelled tasks |
| `TaskCostMinimization` | Minimizes total task cost |
| `TaskCompilationConstraint` | Ensures task compilation consistency |
| `TaskConflictConstraint` | Prevents task time overlaps |
| `TaskTimeConflictConstraint` | Prevents time-level conflicts between tasks |
| `TaskStepConflictConstraint` | Prevents step-level conflicts between tasks |
| `TaskOnTimeMaximization` | Maximizes on-time task completions |
| `TaskNotOnTimeMinimization` | Minimizes late task completions |

## Dependencies

- [gantt-scheduling-infrastructure](../gantt-scheduling-infrastructure/) — time primitives
- [gantt-scheduling-domain-task-context](../gantt-scheduling-domain-task-context/) — task domain model

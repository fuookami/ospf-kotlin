# gantt-scheduling-domain-capacity-scheduling-context

:us: English | :cn: [简体中文](README_ch.md)

Time-slot-based capacity scheduling — models capacity columns, compiles scheduling constraints, and defines capacity-related limits for the master problem.

## Key Types

| Type | Description |
|------|-------------|
| `Capacity` | Capacity definition for a scheduling entity |
| `CapacityColumn` | Column representing a capacity allocation in a time slot |
| `CapacityColumnAggregation` | Aggregation of capacity columns |
| `CapacityCompilation` | Compiles capacity data into scheduling model |
| `CapacityOrderCompilation` | Compiles order-specific capacity constraints |
| `IterativeCapacityCompilation` | Iterative compilation for capacity refinement |
| `CapacitySchedulingSolution` | Solution output for capacity scheduling |
| `ProductionAction` | Action representing a production decision |
| `CapacitySolverValue` | Solver-level value for capacity variables |
| `CapacitySchedulingContext` | Domain context for capacity scheduling |
| `CapacitySchedulingAggregation` | Aggregation root for capacity scheduling results |
| `CapacityCostMinimization` | Limit: minimize total capacity cost |
| `ExecutorCapacityConstraint` | Limit: constrain executor capacity usage |
| `OrderConstraint` | Limit: enforce order fulfillment constraints |

## Dependencies

- `gantt-scheduling-infrastructure`
- `gantt-scheduling-domain-task-context`

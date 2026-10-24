# gantt-scheduling-domain-capacity-scheduling-context

:us: English | :cn: [简体中文](README_ch.md)

Time-slot-based capacity scheduling — models capacity columns, compiles scheduling constraints, and defines capacity-related limits for the master problem.

## Key Types

`IterativeCapacityCompilation` builds call-local first-match action/slot indices and
executor groups during assembly. Each symbol refresh builds the active column-variable
view once and buckets it by executor/slot, avoiding a full column-map scan for every
selection row. Mutable inputs are re-read on each refresh; no cross-call cache is kept.

Column variables still use the existing replacement lifecycle: `CombinationVariableItem`
identity is tied to its parent combination, and rectangular shape growth can change its
flattened coordinates. Therefore a new batch replaces that executor's variable parent
and rebinds all of its historical terms. This compatibility optimization does not claim
append-only variables. Bound copying and token removal/re-registration still visit the
old/new rectangular variable shapes; operation-time refresh still scans all historical
allocations, capacity refresh still rebuilds the action-slot grid, and the global cost
keeps its original flat term order while scanning all historical columns. Thus the
remaining work is at least proportional to the replaced variable shape and affected
history. Selection indexing alone is reduced from repeated
`O(executors × slots × columns)` scans to expected
`O(columns + allocation entries + executor-slot rows)`. Hashing a `CapacityColumn`
also visits its allocation map, so column-key hashing is not constant work.

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

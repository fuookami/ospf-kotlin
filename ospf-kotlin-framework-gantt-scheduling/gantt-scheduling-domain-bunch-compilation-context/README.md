# gantt-scheduling-domain-bunch-compilation-context

:us: English | :cn: [简体中文](README_ch.md)

Column-generation master problem for the gantt-scheduling framework. Handles bunch selection, cost computation, and coverage constraints in the set-partitioning formulation. Provides slot-based compilation and capacity pre-solving for efficient column generation.

## Key Types

`BunchCompilation.addColumns` builds task/executor membership buckets once per new
batch. Standard bunches use their existing task-key membership; subclasses retain
their overridden `contains` predicate. Stable bunch order, repeated task occurrences
and incremental variable groups are unchanged.

| Type | Description |
|------|-------------|
| `BunchCompilation` | Compilation context for bunch-level decisions |
| `BunchCompilationContext` | Shared context for bunch compilation operations |
| `BunchAggregation` | Aggregation of bunch-level decision variables |
| `BunchSchedulingSolution` | Solution extraction from compiled bunch variables |
| `SlotBasedBunch` | Bunch representation using discrete time slots |
| `SlotBasedBunchCompilation` | Compilation context for slot-based bunch formulation |
| `SlotBasedCapacityResult` | Capacity feasibility result from slot-based pre-solving |
| `TaskTime` | Task timing within a bunch compilation |
| `TaskReverse` | Reverse mapping from tasks to their bunch assignments |

## Services

| Service | Description |
|---------|-------------|
| `BunchSolutionAnalyzer` | Analyzes bunch-level solution quality and structure |
| `TaskSolutionAnalyzer` | Analyzes task-level outcomes within bunch solutions |
| `SlotBasedBunchCompilationContext` | Builds slot-based bunch compilation model |
| `SlotBasedCapacityPreSolver` | Pre-solves capacity feasibility before full compilation |

## Limits (Constraints & Objectives)

| Limit | Description |
|-------|-------------|
| `BunchCostMinimization` | Minimizes total cost of selected bunches |

## Dependencies

- [gantt-scheduling-infrastructure](../gantt-scheduling-infrastructure/) — time primitives
- [gantt-scheduling-domain-task-context](../gantt-scheduling-domain-task-context/) — task domain model

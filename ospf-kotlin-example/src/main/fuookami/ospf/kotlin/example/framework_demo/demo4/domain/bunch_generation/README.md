# Bunch Generation

:us: English | :cn: [简体中文](README_ch.md)

Bounded context responsible for generating feasible flight task bunches for each aircraft. It builds a route graph per aircraft, produces initial bunches, and uses shadow-price-driven pricing to discover new bunches with negative reduced cost during column generation iterations.

## Responsibilities

- Build a directed route graph for each aircraft encoding feasible task sequences.
- Generate initial flight task bunches from the route graph.
- Produce new bunches per iteration using shadow prices from the master problem.
- Evaluate feasibility of task sequences (connection time, rules, aircraft usability).
- Manage reversible flight task pairs for order-change operations.

## Key Classes

| Class | Description |
|---|---|
| `BunchGenerationContext` | Entry point managing aggregation, feasibility judger, and per-aircraft generators. |
| `Aggregation` | Holds per-aircraft route graphs, the `FlightTaskReverse` map, and initial bunches. |
| `Graph` | Directed graph of `Node` and `Edge` representing feasible task transitions. |
| `Node` | Sealed class: `RootNode`, `EndNode`, `TaskNode`. |
| `FlightTaskReverse` | Manages reversible task pairs for order-change operations. |
| `FlightTaskBunchGenerator` | Generates bunches for a single aircraft given shadow prices. |
| `InitialFlightTaskBunchGenerator` | Produces the initial set of feasible bunches. |
| `FlightTaskFeasibilityJudger` | Checks whether a task sequence is feasible. |
| `AggregationInitializer` | Initializes the aggregation from aircraft/task data. |

## Dependencies

- **task** — provides `FlightTask`, `Aircraft`, `FlightTaskBunch`, `ShadowPriceMap`.
- **rule** — provides `Lock`, `ConnectionTimeCalculator`, `MinimumDepartureTimeCalculator`, `CostCalculator`, `RuleChecker`.

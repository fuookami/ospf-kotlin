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
| `FlightTaskBunchGenerator` | Generates bunches for a single aircraft using the shared Network path-search kernel and Demo4-specific state transitions. |
| `InitialFlightTaskBunchGenerator` | Produces the initial set of feasible bunches. |
| `FlightTaskFeasibilityJudger` | Checks whether a task sequence is feasible. |
| `AggregationInitializer` | Initializes the aggregation from aircraft/task data. |

## Dependencies

- **task** — provides `FlightTask`, `Aircraft`, `FlightTaskBunch`, `ShadowPriceMap`.
- **rule** — provides `Lock`, `ConnectionTimeCalculator`, `MinimumDepartureTimeCalculator`, `CostCalculator`, `RuleChecker`.
- **Gantt network bridge** — adapts the Network path-search result into a scoped `ColumnGenerationBatch`.

## Search and completion

`Graph` remains the aviation-domain builder and publishes a stable-ID Network topology snapshot. Generic path traversal is delegated to `PathSearchContext`; the transition callback still applies Demo4 recovery, timing, cost, and shadow-price rules, while extraction validates total cost and creates `FlightTaskBunch` values.

`ColumnGenerationBatch` reports `Exhausted`, `BudgetLimit`, `Interrupted`, or `Heuristic` completion together with request/model/dual/branch/executor-slot scope. Exhaustion confirms scope completion only; the optional `noImprovingColumns` field is supplied solely from independent pricing evidence and is never inferred from empty output. The per-node label limit and rejected full-trace validation can prevent an exhaustion certificate; the maximum returned-column count only caps output and does not truncate search. Invalid total-cost terminals prevent a no-improvement certificate. The former dominance comparison is intentionally not used: it compared reduced cost, delay and aircraft changes but omitted arrival-time and visited-history state, so it could suppress a distinct feasible route. Candidate columns do not provide a proof of the minimum reduced cost or integer optimality.

Route graph BFS uses a FIFO deque for amortized constant-time queue operations. Candidate callbacks, backup-arrival enqueue order, node reuse and diagnostics retain the existing traversal semantics.

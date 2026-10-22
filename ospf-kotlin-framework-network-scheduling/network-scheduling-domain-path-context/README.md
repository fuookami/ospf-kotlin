# Network path context

[简体中文](README_ch.md)

Solver-independent resource-constrained path generation over `NetworkGraph<N, A>`.
VRPTW pricing and Demo4 flight-bunch generation use this same search kernel.

## Contract

- Construct a request-local `PathProblem<N, A, S>`. Business state `S` owns time,
  load, visit order, recovery and any other resources; no monotonicity is assumed.
- Supply start, transition and terminal contributions, local feasibility, an
  independent full-trace validator, and optional result ordering/filtering.
- Elementary paths have a finite node-visit domain. Non-elementary cyclic walks
  require `maximumSteps`; a DAG needs no additional step bound.
- Dominance is disabled by default. A supplied predicate must preserve future
  feasibility, costs and master coefficients before `dominanceIsExact` can be set.
  The kernel also checks visited-set inclusion and remaining step capacity.
- `PathSearchBudget` limits computation, not business feasibility. Cancellation
  and label pruning never provide an exhaustive-search certificate.
- `maximumResults` only limits returned paths. Search continues, so zero output
  is not evidence of infeasibility or absence of improving columns.

`PathSearchResult` exposes completion, label diagnostics and `trace(path)`.
Always inspect completion separately from `paths`. `Ret` failures represent
invalid requests or callback failures; budget exhaustion is a normal result.
Callbacks and state must not reuse mutable pricing data across requests.

## Integration

```kotlin
val result = PathSearchContext().search(
    problem = problem,
    budget = PathSearchBudget(maximumLabels = 100_000),
    maximumResults = 20
)
```

The kernel has no numeric or unit assumptions: retain domain quantities in `S`
and convert reduced costs only at the pricing adapter boundary. For Gantt bunch
conversion use the optional `gantt-scheduling-domain-network-context`, not a
dependency from this module to Gantt or an application solver.

`PathSearchContextTest` covers small-graph exhaustive oracles, non-monotone
resources, visit history, terminal contributions, validation and honest budgets.

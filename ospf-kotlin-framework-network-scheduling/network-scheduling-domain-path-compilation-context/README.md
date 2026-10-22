# Network Scheduling Path Compilation

[简体中文](README_ch.md)

This module builds a binary master over complete, indivisible path candidates. It is a finite-pool model component, not a path enumerator and not a proof of global optimality over an unbounded or implicitly generated network.

## Model contract

- Declare stable row IDs, a unit and optional lower/upper bounds for each row. Equal bounds create an equality row; an omitted column coefficient means zero.
- Each `PathColumn` carries a stable ID, its complete business payload, full objective cost and complete row coefficients. The cost must include every path-specific objective contribution (including start, terminal or non-additive costs).
- Rows and costs are normalized at the solver boundary through `NetworkSchedulingSolverValueAdapter`; `FltX` remains supported. Affine units are rejected because offset conversions do not preserve linear coefficients. Use a linear difference unit when appropriate.
- The context registers binary variables and minimizes path costs. Do not add another objective term involving a managed path variable: pricing uses the full cost supplied by `PathColumn`.
- Column IDs are unique within a context. Re-adding an identical ID and canonical definition is a no-op; conflicting payload, cost or coefficients fail.
- A context belongs to one `LinearMetaModel`. A registration or incremental mutation failure can leave partial model state; discard that context and model rather than retrying it.

## Pricing and lifecycle

`PathCompilationAggregation.refreshShadowPrices` accepts the duals from the caller's current solve and a caller-managed scope version. The core `MetaDualSolution` has no solve timestamp, so this module cannot prove that supplied duals came from the latest solve. Snapshot checks cover context revisions and observable model state only; callers must not rely on them to detect arbitrary external model mutations. Never reuse a dual object after a solve, model change, column change or change to the effective pricing domain. The scope version must change when branch decisions, variable fixings, effective graph, or other pricing restrictions change.

Every row that can affect a candidate must be declared in the problem. Refresh rejects unregistered model constraints and missing/non-finite row duals instead of treating them as zero. `reducedCost` applies the signed formula `fullCost - sum(rowCoefficient * rowDual)` to the same normalized full-column definition used for registration. Supply a valid current LP dual solution; the context does not create a continuous relaxation or establish an optimality certificate.

Use `addColumns` and `removeColumns` to keep managed row expressions and objective terms synchronized. Either operation invalidates prior dual snapshots. `extractSolution` checks binary values and declared row bounds, and reports the candidate IDs represented by the returned finite-pool solution.

## API

- `PathCompilationProblem` defines rows, initial candidates and cost unit.
- `PathCompilationContext` registers the initial master, adds/removes columns and extracts a final finite-pool solution.
- `PathCompilationAggregation` exposes normalized column compilation, dual refresh and reduced-cost evaluation for an external pricing loop.
- `PathCompilationRows` owns the row-bound constraints and their dual mapping.

Objective audit: path costs use the standard flattened-objective API. Core's read-only `objectiveSnapshot()` exposes the same effective linear objectives used by the backend. Dual refresh, reduced-cost evaluation and extraction verify managed column coefficients against complete-column costs. Mixed flattened/symbolic objectives and altered managed costs are conservatively rejected rather than silently mispriced. Unrelated objective terms are not included in the reported path-only cost.

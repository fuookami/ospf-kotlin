# Network / Gantt Migration

[简体中文](MIGRATION_ch.md)

1. Replace `NetworkGraph<V>` with `NetworkGraph<N, A>` and explicit `NetworkGraphAccess`. Built-in `NetworkNode<V>` / `NetworkArc<V>` factories remain. Parallel arcs need unique `NetworkArcId`; endpoint pairs are not service identities. Views preserve IDs; use `graph.from/to`, not payload endpoints, for reversed views.
2. `FlowGraph` and its stateful registration kernel are removed. Construct immutable `FlowProblem` plus a fresh `FlowContext` per model. Old conservation/capacity/objective pipelines are now small expression-based components used by `FlowAggregation`, not independent variable registries. Integer steps scale both conservation and cost; mixed units require explicit capacity usage.
3. Resource-path policies use `PathProblem`: business state, transitions and terminal validation remain with the caller. Search budgets are not business path bounds. Inspect completion and diagnostics even for an empty result. Output limits alone do not truncate search.
4. Demo4 retains Gantt orchestration and aviation state/cost/recovery rules while Network owns topology and label traversal. VRPTW remains an independent entry using the same path mechanism. Ordering of equally good columns is not an equivalence guarantee.
5. Column batches distinguish exhaustive, heuristic and budget/interruption outcomes. Evidence must match model/dual/branch/pricing scope. Returned candidate reduced costs do not constitute global lower bounds. Legacy generators are heuristic; exact pricing does not prove global integer optimality.
6. Design levels declare total capacity within the finite arc envelope. Scenario upper bounds cap built capacity absolutely. Facility processing capacity uses explicit shared rows. Failure-node demand remains unless explicitly overridden. Resilience claims cover only the supplied scenario set.
7. Public domain values remain generic quantities. Solver reports retain `Flt64` by their existing boundary contract. Normal infeasibility/no-incumbent limits return structured outcomes; invalid definitions and technical errors return `Ret` failures.

No additional task/calendar/global scheduling framework is introduced. Use `ospf-kotlin-starter-gantt-network` only when the Gantt bridge is needed.

## Intentional Behavior Corrections

- VRPTW no longer permanently excludes a customer because one direct arc violates its time window: non-metric networks and parallel services can provide a feasible alternative. Monotone capacity exclusion remains.
- VRPTW and Demo4 recompute complete business costs at the terminal before reduced-cost filtering, ranking and validation. Non-additive VRPTW costs disable dominance unless the policy explicitly certifies the built-in fixed-plus-each-arc decomposition.
- Removing Phase II route columns also removes incremental objective terms, preventing deleted variables from surviving in the compiled objective.
- Gantt no longer treats the minimum reduced cost among returned candidates as a certified lower bound. Unknown bounds are nullable and an empty pricing round may stop without certification. The current heuristic orchestration does not certify global integer optimality.
- Commodity flows gated by facility or service activation require positive arc-capacity consumption. When a finite linking bound cannot be derived from zero consumption, registration fails rather than introducing an arbitrary big M.

## Validation Entries

Default tests need no native solver. Small paths use enumeration oracles; structural choices use small-domain enumeration and each scenario flow is independently checked. Native profiles are opt-in. A skipped test due to unavailable runtime is not a passed verification.

```powershell
mvn -f ospf-kotlin-framework-network-scheduling/pom.xml test -T 0.75C > "$env:TEMP\ospf-network-unit.log" 2>&1
mvn -pl :ospf-kotlin-framework-network-scheduling-application -am -Pscip-integration-tests test -T 0.75C > "$env:TEMP\ospf-network-scip.log" 2>&1
mvn -pl :ospf-kotlin-example -am -Pdemo4-only test -T 0.75C > "$env:TEMP\ospf-demo4-unit.log" 2>&1
mvn -pl :ospf-kotlin-example -am -Pdemo4-gurobi-cg test -T 0.75C > "$env:TEMP\ospf-demo4-gurobi.log" 2>&1
mvn -pl :ospf-kotlin-example -am -Pdemo5-scip-bp test -T 0.75C > "$env:TEMP\ospf-demo5-scip.log" 2>&1
```

The Network-parent reactor expects external base-module dependencies to be installed locally. Prefer root-reactor `-pl ... -am` commands on a fresh checkout. SCIP/JSCIP and Gurobi runtimes and licenses are environment-provided; default builds do not download or bundle native runtimes.

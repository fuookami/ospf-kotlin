# ospf-kotlin-framework-bpp1d

English | [简体中文](README_ch.md)

## Modules and Entry Points

The seven modules separate infrastructure, item definitions, pattern generation,
pattern assignment, discrete loading, packing, and application orchestration.

- `Bpp1dMilp` solves an integer pattern-assignment model.
- `Bpp1dColumnGeneration` registers FFD/BFD seeds, solves the restricted LP, refreshes
  dual prices, adds negative-reduced-cost columns, and solves a fresh final MILP over
  the generated pool. Bounded enumeration provides pricing candidates, not an
  unrestricted branch-and-price optimality certificate.
- `TailBinOptimizer` repacks a selected tail while preserving prefix inventory. Global
  business rules belong in its full-result acceptance policy, not a local extension.
- `Bpp1dCsvDatasetLoader` reads closed item/bin CSV schemas; numeric parsing and units
  are explicit. Empty inventory means unlimited stock, not zero stock.
- `PackingContext` independently checks exact demand, stock, length, spacing and weight;
  it emits physical placements, free tail intervals and material totals.
- `PackingRendererAdapter` and `PackingResult.kpi(unit)` produce rendering and metrics.
- `Bpp1dDiscreteLoading.context` adapts explicit longitudinal intervals to the
  [shared discrete kernel](../ospf-kotlin-framework-bpp/README.md).

## Domain Contract

`Item<V>`, `BinType<V>`, `Bin<V>`, `Placement<V>`, `PackingPattern<V>` and
`PackingPlan<V>` use `V : FloatingNumber<V>`. Length, coordinate, spacing, weight and
remnant fields use `Quantity<V>`; demands and stock use `UInt64`.

There is inter-piece spacing but no trailing spacing. Every pattern has one bin type
and exact per-item counts. Multiple bin types, finite stock, zero-weight items and
exact demands are supported; items are never merged based on dimensions alone.

`Flt64PackingValueAdapter` and `FltXPackingValueAdapter` normalize values only at solver
registration/extraction boundaries. Quantity dimensions are checked before modeling.

## Extensions and Certificates

`PatternPolicy` controls candidate acceptance. `PatternModelingExtension` provides
`register`, column `refresh`, reduced-cost contribution and final result extraction.
The same registration context exposes `MILP`, `LP` and `FINAL_MILP` stages. Extensions
must keep model-local state keyed by context, since finalization uses a fresh model.
Removing columns refreshes expressions before removing their tokens.

With the built-in numeric adapters, unchanged historical base-expression terms are
retained and only new columns are appended. Each refresh still validates historical
pattern counts, variable bindings and costs because public maps have no mutation
notifications. Modified history or base expressions causes a rebuild; deletion always
rebuilds. Extension refresh hooks keep their original order and failure behavior, and
extension writes are detected before the next update. Arbitrary custom adapters retain
the original full-refresh phases and reads because conversions may have side effects.
Both paths construct rebuilt term lists once rather than repeatedly concatenating
growing immutable polynomials. This does not promise constant-time total addColumns:
validation, cache invalidation and generated coefficients still have their own cost.

`PatternAssignmentContext.reducedCosts(candidates)` shares a sparse dual-price index
within one read-only pricing batch. It preserves candidate order, dual-row accumulation
order and extension contributions. Callers and extensions must not modify the model
or dual maps during that call. The scalar `reducedCost` overloads remain available for
independent calls; `refreshShadowPrice` supplies the prices for each new batch.
The built-in solver uses the batch fast path only without modeling extensions;
existing extensions retain scalar pricing and its per-candidate state reads.

An `Optimal` result requires both a complete candidate set and an optimal final
integer solve. Candidate/node limits yield `candidateComplete=false`; an optimal
restricted pool is reported only as `Feasible`. LP convergence alone does not prove
integer optimality. Solver failures propagate through `Ret`/`Try`.

## Validation

Fixed-position loading also supports shared `PartitionColumnGenerationApplicationService`, using
complete configurations of explicitly declared partitions. This is distinct from ordinary
`Bpp1dColumnGeneration` and is not combined with Benders. Restricted integer-pool success is `Feasible`.

No default test requires a commercial solver license. From the repository root:

```powershell
mvn -B -ntp -pl ospf-kotlin-framework-bpp1d/bpp1d-application -am test '-Dtest=Bpp1dTest,DiscreteLoadingTest,LinearBendersAlgorithmTest' '-Dsurefire.failIfNoSpecifiedTests=false' -T 0.75C *> "$env:TEMP/ospf-bpp1d-tests.log"
```

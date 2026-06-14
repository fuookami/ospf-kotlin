# ospf-kotlin-framework-csp1d

[中文](README_ch.md)

`ospf-kotlin-framework-csp1d` is a reusable one-dimensional cutting stock framework. It keeps the reusable CSP1D kernel in this repository and leaves downstream request DTOs, formula languages, project runtime parameters, tenant context, heartbeat logic, and solver plugin selection to business adapters.

## Boundary

The current public model is limited to entities already expressed by `csp1d-domain-material-context`:

- `Product`, `ProductDemand`, `Production`
- `Costar`
- `Material`, `Machine`
- `CuttingPlanSlice`, `CuttingPlan`
- demand contribution, render DTOs, and the currently integrated yield, waste, and length assignment contexts

Downstream-specific concepts such as defects, segmentation, position constraints, `unitBatch`, material-level costar attributes, business DTO protocols, and formula languages are not modeled here until they first become general domain entities.

## Basic Use

Create a `Csp1dProblem<V>` directly or with the builder DSL:

```kotlin
val problem = csp1dProblem<Flt64> {
    products(products)
    material(material)
    demands(demands)
    configuration(
        Csp1dConfiguration(
            maxInitialPlans = 64,
            maxPricingPlans = 16,
            iterationLimit = 8
        )
    )
    solveConfig {
        columnGeneration(
            maxInitialPlans = 128,
            maxPricingPlans = 32,
            iterationLimit = 16
        )
        yieldConfig(
            YieldModelingConfig(
                underProductionPenalty = mapOf(
                    ProductDemandShadowPriceKey(productId = "p1", unitSymbol = "m") to Flt64(100.0)
                ),
                overProductionPenalty = mapOf(
                    ProductDemandShadowPriceKey(productId = "p1", unitSymbol = "m") to Flt64(10.0)
                ),
                overProductionUpperBound = mapOf(
                    ProductDemandShadowPriceKey(productId = "p1", unitSymbol = "m") to Flt64(50.0)
                )
            )
        )
        lengthConfig(lengthConfig)
        wasteConfig(wasteConfig)
        topKPlanLimit(10)
        allowPartialSolution(true)
    }
}
```

Use `Csp1dMilp` for a plain MILP path and `Csp1dColumnGeneration` for column generation:

```kotlin
val milpSolution = Csp1dMilp<Flt64>(solver).solve(problem)
val result = Csp1dColumnGeneration<Flt64>(solver).solveWithTrace(problem)
```

`solveConfig` can be passed directly to `solve(...)`, stored in `problem.solveConfig`, or supplied through constructor-level default configs. Explicit method arguments have the highest priority.

`Csp1dColumnGeneration` also accepts `yieldConfig`, `wasteConfig`, `lengthConfig`, and `warmStartPlanUsages` as constructor parameters. `warmStartPlanUsages` takes a `List<CuttingPlanUsage<V>>` where each element pairs a `CuttingPlan` with a `UInt64` usage amount; these are written as native initial assignment values into the final MILP model:

```kotlin
val solver = Csp1dColumnGeneration<Flt64>(
    solver = columnGenerationSolver,
    yieldConfig = yieldConfig,
    warmStartPlanUsages = listOf(
        CuttingPlanUsage(plan = cuttingPlan, amount = UInt64(3))
    )
)
```

## Modeling Extensions

`Csp1dSolveConfig<V>` exposes an `extensions` field that accepts a list of `Csp1dModelingExtension<V>`. Each extension carries a `Pipeline<LinearMetaModel<Flt64>>` and an `Csp1dExtensionMode` filter that determines which solve stages the pipeline applies to:

- `MILP` — plain MILP only
- `LP` — column-generation LP master only
- `FINAL_MILP` — column-generation final MILP only
- `ALL` — all stages (default)

Downstream business constraints (same unit length, same width, width difference, material compatibility, etc.) should be injected as extension pipelines rather than modifying framework core code.

Example — injecting a same-width constraint through the public solve entry:

```kotlin
val sameWidthPipeline: Pipeline<LinearMetaModel<Flt64>> = SameWidthConstraintPipeline(materialId = "m1")
val extension = Csp1dModelingExtension<Flt64>(
    pipeline = sameWidthPipeline,
    mode = Csp1dExtensionMode.ALL
)
val solveConfig = Csp1dSolveConfig<Flt64>(
    extensions = listOf(extension)
)
// Or via the builder DSL:
val config = csp1dSolveConfig<Flt64> {
    extension(Csp1dModelingExtension(sameWidthPipeline, Csp1dExtensionMode.ALL))
}
val solution = Csp1dMilp<Flt64>(solver).solve(problem, solveConfig)
```

Extensions propagate through all solve paths: plain MILP, column-generation LP master and final MILP, and recovery/partial fallback MILP. The default empty `extensions` list preserves backward compatibility.

`Csp1dColumnGeneration` keeps one LP master model during the pricing loop and calls `Csp1dProduceContext.addColumns` in place after each accepted pricing batch. `ProduceAggregation.addColumns()` creates `x_$iteration` variable groups and `batch_$iteration` intermediate symbol groups, flushes intermediate symbols via `flush+asMutable` so constraints automatically include new column coefficients, and appends objective terms. Built-in constraint pipelines reference intermediate symbols instead of x variables directly; extension pipelines that also need incremental refresh can implement `Csp1dIncrementalPipeline`. The returned plan list is chained through incremental pipelines and becomes the confirmed plan set appended to the column-generation pool.

## Extension Policies

`Csp1dSolveConfig<V>` exposes an `extensionSet` field of type `Csp1dExtensionSet<V>`, which aggregates six policy categories beyond modeling extensions:

- **Domain policies** (`Csp1dDomainPolicy<V>`): width feasibility overrides, candidate acceptance.
- **Objective policies** (`Csp1dObjectivePolicy<V>`): modify batch coefficients in the objective function.
- **Generation strategies** (`Csp1dGenerationStrategy<V>`): candidate acceptance, custom canonical keys, and dominance acceptance.
- **Pricing policies** (`Csp1dPricingPolicy<V>`): modify reduced cost, modify benefit, and custom `isImproving` judges.
- **Flow policies** (`Csp1dFlowPolicy<V>`): control the CG main loop flow — filter initial plans, determine plan equivalence, trigger early iteration stop, customize termination reason/message, accept partial solutions when final MILP fails, and allow recovery fallback.
- **Extraction policies** (`Csp1dExtractionPolicy<V>`): enrich solution output by writing custom entries into KPI details and render KPI maps without modifying the solution model.

All policies provide default implementations that preserve existing behavior. Empty policy lists maintain full backward compatibility.

Flow policies receive a `Csp1dFlowContext<V>` exposing iteration number, current plans, new plans, iteration limit, LP result status, pricing statistics, and `allowPartialSolution`. This allows context-aware decisions without closure capture.

Extraction policies receive `Produce<V>`, demands, materials, machines, generated plans, iteration count, termination reason, final MILP status, and pricing statistics. They can write to mutable `details` and `renderKpi` maps. Exceptions thrown by extraction policies are caught and logged, preventing escape into the solve path.

## Shadow Price Lifecycle

LP shadow price extraction is managed through `Csp1dShadowPriceLifecycle<V>`, which uses the CGPipeline refresh / extractor mechanism.

The built-in non-length constraint pipelines — `DemandConstraintPipeline`, `MaterialConstraintPipeline`, `MachineConstraintPipeline`, and `YieldConstraintPipeline` — implement `CGPipeline<Args, Model, Map>` directly. During model construction, each shadow-price-aware constraint is registered with `constraint.args = Csp1dShadowPriceKey` (e.g., `ProductDemandShadowPriceKey`, `MaterialUsageShadowPriceKey`, `MachineBatchShadowPriceKey`, `MachineCapacityShadowPriceKey`, `YieldOverProductionBoundShadowPriceKey`). After LP solve, `Csp1dShadowPriceLifecycle` calls each CGPipeline's `refresh` method, which uses `model.constraintsOfGroup(this)` to find the pipeline's constraints and extracts dual values via `constraint.args` — no constraint-name lookup needed.

Each CGPipeline also implements `extractor()`, which computes the reduced cost contribution for a given cutting plan by looking up shadow prices from `AbstractCsp1dShadowPriceMap`. Yield over-production bounds participate in the same refresh lifecycle but return an explicit zero reduced-cost contribution, because the bound constrains aggregate yield slack rather than an individual cutting plan benefit. Length-assignment remains the plain `Pipeline` exception and does not register length shadow price keys.

The lifecycle populates both the framework-compatible `AbstractCsp1dShadowPriceMap` (for downstream CGPipeline consumers) and the lightweight `ShadowPriceMap<V>` (for pricing). LP dual values are converted through the explicit domain-value converter instead of direct casts. Plain extension pipelines that need pricing shadow prices should be modeled as CGPipeline-based extensions.

## Outputs

`Csp1dSolution<V>` contains:

- `produce`: selected cutting plans, material usages, machine capacity usages, and unmet demands
- `generatedPlans`: final plan pool used by MILP
- `kpi`: selected plan count, batch count, satisfied/unmet demands, usage counts, generated plan count, Top-K count, enhancement metric counts, and serializable KPI details
- `render`: stable DTO boundary for UI or serialization
- `status`: `Feasible`, `Partial`, `NoInitialPlans`, or `Failed`
- `failureMessage`: failure reason when available; LP and MILP failure messages are merged with semicolons
- `topPlans`: optional Top-K plan list

`Csp1dColumnGenerationTrace` records termination reason (`PricingConverged`, `IterationLimitReached`, `LpInfeasible`, `LpSolveFailed`, `AllDuplicates`, `NoInitialPlans`), LP iteration objectives, priced plan counts, initial and pricing generation statistics, final MILP status, partial solution availability, failure message, and LP failure detail message.

`LpInfeasible` indicates the first LP solve failed (likely LP relaxation infeasible); `LpSolveFailed` indicates LP solve failed after a prior valid LP result. Both are inferred from LP null returns, not from solver-layer infeasibility determinations.

`Failed` status is returned only when `allowPartialSolution=false` and solve fails; when `allowPartialSolution=true` (default), `Partial` is returned. Neither throws an exception.

Stable KPI field names are exposed by `Csp1dKpiKeys`. Stable scalar keys include plan counts, demand counts, usage counts, `solutionStatus`, `finalMilpStatus`, `partialSolutionAvailable`, `lpFailureMessage`, column-generation iteration/pricing keys, initial-generation statistics keys, and pricing-generation statistics keys. Dynamic detail keys should be built through helper methods such as `materialUsageBatchCount(...)`, `machineCapacityUsed(...)`, `underProduction(...)`, `overProduction(...)`, `materialCost(...)`, `assignedLength(...)`, and `overLength(...)`.

## Generation Semantics

The generation context provides DFS, N-Same, N-Sum, FullSum, and reduced-cost pricing generators. Generators share timeout, max plan, canonical deduplication, feasibility filtering, statistics reporting, width/length-bound pruning statistics, and `GenerationConstraints.parallelism` for material-level coroutine parallel generation.

DFS, N-Sum, and FullSum deduplicate demand width entries by product, width, width unit, and demand unit, then reuse a suffix minimum-width index to skip search branches whose remaining width cannot fit any later product width. The pruned node count is reported as `widthBoundPrunedNodes` and exposed through `Csp1dKpiKeys.InitialGenerationWidthBoundPrunedNodes` / `Csp1dKpiKeys.InitialWidthBoundPrunedNodes`.

When `GenerationConstraints.maxOverProduceLength` is configured, DFS, N-Sum, N-Same, and FullSum filter over-length product entries before combination search or single-product enumeration. The pruned entry count is reported as `lengthBoundPrunedEntries` and exposed through `Csp1dKpiKeys.InitialGenerationLengthBoundPrunedEntries` / `Csp1dKpiKeys.InitialLengthBoundPrunedEntries`.

When `GenerationConstraints.minKnifeCount` is configured, DFS, N-Sum, and FullSum prune combination branches that cannot reach the minimum knife count even after filling the remaining width with the narrowest available later entry. The pruned node count is reported as `knifeBoundPrunedNodes` and exposed through `Csp1dKpiKeys.InitialGenerationKnifeBoundPrunedNodes` / `Csp1dKpiKeys.InitialKnifeBoundPrunedNodes`.

DFS, N-Sum, and FullSum also reuse the filtered product-width entry index for materials with equivalent width-range profiles. This only reuses the search entry index; generated plans are still built per material, so material identity and canonical output remain stable. Cache hits are reported as `materialWidthIndexCacheHits` and exposed through `Csp1dKpiKeys.InitialGenerationMaterialWidthIndexCacheHits` / `Csp1dKpiKeys.InitialMaterialWidthIndexCacheHits`.

On the sequential path and with the built-in generation constraints only, DFS, N-Sum, and FullSum can additionally reuse slice-combination templates across equivalent material width profiles. This cache stores only `CuttingPlanSlice` combinations, not `CuttingPlan` objects; each hit rebuilds plans and demand contributions for the current material, including material-dependent dynamic length contribution. Cache hits are reported as `materialSliceTemplateCacheHits` and exposed through `Csp1dKpiKeys.InitialGenerationMaterialSliceTemplateCacheHits` / `Csp1dKpiKeys.InitialMaterialSliceTemplateCacheHits`.

`GenerationConstraints.enableDominancePruning` enables opt-in same-contribution dominance pruning. For candidates with the same material, machine, capacity consumption, and demand contribution vector, the generator keeps the candidate with smaller remaining width and records the filtered count in `dominatedCandidates`. `GenerationConstraints.dominanceStrategy` selects `SameContribution` (default) or `CrossContribution`: the latter groups by product set and filters candidates where the contribution vector is a strict superset and remaining width is not larger. Cross-contribution dominated count is reported as `crossContributionDominated`.

Quantity caching avoids recomputing demand contribution amounts for repeated product-width entries within a single material. Hits and misses are reported as `quantityCacheHits` / `quantityCacheMisses` and exposed through `Csp1dKpiKeys.InitialGenerationQuantityCacheHits` / `Csp1dKpiKeys.InitialGenerationQuantityCacheMisses`.

Slice-template cache misses are reported as `materialSliceTemplateCacheMisses` and exposed through `Csp1dKpiKeys.InitialGenerationMaterialSliceTemplateCacheMisses`.

Under `parallelism > 1`, cross-worker duplicate candidates (candidates generated independently by parallel workers that represent the same canonical plan) are merged and counted as `crossWorkerDuplicateCandidates`, exposed through `Csp1dKpiKeys.InitialGenerationCrossWorkerDuplicateCandidates`.

The medium-scale, mixed-unit, and expanded-scale baseline tests now cover DFS, N-Sum, N-Same, and FullSum with the same statistics contract: visited nodes, generated candidates, accepted plans, infeasible candidates, duplicate candidates, dominated candidates, width-bound pruned nodes, knife-bound pruned nodes, length-bound pruned entries, material width-index cache hits, material slice-template cache hits, quantity cache hits, quantity cache misses, material slice-template cache misses, cross-worker duplicate candidates, cross-contribution dominated, elapsed milliseconds, and stop reason. `CuttingPlanGenerationBenchmarkSnapshot` keeps deterministic count fields and renders `toStableLine()` for comparable benchmark snapshots; elapsed time remains in raw statistics for trend observation.

`Costar` is a filler for remaining width. It can appear in slices and render output, but it does not create demand contribution.

For dynamic-length products, generation-stage demand contribution uses the product length when fixed, then falls back to material length when available. Final roll length assignment is still owned by the length assignment MILP context.

## Recovery

`Csp1dRecovery` keeps the simple `solve(problem, solveConfig)` API and also provides `solveWithTrace(Csp1dRecoveryInput<V>)`. `Csp1dColumnGenerationRecovery` provides the same recovery input, trace, fallback, and adapter contract for column generation. The trace records recovery status, warm start status, attempt count, warm-start plan count, applied plan count, applied usage count, and message. Empty warm starts are marked as `Ignored`; compatible warm starts use the configured `Csp1dWarmStartAdapter`; incompatible warm starts are marked as `Invalid`.

The default adapter is `AdapterUnsupported` and falls back to normal solving when `retryWithoutWarmStart` is enabled. `Csp1dWarmStartPlanPoolAdapter` can be passed explicitly to apply compatible warm-start cutting plans as the initial plan pool, optionally appending the normal fallback generator. When a `previousSolution` is provided, recovery extracts the plans still compatible with the current problem, matches selected plan usages by canonical plan key, and writes those usages into the MILP model as native initial assignment values. Plain `Csp1dMilp`, `Csp1dRecovery`, the final MILP stage of `Csp1dColumnGeneration`, and `Csp1dColumnGenerationRecovery` consume these native initial assignment values when compatible usages are available. Application acceptance coverage includes column-generation recovery from a previous solution, using a recovered solution as the next previous solution, and preserving partial-solution semantics when the final MILP fails. The Gurobi profile includes real-solver smoke tests for direct MILP native initial solution, column-generation final MILP native initial solution, recovery previous-solution warm start, column-generation recovery previous-solution warm start, column-generation recovery next-round previous solution, changed-problem compatible-subset filtering, and machine-capacity yield recovery warm start. When `retryWithoutWarmStart` is disabled, recovery throws `Csp1dRecoveryFallbackDisabledException` with trace. Solver failures are wrapped by `Csp1dRecoverySolveException` with trace.

## Demo

The framework demo is:

`ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3`

The demo uses `Csp1dProblem<Flt64>`, framework generators, `ReducedCostPricingGenerator`, and `Csp1dColumnGeneration`. It no longer keeps a local hand-written RMP/SP model.

## Local Validation

Focused CSP1D tests:

```powershell
mvn -B -ntp "-Dkotlin.compiler.execution.strategy=in-process" "-Dkotlin.daemon.enabled=false" -pl .\ospf-kotlin-framework-csp1d\csp1d-domain-material-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-cutting-plan-generation-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-length-assignment-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-yield-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-wasting-minimization-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-produce-context,.\ospf-kotlin-framework-csp1d\csp1d-application "-Dtest=ProductDemandModelTest,DFSGeneratorTest,NSumGeneratorTest,NSameGeneratorTest,FullSumGeneratorTest,CostarFillerTest,CuttingPlanCanonicalKeyTest,ReducedCostPricingGeneratorTest,GeneratorParallelismTest,GeneratorMediumScaleBaselineTest,Csp1dApplicationAcceptanceTest,Csp1dCgLifecycleTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Gurobi profile compile gate:

```powershell
mvn -B -ntp "-Dkotlin.compiler.execution.strategy=in-process" "-Dkotlin.daemon.enabled=false" -pl .\ospf-kotlin-framework-csp1d\csp1d-domain-material-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-cutting-plan-generation-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-length-assignment-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-yield-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-wasting-minimization-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-produce-context,.\ospf-kotlin-framework-csp1d\csp1d-application -Pgurobi-cg-test -DskipTests test-compile
```

Gurobi native warm-start smoke:

```powershell
mvn -B -ntp "-Dkotlin.compiler.execution.strategy=in-process" "-Dkotlin.daemon.enabled=false" -pl .\ospf-kotlin-framework-csp1d\csp1d-domain-material-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-cutting-plan-generation-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-length-assignment-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-yield-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-wasting-minimization-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-produce-context,.\ospf-kotlin-framework-csp1d\csp1d-application -Pgurobi-cg-test "-Dcsp1d.gurobi.cg.test.enabled=true" "-Dtest=Csp1dColumnGenerationRealSolverTest#milpWarmStartInitialSolutionWorksOnRealSolver+columnGenerationFinalMilpWarmStartWorksOnRealSolver+recoveryPreviousSolutionWarmStartWorksOnRealSolver+columnGenerationRecoveryPreviousSolutionWarmStartWorksOnRealSolver+columnGenerationRecoveryResultCanBeReusedAsPreviousSolutionOnRealSolver+recoveryPreviousSolutionWarmStartFiltersChangedProblemOnRealSolver+recoveryWarmStartWithMachineCapacityAndYieldWorksOnRealSolver" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Demo3 compile gate:

```powershell
mvn -B -ntp "-Dkotlin.compiler.execution.strategy=in-process" "-Dkotlin.daemon.enabled=false" -pl .\ospf-kotlin-framework-csp1d\csp1d-infrastructure,.\ospf-kotlin-framework-csp1d\csp1d-domain-material-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-cutting-plan-generation-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-length-assignment-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-yield-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-wasting-minimization-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-produce-context,.\ospf-kotlin-framework-csp1d\csp1d-application,.\ospf-kotlin-starters\ospf-kotlin-starter-csp1d,.\ospf-kotlin-example "-Dexample.source.directory=src/main/fuookami/ospf/kotlin/example/framework_demo/demo3" -DskipTests compile
```

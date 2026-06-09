# ospf-kotlin-framework-csp1d

[中文](README_ch.md)

`ospf-kotlin-framework-csp1d` is a reusable one-dimensional cutting stock framework. It keeps the generic CSP1D kernel in this repository and leaves downstream request DTOs, formula languages, project runtime parameters, tenant context, heartbeat logic, and solver plugin selection to business adapters.

## Boundary

The current public model is limited to entities already expressed by `csp1d-domain-material-context`:

- `Product`, `ProductDemand`, `Production`
- `Costar`
- `Material`, `Machine`
- `CuttingPlanSlice`, `CuttingPlan`
- demand contribution, render DTOs, and the currently integrated yield, waste, and length assignment contexts

POIT-specific concepts such as defects, segmentation, position constraints, `unitBatch`, material-level costar attributes, business DTO protocols, and formula languages are not modeled here until they first become generic domain entities.

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

## Outputs

`Csp1dSolution<V>` contains:

- `produce`: selected cutting plans, material usages, machine capacity usages, and unmet demands
- `generatedPlans`: final plan pool used by MILP
- `kpi`: selected plan count, batch count, satisfied/unmet demands, usage counts, generated plan count, Top-K count, enhancement metric counts, and serializable KPI details
- `render`: stable DTO boundary for UI or serialization
- `status`: `Feasible`, `Partial`, `NoInitialPlans`, or `Failed`
- `failureMessage`: failure reason when available
- `topPlans`: optional Top-K plan list

`Csp1dColumnGenerationTrace` records termination reason, LP iteration objectives, priced plan counts, initial generation statistics, final MILP status, partial solution availability, and failure message.

Stable KPI field names are exposed by `Csp1dKpiKeys`. Stable scalar keys include plan counts, demand counts, usage counts, `solutionStatus`, `finalMilpStatus`, `partialSolutionAvailable`, column-generation iteration/pricing keys, and initial-generation statistics keys. Dynamic detail keys should be built through helper methods such as `materialUsageBatchCount(...)`, `machineCapacityUsed(...)`, `underProduction(...)`, `overProduction(...)`, `materialCost(...)`, `assignedLength(...)`, and `overLength(...)`.

## Generation Semantics

The generation context provides DFS, N-Same, N-Sum, FullSum, and reduced-cost pricing generators. Generators share timeout, max plan, canonical deduplication, feasibility filtering, statistics reporting, width-bound pruning statistics, and `GenerationConstraints.parallelism` for material-level coroutine parallel generation.

DFS, N-Sum, and FullSum deduplicate demand width entries by product, width, width unit, and demand unit, then reuse a suffix minimum-width index to skip search branches whose remaining width cannot fit any later product width. The pruned node count is reported as `widthBoundPrunedNodes` and exposed through `Csp1dKpiKeys.InitialGenerationWidthBoundPrunedNodes` / `Csp1dKpiKeys.InitialWidthBoundPrunedNodes`.

`GenerationConstraints.enableDominancePruning` enables opt-in same-contribution dominance pruning. For candidates with the same material, machine, capacity consumption, and demand contribution vector, the generator keeps the candidate with smaller remaining width and records the filtered count in `dominatedCandidates`.

The medium-scale and mixed-unit baseline tests now cover DFS, N-Sum, N-Same, and FullSum with the same statistics contract: visited nodes, generated candidates, accepted plans, infeasible candidates, duplicate candidates, dominated candidates, width-bound pruned nodes, elapsed milliseconds, and stop reason. `CuttingPlanGenerationBenchmarkSnapshot` keeps deterministic count fields and renders `toStableLine()` for comparable benchmark snapshots; elapsed time remains in raw statistics for trend observation.

`Costar` is a filler for remaining width. It can appear in slices and render output, but it does not create demand contribution.

For dynamic-length products, generation-stage demand contribution uses the product length when fixed, then falls back to material length when available. Final roll length assignment is still owned by the length assignment MILP context.

## Recovery

`Csp1dRecovery` keeps the simple `solve(problem, solveConfig)` API and also provides `solveWithTrace(Csp1dRecoveryInput<V>)`. The trace records recovery status, warm start status, attempt count, warm-start plan count, applied plan count, applied usage count, and message. Empty warm starts are marked as `Ignored`; compatible warm starts use the configured `Csp1dWarmStartAdapter`; incompatible warm starts are marked as `Invalid`.

The default adapter is `AdapterUnsupported` and falls back to normal solving when `retryWithoutWarmStart` is enabled. `Csp1dWarmStartPlanPoolAdapter` can be passed explicitly to apply compatible warm-start cutting plans as the initial plan pool, optionally appending the normal fallback generator. When a compatible `previousSolution` is provided, selected plan usages are matched by canonical plan key and written into the MILP model as native initial assignment values. The Gurobi profile includes real-solver smoke tests for both direct MILP native initial solution and recovery previous-solution warm start paths. When `retryWithoutWarmStart` is disabled, recovery throws `Csp1dRecoveryFallbackDisabledException` with trace. Solver failures are wrapped by `Csp1dRecoverySolveException` with trace.

## Demo

The framework demo is:

`ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3`

The demo uses `Csp1dProblem<Flt64>`, framework generators, `ReducedCostPricingGenerator`, and `Csp1dColumnGeneration`. It no longer keeps a local hand-written RMP/SP model.

## Local Validation

Focused CSP1D tests:

```powershell
mvn -B -ntp "-Dkotlin.compiler.execution.strategy=in-process" "-Dkotlin.daemon.enabled=false" -pl .\ospf-kotlin-framework-csp1d\csp1d-domain-material-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-cutting-plan-generation-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-length-assignment-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-yield-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-wasting-minimization-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-produce-context,.\ospf-kotlin-framework-csp1d\csp1d-application "-Dtest=ProductDemandModelTest,DFSGeneratorTest,NSumGeneratorTest,NSameGeneratorTest,FullSumGeneratorTest,CostarFillerTest,CuttingPlanCanonicalKeyTest,ReducedCostPricingGeneratorTest,GeneratorParallelismTest,GeneratorMediumScaleBaselineTest,Csp1dApplicationAcceptanceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Gurobi profile compile gate:

```powershell
mvn -B -ntp "-Dkotlin.compiler.execution.strategy=in-process" "-Dkotlin.daemon.enabled=false" -pl .\ospf-kotlin-framework-csp1d\csp1d-domain-material-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-cutting-plan-generation-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-length-assignment-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-yield-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-wasting-minimization-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-produce-context,.\ospf-kotlin-framework-csp1d\csp1d-application -Pgurobi-cg-test -DskipTests test-compile
```

Gurobi native warm-start smoke:

```powershell
mvn -B -ntp "-Dkotlin.compiler.execution.strategy=in-process" "-Dkotlin.daemon.enabled=false" -pl .\ospf-kotlin-framework-csp1d\csp1d-domain-material-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-cutting-plan-generation-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-length-assignment-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-yield-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-wasting-minimization-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-produce-context,.\ospf-kotlin-framework-csp1d\csp1d-application -Pgurobi-cg-test "-Dcsp1d.gurobi.cg.test.enabled=true" "-Dtest=Csp1dColumnGenerationRealSolverTest#milpWarmStartInitialSolutionWorksOnRealSolver+recoveryPreviousSolutionWarmStartWorksOnRealSolver" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Demo3 compile gate:

```powershell
mvn -B -ntp "-Dkotlin.compiler.execution.strategy=in-process" "-Dkotlin.daemon.enabled=false" -pl .\ospf-kotlin-framework-csp1d\csp1d-infrastructure,.\ospf-kotlin-framework-csp1d\csp1d-domain-material-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-cutting-plan-generation-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-length-assignment-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-yield-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-wasting-minimization-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-produce-context,.\ospf-kotlin-framework-csp1d\csp1d-application,.\ospf-kotlin-starters\ospf-kotlin-starter-csp1d,.\ospf-kotlin-example "-Dexample.source.directory=src/main/fuookami/ospf/kotlin/example/framework_demo/demo3" -DskipTests compile
```

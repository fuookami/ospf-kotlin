# ospf-kotlin-core

[中文文档 (README_ch.md)](./README_ch.md)

`ospf-kotlin-core` is the core modeling and solving module of OSPF Kotlin.
It provides:

1. Meta/Mechanism/Intermediate layered model pipeline for LP and QP.
2. Unified solver entry based on `SolveOptions`.
3. Unified model-building status callback model.
4. Unified solver output model with common runtime statistics.

## Unified Solve Entry

You can use the new extension entrypoints from `backend/solver/SolverExt.kt`:

```kotlin
import fuookami.ospf.kotlin.core.backend.solver.SolveOptions
import fuookami.ospf.kotlin.core.backend.solver.solveWithOptions

suspend fun solveWithUnifiedEntry(
    solver: AbstractLinearSolver,
    model: LinearMetaModel
) {
    val result = solver.solveWithOptions(
        model = model,
        options = SolveOptions.build {
            solutionAmount = UInt64.one
            modelBuildingStatusCallBack = { status ->
                println("${status.modelName} ${status.stage} ${status.ready}/${status.total}")
                ok
            }
            solvingStatusCallBack = { status ->
                println("obj=${status.obj}, gap=${status.gap}")
                ok
            }
        }
    )
}
```

## Compatibility Notes

Legacy overloads like `invoke(...)` and `solveAsync(...)` are still available.
You can migrate incrementally:

1. Keep old API unchanged for existing call sites.
2. Move new call sites to `solve(...)` / `solveWithOptions(...)`.
3. Replace old status callbacks with `ModelBuildingStatusCallBack` in batches.

## Solver Output

`FeasibleSolverOutput` keeps existing fields and now includes optional/common fields:

1. `iterations`
2. `nodeCount`
3. `bestBound`
4. `mipGap`
5. `solveTime`

This allows LP/QP/MIP adapters to expose a consistent output shape while preserving backward compatibility.

## Scope Relation to Root README

Root README focuses on repository-level navigation.
This module README focuses on:

1. Core architecture.
2. Unified entry migration path.
3. Core-specific callback and output contracts.


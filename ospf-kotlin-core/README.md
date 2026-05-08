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

## Genericization Migration Guide

The core module has been migrated from `Flt64`-specific APIs to generic `V : RealNumber<V>, NumberField<V>` typed APIs. This section documents the migration path for downstream code.

### Removed Typealiases

The following `Flt64` convenience typealiases have been removed. Use the V-typed equivalent instead:

| Removed alias | Replacement |
|---|---|
| `CallBackModelInterface` | `CallBackModelInterfaceV<Flt64>` (or your V type) |
| `MultiObjectiveModelInterface` | `MultiObjectiveModelInterfaceV<Flt64>` (or your V type) |

### Internalized Methods

The following methods were previously public but are now `internal` (module-private):

- `LinearInequality<Flt64>.sign`, `QuadraticInequality.sign`
- `LinearInequality<Flt64>.flattenData`, `QuadraticInequality.flattenData`
- `LinearPolynomial<Flt64>.toFlattenData()`, `QuadraticPolynomial<Flt64>.toFlattenData()`
- `LinearRelation.toConstraint()`, `QuadraticRelation.toConstraint()`, `LinearRelation.toQuadraticConstraint()`
- `LinearPolynomial<Flt64>.toFrontendPolynomial()` (deleted — was identity function)

### SolverBoundaryCasts

All `@Suppress("UNCHECKED_CAST")` annotations are now centralized in `SolverBoundaryCasts.kt`. If you previously used type-erased bridge methods like `registerAuxiliaryTokensAny` or `registerConstraintsAny`, use the V-typed methods directly instead. The framework internally delegates to `SolverBoundaryCasts` for star-projected generic calls.

### Legacy Typealiases (Still Available)

The following `Flt64` typealiases in `token/TokenList.kt` remain as legacy convenience aliases:

- `AbstractTokenList` → `AbstractTokenList<Flt64>`
- `TokenList` → `TokenList<Flt64>`
- `AddableTokenCollection` → `AddableTokenCollection<Flt64>`
- `AbstractMutableTokenList` → `AbstractMutableTokenList<Flt64>`
- `MutableTokenList` → `MutableTokenList<Flt64>`
- `AutoTokenList` → `AutoTokenList<Flt64>`
- `ManualTokenList` → `ManualTokenList<Flt64>`

New code should prefer the generic form directly.

### adapter/flt64 Compatibility Layer

The `math.symbol.adapter.flt64` package provides Flt64-specialized convenience functions and a `QuadraticInequality` typealias. This layer is a **compatibility boundary** — it is not part of the main-chain V-typed API.

**Deprecation policy:**

- `QuadraticInequality` is now `@Deprecated(WARNING)`. Use `QuadraticInequalityOf<Flt64>` directly.
- The remaining adapter functions (evaluate, parse, serde, DSL, etc.) are retained for backward compatibility but new code should prefer the generic V-typed equivalents where available.
- This adapter layer will be removed in a future major version. No timeline is committed yet, but the deprecation signals the intent.

### Scan Gate Enforcement (I5)

The genericization scan gate (`scripts/scan-full-genericization.ps1`) enforces:

1. `public_api_blocking = 0` for all check items (no Flt64 in non-adapter public API signatures).
2. `UNCHECKED_CAST` blocking = 0 (all casts centralized in `SolverBoundaryCasts.kt`).
3. Non-adapter public API signature Flt64 = 0 (I5 signature-level check, catches nested generics and multi-line signatures that regex-only scans miss).
4. Boundary items classified into three tiers: **permanent** (solver-inherent), **deprecated** (planned removal), **must_decrease** (tracked for reduction).

## Scope Relation to Root README

Root README focuses on repository-level navigation.
This module README focuses on:

1. Core architecture.
2. Unified entry migration path.
3. Core-specific callback and output contracts.


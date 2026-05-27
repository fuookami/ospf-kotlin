# ospf-kotlin-core

[中文文档 (README_ch.md)](./README_ch.md)

`ospf-kotlin-core` is the core modeling and solving module of OSPF Kotlin.
It provides:

1. Meta/Mechanism/Intermediate layered model pipeline for LP and QP.
2. Unified solver entry based on `SolveOptions`.
3. Unified model-building status callback model.
4. Unified solver output model with common runtime statistics.

## Module Structure

```
core/
├── error/                  - Error handling types (ErrorCode, ErrorKind)
├── model/
│   ├── basic/              - Basic model types (Constraint, Expression, Model, ModelView)
│   ├── callback/           - Model building status callbacks
│   ├── intermediate/       - Intermediate model layer (LinearTriadModel, QuadraticTetradModel)
│   └── mechanism/          - Mechanism model layer (MetaModel, constraint registration)
├── solver/
│   ├── config/             - Solver configuration (SolverConfig)
│   ├── heuristic/          - Heuristic solver implementations
│   ├── iis/                - Irreducible Infeasible Set analysis
│   ├── output/             - Solver output models (FeasibleSolverOutput)
│   └── value/              - Solver value types
├── symbol/
│   ├── flatten/            - Symbol flattening utilities
│   └── function/           - Function symbols (BigM, Binaryization, Piecewise, etc.)
├── token/                  - Token types for model building (TokenList, TokenTable)
└── variable/               - Variable types and collections (VariableItem, VariableRange)
```

## Unified Solve Entry

You can use the new extension entrypoints from `core/solver/SolverExt.kt`:

```kotlin
import fuookami.ospf.kotlin.core.solver.SolveOptions
import fuookami.ospf.kotlin.core.solver.solveWithOptions

suspend fun solveWithUnifiedEntry(
    solver: AbstractLinearSolver,
    model: LinearTriadModelView
) {
    val result = solver.solveWithOptions(
        model = model,
        options = SolveOptions.build {
            solutionAmount = UInt64.one
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

The following `Flt64` convenience typealiases have been removed. Use the generic form directly instead:

| Removed alias | Replacement |
|---|---|
| `CallBackModelInterface` (Flt64 typealias) | `CallBackModelInterface<Flt64>` (or your V type) |
| `MultiObjectiveModelInterface` (Flt64 typealias) | `MultiObjectiveModelInterface<Flt64>` (or your V type) |

### Internalized Methods

The following methods were previously public but are now `internal` (module-private):

- `LinearInequality<Flt64>.sign`, `QuadraticInequalityOf<Flt64>.sign`
- `LinearInequality<Flt64>.flattenData`, `QuadraticInequalityOf<Flt64>.flattenData`
- `LinearPolynomial<Flt64>.toFlattenData()`, `QuadraticPolynomial<Flt64>.toFlattenData()`
- `LinearRelation.toConstraint()`, `QuadraticRelation.toConstraint()`, `LinearRelation.toQuadraticConstraint()`
- `LinearPolynomial<Flt64>.toFrontendPolynomial()` (deleted — was identity function)

### SolverBoundaryCasts

All `@Suppress("UNCHECKED_CAST")` annotations are now centralized in `SolverBoundaryCasts.kt`. If you previously used type-erased bridge methods like `registerAuxiliaryTokensStar` or `registerConstraintsLinearStar` / `registerConstraintsQuadraticStar`, use the V-typed methods directly instead. The framework internally delegates to `SolverBoundaryCasts` for star-projected generic calls.

### Token Types

The token types in `token/TokenList.kt` are now fully generic. Use them with an explicit type parameter:

- `AbstractTokenList<V>`
- `TokenList<V>`
- `AddableTokenCollection<V>`
- `AbstractMutableTokenList<V>`
- `MutableTokenList<V>`
- `AutoTokenList<V>`
- `ManualTokenList<V>`

New code should use the generic form directly (e.g. `TokenList<Flt64>`).

### QuadraticInequalityOf

The quadratic inequality type is now fully generic: `QuadraticInequalityOf<V>`. Use it directly with your V type parameter (e.g. `QuadraticInequalityOf<Flt64>`). No compatibility alias is provided.

## Package Naming Migration

`core.intermediate_symbol.*` has been renamed to `core.symbol.*`. Update your imports accordingly:

| Old import | New import |
|---|---|
| `fuookami.ospf.kotlin.core.intermediate_symbol.*` | `fuookami.ospf.kotlin.core.symbol.*` |
| `fuookami.ospf.kotlin.core.intermediate_symbol.function.*` | `fuookami.ospf.kotlin.core.symbol.function.*` |
| `fuookami.ospf.kotlin.core.intermediate_symbol.flatten.*` | `fuookami.ospf.kotlin.core.symbol.flatten.*` |

## Scope Relation to Root README

Root README focuses on repository-level navigation.
This module README focuses on:

1. Core architecture.
2. Unified entry migration path.
3. Core-specific callback and output contracts.


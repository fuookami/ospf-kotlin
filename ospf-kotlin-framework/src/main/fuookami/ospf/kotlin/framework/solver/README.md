# Solver Package

:us: English | :cn: [简体中文](README_ch.md)

Solver abstraction layer defining column generation, Benders decomposition, and combinatorial solver interfaces.

## Interface Hierarchy

```
ColumnGenerationSolver
  ├── solveMILP / solveMILPAsync        (MILP solving)
  ├── solveLP / solveLPAsync            (LP solving, returns dual solution)
  ├── solveMILPAs / solveMILPAsAsync    (MILP solving with value conversion)
  ├── solveLPAs / solveLPAsAsync        (LP solving with value conversion)
  └── LPResult / LPResultOf<V>         (LP result with dual solution)

LinearBendersDecompositionSolver
  ├── solveMaster / solveMasterAs       (Master problem solving)
  ├── solveSub / solveSubAs             (Sub problem solving)
  └── LinearSubResult (Feasible | Infeasible)

QuadraticBendersDecompositionSolver
  ├── solveMaster / solveMasterAs       (Quadratic master problem)
  ├── solveSub / solveSubAs             (Quadratic sub problem)
  └── QuadraticSubResult (Feasible | Infeasible)
```

## Combinatorial Solvers

| Class | Strategy | Result Selection |
| --- | --- | --- |
| `SerialCombinatorialLinearSolver` | Serial | First success |
| `ParallelCombinatorialLinearSolver` | Parallel | `First` or `Best` |
| `SerialCombinatorialQuadraticSolver` | Serial | First success |
| `ParallelCombinatorialQuadraticSolver` | Parallel | `First` or `Best` |
| `SerialCombinatorialColumnGenerationSolver` | Serial | First success |
| `ParallelCombinatorialColumnGenerationSolver` | Parallel | `First` or `Best` |

The `ParallelCombinatorialMode` enum controls the result selection strategy for parallel combinatorial solvers:
- `First` — Return the first successful result
- `Best` — Wait for all solvers to complete, return the result with the best objective value

## FrameworkSolveOptions

Unified solve options that consolidate scattered parameters from various shortcut solver entry points:

| Property | Description |
| --- | --- |
| `name` | Solve name |
| `toLogModel` | Whether to export model log |
| `solutionAmount` | Desired solution amount |
| `registrationStatusCallBack` | Registration status callback |
| `solvingStatusCallBack` | Solving status callback |
| `valueConversionPolicy` | Value conversion policy (defaults to `Strict`) |
| `bendersIterationLimit` | Benders iteration limit |
| `bendersStallIterationLimit` | Benders stall iteration limit |

Builder usage:

```kotlin
val options = FrameworkSolveOptions.build {
    name = "my-solve"
    toLogModel = true
    solutionAmount = UInt64(3)
}
```

## Type Alias Quick Reference

`FrameworkNumberAliases.kt` provides convenient type aliases for meta models, solver outputs, and solution pools across numeric types:

| Alias | Expansion |
| --- | --- |
| `Flt64LinearMetaModel` | `LinearMetaModel<Flt64>` |
| `FltXLinearMetaModel` | `LinearMetaModel<FltX>` |
| `Rtn64LinearMetaModel` | `LinearMetaModel<Rtn64>` |
| `RtnXLinearMetaModel` | `LinearMetaModel<RtnX>` |
| `Flt64QuadraticMetaModel` | `QuadraticMetaModel<Flt64>` |
| `FltXQuadraticMetaModel` | `QuadraticMetaModel<FltX>` |
| `Rtn64QuadraticMetaModel` | `QuadraticMetaModel<Rtn64>` |
| `RtnXQuadraticMetaModel` | `QuadraticMetaModel<RtnX>` |
| `FltXFeasibleSolverOutput` | `FeasibleSolverOutput<FltX>` |
| `Rtn64FeasibleSolverOutput` | `FeasibleSolverOutput<Rtn64>` |
| `RtnXFeasibleSolverOutput` | `FeasibleSolverOutput<RtnX>` |

`ColumnGenerationSolver.kt` additionally defines `Flt64`-specific aliases:

| Alias | Expansion |
| --- | --- |
| `Flt64LinearMetaModel` | `LinearMetaModel<Flt64>` |
| `Flt64FeasibleSolverOutput` | `FeasibleSolverOutput<Flt64>` |
| `Flt64SolutionPool` | `List<Solution<Flt64>>` |

## Remote Solver Architecture

The `solver.remote` sub-package implements time-sliced remote solving:

```
client/                     Client
  RemoteSolverClient        Round-by-round solving with checkpoint management
  RemoteSolverHttpClient    HTTP transport layer
  RemoteSolverRuntimeConfig Runtime config (tenant, node, time quantum, rounds)

domain/                     Domain models
  ValueTypes                Value types (TaskId, SliceId, NodeId, etc.)
  Errors                    Error codes and exceptions
  SerializedModels          Serialized models
  NormalizedModels          Normalized models
  StorageModels             Storage models
  TaskModels                Task models
  ExecutionModels           Execution models

port/                       Ports
  ObjectStoragePort         Object storage interface (put/get/delete/exists)
  SolverExecutionPort       Solver execution interface (start/resume/await/export/fetch/stop)

adapter/                    Adapters
  localfs/LocalFileObjectStoragePort     Local filesystem storage adapter
  ospf/OspfRemoteModelSerializer         OSPF serialization format adapter
```

## Async Coroutine Scope

`FrameworkAsync.kt` provides a shared `CoroutineScope` (`SupervisorJob` + `Dispatchers.Default`) for creating `CompletableFuture` instances in framework-level async solving.

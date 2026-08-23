# Solver Package

:us: English | :cn: [简体中文](README_ch.md)

Solver abstraction layer defining column generation, Benders decomposition, and combinatorial solver interfaces.

## Interface Hierarchy

```
ColumnGenerationSolver
  ├── solveMILP / solveMILPAsync        (MILP solving)
  ├── solveMILPWithStatus               (MILP solving with terminal status)
  ├── solveLP / solveLPAsync            (LP solving, returns dual solution)
  ├── solveLPWithStatus                 (LP solving with terminal status)
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

`solveMILPWithStatus` and `solveLPWithStatus` preserve an infeasible result as
`MILPSolveResult.Infeasible` or `LPResultWithStatus.Infeasible`. For a feasible
LP, callers must check `LPResult.status == SolverStatus.Optimal` before using
its duals as an exact pricing certificate.

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
| `FltXSolveReport` | `SolveReport<FltX>` |
| `Rtn64SolveReport` | `SolveReport<Rtn64>` |
| `RtnXSolveReport` | `SolveReport<RtnX>` |

`ColumnGenerationSolver.kt` additionally defines `Flt64`-specific aliases:

| Alias | Expansion |
| --- | --- |
| `Flt64LinearMetaModel` | `LinearMetaModel<Flt64>` |
| `Flt64SolveReport` | `SolveReport<Flt64>` |
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

### CP Benders and remote report contract

`LogicBasedBendersEngine` keeps master bindings, assumptions, cuts, proof status, and convergence evidence separate. `Exact` mode accepts only verified globally valid cuts and a verified master bound gap; a feasible subproblem without an optimality certificate remains `Feasible`, not `Optimal`.

`RemoteConstraintProgrammingClient.solveOutput()` sends a versioned CP snapshot and materializes `variableValuesById` and `intervalValues` from the result artifact. It validates domains, interval equations, every snapshot constraint, objective expressions, raw/resultRef consistency, fingerprints, and proof claims. CP integers use JSON `Long`; older DTOs remain readable but cannot upgrade an unverified result. `ConstraintProgrammingCheckpointCodec` and the remote checkpoint store use portable v2 envelopes with integrity digests and rebuild-from-snapshot semantics. Native search-state resume is unsupported.

### Identity, portability, and capability boundaries

Model element identity is explicit and has two scopes:

| Scope | Meaning | Cross-rebuild use |
| --- | --- | --- |
| `Stable` | The model entry point supplied `id`, `namespace`, `schemaVersion`, and optional `origin`. | Allowed for diagnostics, reports, remote DTOs, and portable checkpoints after fingerprint validation. |
| `ModelLocal` | The element has only a deterministic identity within the current model instance. | May be used for local diagnostics, but must not be advertised as a cross-rebuild binding. |

Older payloads without identity metadata remain readable as `ModelLocal`; they are never upgraded to `Stable` implicitly. A checkpoint is portable when it contains a verified snapshot and can rebuild the model. It is not a native search-state resume: vendor handles, transformed trees, JNI pointers, and solver-internal search state never cross the checkpoint boundary.

The same distinction applies to solver reuse. The default CP and MIP-backed paths rebuild a fresh backend model for every attempt. `reuse` or `reoptimization` is opt-in only when the backend descriptor exposes a tested capability; a solver name, warm start, or solution hint alone does not imply native reuse or exact resume. Capability publication requires the shared terminal-state, identity, provenance, cancellation, and resource-release tests described in `plans/solver_cp.md`.

Migration example: preserve the serialized identity fields when moving a model to the remote API, then validate the returned model/configuration/solver fingerprints before restoring a checkpoint. If any required field is absent or the scope is `ModelLocal`, keep the result local and rebuild instead of coercing it into a stable binding.

## Async Coroutine Scope

`FrameworkAsync.kt` provides a shared `CoroutineScope` (`SupervisorJob` + `Dispatchers.Default`) for creating `CompletableFuture` instances in framework-level async solving.

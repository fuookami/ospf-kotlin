# ospf-kotlin-core

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

ospf-kotlin-core is the **core module** of the OSPF (Open Solver Platform Framework) Kotlin project. It implements the complete mathematical optimization model lifecycle — from variable definition and symbol expression construction, through model building and flattening, to solver abstraction and result retrieval.

## Scope

This module covers:

1. Variable and token systems.
2. Symbolic expression and function-symbol systems.
3. `MetaModel`, mechanism model, intermediate standard-form models, and callback model layers.
4. Solver traits, options, outputs, value conversion, heuristic interfaces, IIS diagnostics, and solver backend plugins.
5. Constraint-programming models, snapshots, sessions, and MIP-backed lowering.

Explicit non-goals:

1. Framework-level column-generation orchestration, Benders combinators, persistence, and remote solving; those belong in `ospf-kotlin-framework`.
2. Domain-specific modeling such as cutting stock, packing, or scheduling.
3. Solver installation and license management beyond backend setup notes.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      User Application                       │
├─────────────────────────────────────────────────────────────┤
│  model/       │ Optimization model definition & management  │
│  variable/    │ Variable type system & variable items       │
│  symbol/      │ Intermediate symbol expressions & functions │
│  token/       │ Variable-solver mapping & caching           │
│  solver/      │ Solver abstraction & value conversion       │
│  error/       │ Core error definitions                      │
├─────────────────────────────────────────────────────────────┤
│  ospf-kotlin-math  │ ospf-kotlin-utils  │ ospf-kotlin-multiarray │
└─────────────────────────────────────────────────────────────┘
```

## Module Structure

| Package | Description | README |
|---------|-------------|--------|
| `variable` | Variable type system, variable items, combinations, and ranges | [README](src/main/fuookami/ospf/kotlin/core/variable/README.md) |
| `token` | Token management — variable-solver mapping, dual-view results, multi-level caching | [README](src/main/fuookami/ospf/kotlin/core/token/README.md) |
| `symbol` | Intermediate symbol system — expressions, combinations, and function symbols | [README](src/main/fuookami/ospf/kotlin/core/symbol/README.md) |
| `model` | Optimization model lifecycle — MetaModel → MechanismModel → Triad/TetradModel → Solver | [README](src/main/fuookami/ospf/kotlin/core/model/README.md) |
| `solver` | Solver abstraction — linear/quadratic solvers, heuristics, IIS diagnostics, output | [README](src/main/fuookami/ospf/kotlin/core/solver/README.md) |
| `error` | Core error code definitions | — |

## Public API

| API | Responsibility | Stability |
| --- | --- | --- |
| `MetaModel<V>` | Primary user-facing model assembly object. | stable |
| `Variable`, `VariableRange`, `VariableType` | Decision variable definitions and ranges. | stable |
| `Token`, `TokenList`, `TokenTable` | Solver-order token mapping and result/cache access. | stable |
| `symbol.function` | Preferred function-symbol path. | stable |
| `symbol.flatten` | Preferred expression-flattening path. | stable |
| `model.mechanism` | Mechanism model and constraint/objective lowering. | stable |
| `LinearTriadModel`, `QuadraticTetradModel` | Standard-form solver input models. | stable |
| `Solver`, `SolveOptions` | Unified solver abstraction and solve options. | stable |
| `solver.config` | Solver-specific configuration. | stable |

## Constraint Programming

The `model.constraint_programming` package provides integer-domain CP models, Boolean literals, intervals, global constraints, immutable snapshots, and a portable snapshot codec. The `solver.constraint_programming` package provides the solver/session SPI, a fake contract solver, SCIP integration, and an exact MIP-backed path. The MIP path supports the declared bounded subset, including optional intervals and variable duration; unsupported formulations return structured `Ret` errors.

For Logic-Based Benders, use `LogicBasedBendersEngine` from `ospf-kotlin-framework`. The implementation keeps proof status separate from feasibility, requires globally valid cuts in `Exact` mode, and exposes structured conflict/IIS evidence through the solver report. See [the implementation plan](../plans/release.md) for capability boundaries and verification commands.

CP model elements carry an explicit identity scope. Use `scope = "stable"` with a caller-owned `origin` when an ID must survive model rebuilds; the default `model-local` scope is only valid within the current model instance. Snapshot, remote result, diagnostic, and checkpoint codecs preserve these IDs and reject duplicate or incomplete identity metadata. The closed repository-wide stable-ID contract (`OSPF-SOL-013`) and its SCIP/Gurobi evidence are recorded in [the release plan](../plans/release.md); adapters for plugins that have not yet passed this contract are governed by [the solver CP plan](../plans/solver_cp.md).

`ConstraintProgrammingCheckpointCodec` writes portable checkpoint v2 envelopes containing the snapshot fingerprint, solver/configuration provenance, validated incumbent, interval values, and audit fields. Restoring a checkpoint rebuilds from the snapshot and revalidates the incumbent; no SCIP/JNI search tree or native handle is persisted, so the capability is `RebuildFromSnapshot`, not `Native`.


## Unified Solve Contract

The core module defines a unified solve contract for consistent solver behavior across all backends:

### Core Contract

- **Result type**: `Ret<SolveReport<V>>` as the primary result contract.
- **Terminal states**: `Optimal`, `Feasible`, `Infeasible`, `Unbounded`, `TimeLimit`, `NodeLimit`, `SolverStopped`.
- **Proof gates**: `Exact` mode requires proven optimality; `Heuristic` mode allows best-effort solutions.
- **Attempt identity**: Each solve attempt carries a unique identifier for traceability.

### Cancellation

- `CancellationToken` supports cooperative cancellation.
- Long-running solves check cancellation at regular intervals.
- Cancellation preserves partial solution when available.

### Error Handling

- **Solver failures**: Wrapped in structured error types with trace context.
- **Infeasibility**: Reported as valid terminal state with IIS evidence when available.
- **Numeric issues**: Overflow and precision loss reported explicitly.

### Fingerprint and Provenance

- `SolveReport` includes solution fingerprint for integrity verification.
- Provenance metadata records solver version, configuration, and execution environment.
- Checkpoint codec enables portable snapshot and incumbent serialization.

## Four-Layer Model Architecture

The core module implements a **four-layer model architecture**:

```
User Definition Layer    →  MetaModel<V>                  (user-facing DSL)
    ↓ dump
Mechanism Model Layer    →  MechanismModel<V>             (flatten symbols, constraints)
    ↓ dump
Standard Form Layer      →  LinearTriadModel /            (sparse matrix form)
                            QuadraticTetradModel
    ↓ invoke
Solver Layer             →  SolverOutput                  (results)
```

## Key Design Patterns

### Dual-View Value Access

Solver backends always produce `Flt64` results. The framework provides type-safe access via `IntoValue<V>`:

```
Solver → Flt64 → Token._result → IntoValue<V> → Token.result (V?)
```

### Intermediate Symbol System

Intermediate symbols are evaluable expressions composed of variables and constants. The framework provides 30+ built-in function symbols (Slack, If, Max, Piecewise, etc.) for constraint construction.

### Cache-Driven Evaluation

The `TokenTable` maintains multi-level caches (linear flatten, quadratic flatten, value, range) to avoid redundant symbol evaluation during model construction and solving.

## Sub-package Overview

### solver/

| Sub-package | Description |
|-------------|-------------|
| `config` | Solver-specific configuration (COPT, Gurobi, SCIP) |
| `heuristic` | Metaheuristic framework (PSO, selection, crossover, mutation) |
| `iis` | Irreducible Infeasible Subsystem diagnostics |
| `output` | Solver output data structures (feasible/infeasible) |
| `value` | Value type conversion (IntoValue interface) |

### model/

| Sub-package | Description |
|-------------|-------------|
| `basic` | Foundation interfaces, enums, and view types |
| `mechanism` | MetaModel, MechanismModel, constraint/objective DSL |
| `intermediate` | Standard form models (triad/tetrad), sparse matrix |
| `callback` | Callback model interface for heuristic solvers |

### symbol/

| Sub-package | Description |
|-------------|-------------|
| `function` | 30+ function symbols (Slack, If, Max, Piecewise, etc.) |
| `flatten` | Expression flattening utilities |

## Testing

```bash
mvn -pl ospf-kotlin-core test -DskipITs

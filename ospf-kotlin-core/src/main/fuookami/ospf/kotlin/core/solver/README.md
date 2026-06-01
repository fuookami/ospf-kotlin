# solver — Solver Abstraction Layer

:us: English | :cn: [简体中文](README_ch.md)

## Overview

The `solver` package is the **solver abstraction layer** of the OSPF framework, defining core interfaces for linear and quadratic solvers, solve output structures, value type conversion, solver configuration, heuristic solving algorithms, and IIS (Irreducible Infeasible Subsystem) diagnostics. It serves as the unified abstraction connecting upper-layer models with low-level solver plugins (Gurobi, CPLEX, SCIP, etc.).

## Package Structure

```
solver/
├── LinearSolver.kt               # Linear solver interface
├── QuadraticSolver.kt            # Quadratic solver interface
├── CoreSolverAsync.kt            # Async solving scope
├── Gap.kt                        # Gap calculation
├── ModelingPreparation.kt        # Model preparation
├── SolveOptions.kt               # Solve options
├── SolverExt.kt                  # Solver extension functions
├── SolverFailureSupport.kt       # Solve failure support
├── SolverMemoryCleanupSupport.kt # Memory cleanup support
├── SolverStatusSupport.kt        # Solve status support
├── UnsupportedFeatureNotice.kt   # Unsupported feature notice
├── config/                       # Solver configuration
│   ├── SolverConfig.kt           # Generic solver config
│   ├── CoptSolverConfig.kt       # COPT config
│   ├── GurobiSolverConfig.kt     # Gurobi config
│   └── SCIPSolverConfig.kt       # SCIP config
├── heuristic/                    # Heuristic solvers
│   ├── ParticleSwarmHeuristicSolver.kt  # PSO solver
│   ├── Population.kt             # Population management
│   ├── Selection.kt / SelectionMode.kt  # Selection strategies
│   ├── Cross.kt / CrossMode.kt   # Crossover strategies
│   ├── Mutation.kt / MutationMode.kt    # Mutation strategies
│   ├── Migration.kt              # Migration strategy
│   ├── Normalization.kt          # Normalization
│   ├── Iteration.kt              # Iteration control
│   └── Policy.kt                 # Policy definition
├── iis/                          # Infeasible subsystem diagnostics
│   ├── IISComputingStatus.kt     # IIS computing status
│   ├── IISConfig.kt              # IIS configuration
│   ├── Linear.kt                 # Linear IIS
│   └── Quadratic.kt              # Quadratic IIS
├── output/                       # Solve output
│   ├── SolverOutput.kt           # Solver output data structures
│   ├── SolverStatus.kt           # Solver status
│   ├── SolvingStatus.kt          # Solving process status
│   └── InfeasibleOutputFields.kt # Infeasible output fields
└── value/                        # Value type conversion
    ├── IntoValue.kt              # Value type conversion interface
    ├── SolveValue.kt             # Solve value
    ├── SolveValueConversionContext.kt  # Conversion context
    └── SolveValueValidation.kt   # Value validation
```

## Core Concepts

### Linear Solver Interface (`LinearSolver.kt`)

The `LinearSolver` interface defines complete linear programming solving capabilities:

**Core solving methods**:
- `invoke(model, callback)` — Solve linear model, returns `FeasibleSolverOutput<Flt64>`
- `invoke(model, solutionAmount, callback)` — Solve for multiple solutions
- `solve(model, converter, callback)` — Generic solve supporting arbitrary value type V

**Full pipeline solving**:
```
MetaModel<V> → dump → MechanismModel<Flt64> → dump → LinearTriadModel → invoke → SolverOutput
```

**Async support**:
- `solveAsync(...)` — Returns `CompletableFuture`, based on coroutine scope

**IIS diagnostics**:
- `invoke(model, callback, iisConfig)` — Solve with infeasible subsystem analysis

### Quadratic Solver Interface (`QuadraticSolver.kt`)

Symmetric to `LinearSolver`, handling quadratic programming problems using `QuadraticTetradModel` as solver input.

### Solve Output (`output/SolverOutput.kt`)

Sealed interface hierarchy:

```
SolverOutput
├── UnifiedSolverOutput     — Unified statistics (iterations, nodes, best bound, gap, time)
├── LinearSolverOutput      — Linear solver output
└── QuadraticSolverOutput   — Quadratic solver output

FeasibleSolverOutput<V>     — Feasible solution output (objective, solution, statistics)
LinearInfeasibleSolverOutput  — Linear infeasible output (with IIS)
QuadraticInfeasibleSolverOutput — Quadratic infeasible output (with IIS)
```

`FeasibleSolverOutput<V>` provides dual-view access for both Flt64 and V types:
- `obj` / `objValue` — Objective value
- `possibleBestObj` / `possibleBestObjValue` — Possible best objective value
- `bestBound` / `bestBoundValue` — Best bound

### Value Type Conversion (`value/IntoValue.kt`)

The `IntoValue<V>` interface is the core conversion mechanism at the solver boundary:

- `intoValue(Flt64) → V` — Convert Flt64 to generic value type
- `fromValue(V) → Flt64` — Convert generic value type back to Flt64
- `zero` / `one` — V-typed constants
- `IntoValue.Identity` — Flt64 identity converter

### Solver Configuration (`config/`)

- `SolverConfig` — Generic config interface controlling model dumping behavior (concurrency, bounds, etc.)
- `CoptSolverConfig` — COPT solver-specific configuration
- `GurobiSolverConfig` — Gurobi solver-specific configuration
- `SCIPSolverConfig` — SCIP solver-specific configuration

### Heuristic Solvers (`heuristic/`)

Built-in metaheuristic solving framework, including:

- **Particle Swarm Optimization** (`ParticleSwarmHeuristicSolver`) — PSO algorithm implementation
- **Population management** (`Population`) — Individual collection management
- **Selection strategies** (`Selection`, `SelectionMode`) — Roulette, tournament, etc.
- **Crossover strategies** (`Cross`, `CrossMode`) — Genetic crossover operations
- **Mutation strategies** (`Mutation`, `MutationMode`) — Genetic mutation operations
- **Migration strategy** (`Migration`) — Inter-population individual migration
- **Iteration control** (`Iteration`) — Iteration count and convergence conditions

### IIS Diagnostics (`iis/`)

Irreducible Infeasible Subsystem analysis:

- `IISConfig` — IIS computation configuration
- `IISComputingStatus` — Computing status enumeration
- Linear/quadratic IIS model views identifying the minimal constraint subset causing infeasibility

## Relationships with Other Packages

- **model** — Solvers consume `LinearTriadModel` / `QuadraticTetradModel` as input
- **token** — Solvers write solve results via `TokenList.setSolverSolution(Flt64)`
- **variable** — Solvers indirectly access variable types and bounds through Token
- **symbol** — `IntoValue` interface is widely used for intermediate symbol evaluation
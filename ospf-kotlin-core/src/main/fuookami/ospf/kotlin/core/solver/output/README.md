# solver/output — Solver Output Package

:us: English | :cn: [简体中文](README_ch.md)

## Overview

The `output` sub-package defines the **data structures for solver results** in the OSPF framework. It provides sealed interfaces and data classes representing feasible solutions, infeasible outputs, solver status, and solving statistics.

## Package Structure

```
output/
├── SolverOutput.kt           # Solver output data structures
├── SolverStatus.kt           # Solver status enumeration
├── SolvingStatus.kt          # Solving process status
└── InfeasibleOutputFields.kt # Infeasible output fields
```

## Core Concepts

### SolverOutput (`SolverOutput.kt`)

Sealed interface hierarchy for solver outputs:

- **`SolverOutput`** — Base sealed interface
- **`UnifiedSolverOutput`** — Unified statistics (iterations, node count, best bound, MIP gap, solve time)
- **`LinearSolverOutput`** — Linear solver output marker
- **`QuadraticSolverOutput`** — Quadratic solver output marker

**`FeasibleSolverOutput<V>`** — Feasible solution output containing:
- `obj` / `objValue` — Objective value (Flt64 and V-typed dual view)
- `solution` — Solution vector
- `time` — Solve time
- `gap` — Optimality gap
- `bestBound` / `bestBoundValue` — Best bound
- `mipGap` — MIP gap
- `iterations` / `nodeCount` — Solver statistics

**`LinearInfeasibleSolverOutput`** / **`QuadraticInfeasibleSolverOutput`** — Infeasible outputs with IIS information.

### SolverStatus (`SolverStatus.kt`)

Enumeration of solver statuses (optimal, infeasible, unbounded, timeout, etc.).

### SolvingStatus (`SolvingStatus.kt`)

Solving process status for callback during long-running solves.

### InfeasibleOutputFields (`InfeasibleOutputFields.kt`)

Fields specific to infeasible solver outputs.

## Relationships with Other Packages

- **solver** — Solver interfaces return `SolverOutput` subtypes
- **solver/iis** — IIS results are embedded in infeasible output types
- **solver/value** — `FeasibleSolverOutput` uses `IntoValue<V>` for type conversion
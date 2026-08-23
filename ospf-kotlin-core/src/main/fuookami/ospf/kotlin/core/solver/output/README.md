# solver/output — Solver Output Package

:us: English | :cn: [简体中文](README_ch.md)

## Overview

The `output` sub-package defines compatibility data structures for solver results. The primary
result contract lives in `solver.report.SolveReport`; this package retains solver status views and
materialized IIS artifacts used by explicit compatibility entry points.

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

**`SolveReport<V>`** — Primary unified result, containing orthogonal problem status,
termination reason, solution/incumbent, proof, statistics, diagnostics, provenance, and fingerprints.

**`LinearInfeasibleSolverOutput`** / **`QuadraticInfeasibleSolverOutput`** — Infeasible outputs with IIS information. When `iisAvailable=false`, IIS analysis failed: `iis` is an original-model snapshot, the failure is recorded in `diagnostics.errors`, and `withIIS()` returns no IIS.

### SolverStatus (`SolverStatus.kt`)

Enumeration of solver statuses (optimal, infeasible, unbounded, timeout, etc.).

### SolvingStatus (`SolvingStatus.kt`)

Solving process status for callback during long-running solves.

### InfeasibleOutputFields (`InfeasibleOutputFields.kt`)

Fields specific to infeasible solver outputs.

## Relationships with Other Packages

- **solver.report** — Solver interfaces return `SolveReport<V>`
- **solver** — Explicit IIS compatibility entry points return `SolverOutput` artifacts
- **solver/iis** — IIS results are embedded in infeasible output types
- **solver/value** — `SolveReport<Flt64>.convertTo(converter)` converts solutions and diagnostics

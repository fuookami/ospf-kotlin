# solver/config — Solver Configuration Package

:us: English | :cn: [简体中文](README_ch.md)

## Overview

The `config` sub-package defines configuration interfaces and implementations for various solver backends in the OSPF framework. Configurations control model dumping behavior, solver-specific parameters, and performance tuning options.

## Package Structure

```
config/
├── SolverConfig.kt       # Generic solver configuration interface
├── CoptSolverConfig.kt   # COPT solver configuration
├── GurobiSolverConfig.kt # Gurobi solver configuration
└── SCIPSolverConfig.kt   # SCIP solver configuration
```

## Core Concepts

### SolverConfig (`SolverConfig.kt`)

Generic solver configuration interface controlling model dumping behavior:

- `dumpIntermediateModelBounds` — Whether to dump intermediate model bounds
- `dumpIntermediateModelForceBounds` — Whether to force dump bounds
- `dumpIntermediateModelConcurrent` — Whether to enable concurrent model dumping
- `dumpMechanismModelConcurrent` — Whether to enable concurrent mechanism model dumping

### CoptSolverConfig (`CoptSolverConfig.kt`)

COPT solver-specific configuration parameters.

### GurobiSolverConfig (`GurobiSolverConfig.kt`)

Gurobi solver-specific configuration parameters.

### SCIPSolverConfig (`SCIPSolverConfig.kt`)

SCIP solver-specific configuration parameters.

## Relationships with Other Packages

- **solver** — `LinearSolver` and `QuadraticSolver` interfaces accept `SolverConfig` to control dumping behavior
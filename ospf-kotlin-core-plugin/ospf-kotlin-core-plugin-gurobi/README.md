# ospf-kotlin-core-plugin-gurobi

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

Gurobi 10+ solver plugin for the OSPF Kotlin framework. This module provides concrete solver implementations that bridge the core solver abstraction layer to the [Gurobi Optimizer](https://www.gurobi.com/) (using `gurobi.*` package imports for Gurobi 10 API).

> For Gurobi 11+, use the [`ospf-kotlin-core-plugin-gurobi11`](../ospf-kotlin-core-plugin-gurobi11) module instead, which uses the relocated `com.gurobi.gurobi.*` package.

## Features

| Capability | Status |
|------------|--------|
| Linear Programming (LP) | :white_check_mark: |
| Mixed-Integer Linear Programming (MILP) | :white_check_mark: |
| Quadratic Programming (QP) | :white_check_mark: |
| Mixed-Integer Quadratic Programming (MIQP) | :white_check_mark: |
| Column Generation | :white_check_mark: |
| Benders Decomposition | :white_check_mark: |
| Remote Server (Compute Server) | :white_check_mark: |
| Multi-Solution Pool | :white_check_mark: |
| Native Callback (GRBCallback) | :white_check_mark: |

## Architecture

```
┌────────────────────────────────────────────────────┐
│  GurobiLinearSolver / GurobiQuadraticSolver        │  Public API
├────────────────────────────────────────────────────┤
│  GurobiColumnGenerationSolver                      │  Column generation
│  GurobiLinearBendersDecompositionSolver            │  Benders decomposition (linear)
│  GurobiQuadraticBendersDecompositionSolver         │  Benders decomposition (quadratic)
├────────────────────────────────────────────────────┤
│  GurobiLinearSolverCallBack                        │  Linear callback manager
│  GurobiQuadraticSolverCallBack                     │  Quadratic callback manager
│  GurobiVariable / GurobiConstraintSign             │  Type mappings
├────────────────────────────────────────────────────┤
│  GurobiSolver (abstract)                           │  Base — init (local/remote), solve, analyzeStatus
│  PluginSolverAsync                                 │  Coroutine scope
└────────────────────────────────────────────────────┘
```

## File Structure

| File | Description |
|------|-------------|
| `GurobiSolver.kt` | Abstract base class — environment initialization (local/remote), solving, and status analysis |
| `GurobiLinearSolver.kt` | Linear solver implementation with multi-solution support |
| `GurobiQuadraticSolver.kt` | Quadratic solver implementation |
| `GurobiColumnGenerationSolver.kt` | Column generation strategy (LP relaxation with dual extraction) |
| `GurobiBendersDecompositionSolver.kt` | Benders decomposition strategy (linear + quadratic) |
| `GurobiSolverCallBack.kt` | Callback managers for linear and quadratic solvers |
| `GurobiVariable.kt` | Variable type mapping (Binary → `GRB.BINARY`, Integer → `GRB.INTEGER`, Continuous → `GRB.CONTINUOUS`) |
| `GurobiConstraint.kt` | Constraint sign mapping (GE/EQ/LE → GRB constraint senses) |
| `PluginSolverAsync.kt` | Coroutine scope for async solving |

## Usage

### Dependency

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-gurobi:1.1.0")
```

> **Note:** The Gurobi native library (`gurobi.jar`) must be provided separately with `provided` scope. Ensure Gurobi is installed and the license is valid.

### Basic Solving

```kotlin
val solver = GurobiLinearSolver(
    config = SolverConfig(timeLimit = 60.seconds)
)
val result: Ret<FeasibleSolverOutput<Flt64>> = solver(model)
```

### With Callbacks

```kotlin
val solver = GurobiLinearSolver(
    config = SolverConfig(),
    callBack = GurobiLinearSolverCallBack()
        .configuration { status, gurobi, variables, constraints ->
            gurobi.set(GRB.IntParam.Threads, 4)
            ok
        }
)
```

### Remote Compute Server

```kotlin
val solver = GurobiLinearSolver(
    config = SolverConfig(
        serverConfig = ServerConfig(
            server = "gurobi-server.example.com",
            password = "secret"
        )
    )
)
```

## Dependencies

| Dependency | Scope | Description |
|------------|-------|-------------|
| `ospf-kotlin-core` | compile | Solver abstraction interfaces |
| `gurobi` | provided | Gurobi Java SDK (not bundled) |

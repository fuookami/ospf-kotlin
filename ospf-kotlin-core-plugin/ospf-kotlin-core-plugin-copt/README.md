# ospf-kotlin-core-plugin-copt

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

COPT (Cardinal Optimizer) solver plugin for the OSPF Kotlin framework. This module provides concrete solver implementations that bridge the core solver abstraction layer to the [COPT](https://www.shanshu.ai/copt) optimization solver developed by杉数科技 (Cardinal Operations).

## Features

| Capability | Status |
|------------|--------|
| Linear Programming (LP) | :white_check_mark: |
| Mixed-Integer Linear Programming (MILP) | :white_check_mark: |
| Quadratic Programming (QP) | :white_check_mark: |
| Mixed-Integer Quadratic Programming (MIQP) | :white_check_mark: |
| Column Generation | :white_check_mark: |
| Benders Decomposition | :white_check_mark: |
| Remote Server Connection | :white_check_mark: |
| Multi-Solution Pool | :white_check_mark: |

## Architecture

```
┌────────────────────────────────────────────────────┐
│  CoptLinearSolver / CoptQuadraticSolver            │  Public API
├────────────────────────────────────────────────────┤
│  CoptColumnGenerationSolver                        │  Column generation
│  CoptLinearBendersDecompositionSolver              │  Benders decomposition (linear)
│  CoptQuadraticBendersDecompositionSolver           │  Benders decomposition (quadratic)
├────────────────────────────────────────────────────┤
│  CoptSolverCallBack                               │  Callback management
│  CoptVariable / CoptConstraintSign                │  Type mappings
├────────────────────────────────────────────────────┤
│  CoptSolver (abstract)                            │  Base — init, solve, analyzeStatus
│  PluginSolverAsync                                │  Coroutine scope
└────────────────────────────────────────────────────┘
```

## File Structure

| File | Description |
|------|-------------|
| `CoptSolver.kt` | Abstract base class — environment initialization (local/remote), solving, and status analysis |
| `CoptLinearSolver.kt` | Linear solver implementation with multi-solution support |
| `CoptQuadraticSolver.kt` | Quadratic solver implementation |
| `CoptColumnGenerationSolver.kt` | Column generation strategy (LP relaxation with dual extraction) |
| `CoptBendersDecompositionSolver.kt` | Benders decomposition strategy (linear + quadratic) |
| `CoptSolverCallBack.kt` | Callback manager — configuration, solution analysis, failure handling |
| `CoptVariable.kt` | Variable type mapping (Binary → `COPT.BINARY`, Integer → `COPT.INTEGER`, Continuous → `COPT.CONTINUOUS`) |
| `CoptConstraint.kt` | Constraint sign mapping (GE/EQ/LE → COPT constraint senses) |
| `Copt.kt` | COPT native constant definitions |
| `PluginSolverAsync.kt` | Coroutine scope for async solving |

## Usage

### Dependency

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-copt:1.1.0")
```

> **Note:** The COPT native library (`copt.jar`) must be provided separately with `provided` scope. Ensure the COPT runtime is installed and the license is valid.

### Basic Solving

```kotlin
val solver = CoptLinearSolver(
    config = SolverConfig(timeLimit = 60.seconds)
)
val result: Ret<FeasibleSolverOutput<Flt64>> = solver(model)
```

### With Callbacks

```kotlin
val solver = CoptLinearSolver(
    config = SolverConfig(),
    callBack = CoptSolverCallBack()
        .configuration { status, coptModel, variables, constraints ->
            // Configure COPT parameters here
            ok
        }
        .analyzingSolution { status, coptModel, variables, constraints ->
            // Post-solve analysis
            ok
        }
)
```

### Remote Server

```kotlin
val solver = CoptLinearSolver(
    config = SolverConfig(
        serverConfig = ServerConfig(
            server = "copt-server.example.com",
            port = UInt64(7878),
            password = "secret"
        )
    )
)
```

## Dependencies

| Dependency | Scope | Description |
|------------|-------|-------------|
| `ospf-kotlin-core` | compile | Solver abstraction interfaces |
| `copt` | provided | COPT Java SDK (not bundled) |

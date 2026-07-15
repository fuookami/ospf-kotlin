# ospf-kotlin-core-plugin-cplex

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

IBM ILOG CPLEX solver plugin for the OSPF Kotlin framework. This module provides concrete solver implementations that bridge the core solver abstraction layer to the [IBM ILOG CPLEX Optimizer](https://www.ibm.com/products/ilog-cplex-optimization-studio).

## Features

| Capability | Status |
|------------|--------|
| Linear Programming (LP) | :white_check_mark: |
| Mixed-Integer Linear Programming (MILP) | :white_check_mark: |
| Quadratic Programming (QP) | :white_check_mark: |
| Mixed-Integer Quadratic Programming (MIQP) | :white_check_mark: |
| Column Generation | :white_check_mark: |
| Benders Decomposition | :white_check_mark: |
| Multi-Solution Pool | :white_check_mark: |

## Architecture

```
┌────────────────────────────────────────────────────┐
│  CplexLinearSolver / CplexQuadraticSolver          │  Public API
├────────────────────────────────────────────────────┤
│  CplexColumnGenerationSolver                       │  Column generation
│  CplexLinearBendersDecompositionSolver             │  Benders decomposition (linear)
│  CplexQuadraticBendersDecompositionSolver          │  Benders decomposition (quadratic)
├────────────────────────────────────────────────────┤
│  CplexSolverCallBack                              │  Callback management
│  CplexVariable                                    │  Type mapping (Binary/Integer/Continuous → IloNumVarType)
├────────────────────────────────────────────────────┤
│  CplexSolver (abstract)                           │  Base — init, analyzeStatus
│  PluginSolverAsync                                │  Coroutine scope
└────────────────────────────────────────────────────┘
```

## File Structure

| File | Description |
|------|-------------|
| `CplexSolver.kt` | Abstract base class — environment initialization and status analysis |
| `CplexLinearSolver.kt` | Linear solver implementation with multi-solution support |
| `CplexQuadraticSolver.kt` | Quadratic solver implementation |
| `CplexColumnGenerationSolver.kt` | Column generation strategy (LP relaxation with dual extraction) |
| `CplexBendersDecompositionSolver.kt` | Benders decomposition strategy (linear + quadratic) |
| `CplexSolverCallBack.kt` | Callback manager — configuration, solution analysis, failure handling |
| `CplexVariable.kt` | Variable type mapping (Binary → `Bool`, Integer → `Int`, Continuous → `Float`) |
| `PluginSolverAsync.kt` | Coroutine scope for async solving |

## Usage

### Dependency

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-cplex:1.1.0")
```

> **Note:** The CPLEX Java library (`cplex.jar`) must be provided separately with `provided` scope. Ensure CPLEX is installed and the license is valid.

### Basic Solving

```kotlin
val solver = CplexLinearSolver(
    config = SolverConfig(timeLimit = 60.seconds)
)
val result: Ret<FeasibleSolverOutput<Flt64>> = solver(model)
```

### With Callbacks

```kotlin
val solver = CplexLinearSolver(
    config = SolverConfig(),
    callBack = CplexSolverCallBack()
        .configuration { status, cplex, variables, constraints ->
            // Configure CPLEX parameters here
            ok
        }
)
```

## Dependencies

| Dependency | Scope | Description |
|------------|-------|-------------|
| `ospf-kotlin-core` | compile | Solver abstraction interfaces |
| `cplex` | provided | CPLEX Java SDK (not bundled) |

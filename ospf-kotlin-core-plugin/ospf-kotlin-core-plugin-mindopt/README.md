# ospf-kotlin-core-plugin-mindopt

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

MindOPT (达摩院求解器) solver plugin for the OSPF Kotlin framework. This module provides concrete solver implementations that bridge the core solver abstraction layer to [MindOPT](https://opt.aliyun.com), the optimization solver developed by Alibaba DAMO Academy.

## Features

| Capability | Status |
|------------|--------|
| Linear Programming (LP) | :white_check_mark: |
| Mixed-Integer Linear Programming (MILP) | :white_check_mark: |
| Quadratic Programming (QP) | :white_check_mark: |
| Mixed-Integer Quadratic Programming (MIQP) | :white_check_mark: |
| Column Generation | :white_check_mark: |
| Benders Decomposition | :white_check_mark: |
| Remote Server Connection | :x: |

## Architecture

```
┌────────────────────────────────────────────────────┐
│  MindOPTLinearSolver / MindOPTQuadraticSolver      │  Public API
├────────────────────────────────────────────────────┤
│  MindOPTColumnGenerationSolver                     │  Column generation
│  MindOPTBendersDecompositionSolver                 │  Benders decomposition
├────────────────────────────────────────────────────┤
│  MindOPTSolverCallBack                             │  Callback management
│  MindOPTVariable / MindOPTConstraint               │  Type mappings
├────────────────────────────────────────────────────┤
│  MindOPTSolver (abstract)                          │  Base — init, solve, analyzeStatus
│  PluginSolverAsync                                 │  Coroutine scope
└────────────────────────────────────────────────────┘
```

## File Structure

| File | Description |
|------|-------------|
| `MindOPTSolver.kt` | Abstract base class — environment initialization, solving, and status analysis |
| `MindOPTLinearSolver.kt` | Linear solver implementation |
| `MindOPTQuadraticSolver.kt` | Quadratic solver implementation |
| `MindOPTColumnGenerationSolver.kt` | Column generation strategy (LP relaxation with dual extraction) |
| `MindOPTBendersDecompositionSolver.kt` | Benders decomposition strategy |
| `MindOPTSolverCallBack.kt` | Callback manager — configuration, solution analysis, failure handling |
| `MindOPTVariable.kt` | Variable type mapping (Binary, Integer, Continuous) |
| `MindOPTConstraint.kt` | Constraint type mapping |
| `PluginSolverAsync.kt` | Coroutine scope for async solving |

## Usage

### Dependency

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-mindopt:1.1.0")
```

> **Note:** The MindOPT native library (`mindopt`) must be provided separately with `provided` scope. Ensure the MindOPT runtime is installed and the license is valid.

### Basic Solving

```kotlin
val solver = MindOPTLinearSolver(
    config = SolverConfig(timeLimit = 60.seconds)
)
val result: Ret<FeasibleSolverOutput<Flt64>> = solver(model)
```

### With Callbacks

```kotlin
val solver = MindOPTLinearSolver(
    config = SolverConfig(),
    callBack = MindOPTSolverCallBack()
        .configuration { status, mindoptModel, variables, constraints ->
            // Configure MindOPT parameters here
            ok
        }
        .analyzingSolution { status, mindoptModel, variables, constraints ->
            // Post-solve analysis
            ok
        }
)
```

## Dependencies

| Dependency | Scope | Description |
|------------|-------|-------------|
| `ospf-kotlin-core` | compile | Solver abstraction interfaces |
| `mindopt` | provided | MindOPT Java SDK (not bundled) |
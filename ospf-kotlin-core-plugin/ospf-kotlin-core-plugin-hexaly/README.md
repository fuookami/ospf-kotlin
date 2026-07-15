# ospf-kotlin-core-plugin-hexaly

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

[Hexaly](https://www.hexaly.com/) (formerly LocalSolver) solver plugin for the OSPF Kotlin framework. Hexaly is a global optimization solver specializing in large-scale combinatorial and mixed-integer nonlinear problems.

## Features

| Capability | Status |
|------------|--------|
| Linear Programming | :white_check_mark: |
| Quadratic Programming | :white_check_mark: |
| Column Generation | :white_check_mark: |
| Multi-Solution | :construction: (partial support) |
| Benders Decomposition | — |

## Architecture

```
┌────────────────────────────────────────────────────┐
│  HexalyLinearSolver / HexalyQuadraticSolver        │  Public API
├────────────────────────────────────────────────────┤
│  HexalyColumnGenerationSolver                      │  Column generation
├────────────────────────────────────────────────────┤
│  HexalySolverCallBack                              │  Callback management
│  HexalyVariable                                    │  Variable type mapping (sealed interface)
├────────────────────────────────────────────────────┤
│  HexalySolver (abstract)                           │  Base — init, solve, analyzeStatus
│  PluginSolverAsync                                 │  Coroutine scope
└────────────────────────────────────────────────────┘
```

## File Structure

| File | Description |
|------|-------------|
| `HexalySolver.kt` | Abstract base class — Hexaly optimizer initialization, solving, and status analysis |
| `HexalyLinearSolver.kt` | Linear solver implementation |
| `HexalyQuadraticSolver.kt` | Quadratic solver implementation |
| `HexalyColumnGenerationSolver.kt` | Column generation strategy |
| `HexalySolverCallBack.kt` | Callback manager with native Hexaly callback support |
| `HexalyVariable.kt` | Sealed interface variable type mapping (Binary → `boolVar`, Integer → `intVar`, Continuous → `floatVar`) |
| `PluginSolverAsync.kt` | Coroutine scope for async solving |

## Usage

### Dependency

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-hexaly:1.1.0")
```

> **Note:** The Hexaly native library must be provided separately with `provided` scope. Ensure Hexaly is installed and the license is valid.

### Basic Solving

```kotlin
val solver = HexalyLinearSolver(
    config = SolverConfig(timeLimit = 60.seconds)
)
val result = solver(model)
```

### With Callbacks

```kotlin
val solver = HexalyLinearSolver(
    callBack = HexalySolverCallBack()
        .configuration { status, hexaly, variables, constraints ->
            hexaly.getParamDouble(HxParamType.VTimeLimit) = 30.0
            ok
        }
)
```

## Dependencies

| Dependency | Scope | Description |
|------------|-------|-------------|
| `ospf-kotlin-core` | compile | Solver abstraction interfaces |
| `hexaly` | provided | Hexaly Java SDK (not bundled) |
| `localsolver` | provided | LocalSolver legacy SDK (not bundled) |
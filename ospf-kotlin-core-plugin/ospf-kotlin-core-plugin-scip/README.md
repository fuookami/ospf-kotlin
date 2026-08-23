# ospf-kotlin-core-plugin-scip

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

SCIP (Solving Constraint Integer Programs) solver plugin for the OSPF Kotlin framework. This module provides concrete solver implementations that bridge the core solver abstraction layer to [SCIP](https://scipopt.org), an open-source mixed-integer programming solver developed by the Zuse Institute Berlin (ZIB).

## Features

| Capability | Status |
|------------|--------|
| Linear Programming (LP) | :white_check_mark: |
| Mixed-Integer Linear Programming (MILP) | :white_check_mark: |
| Quadratic Programming (QP) | :white_check_mark: |
| Mixed-Integer Quadratic Programming (MIQP) | :white_check_mark: |
| Column Generation | :white_check_mark: |
| Benders Decomposition | :white_check_mark: |
| Concurrent Solving | :white_check_mark: |
| Constraint Programming snapshot solving | :white_check_mark: (exact compiler lowering) |
| Optional interval / variable duration | :white_check_mark: (exact lowering) |
| Native optional interval / native checkpoint | :x: (not declared) |
| JAR-packaged Native Library | :white_check_mark: |
| Remote Server Connection | :x: |

## Architecture

```
┌────────────────────────────────────────────────────┐
│  ScipLinearSolver / ScipQuadraticSolver            │  Public API
├────────────────────────────────────────────────────┤
│  ScipColumnGenerationSolver                        │  Column generation
│  ScipBendersDecompositionSolver                    │  Benders decomposition
├────────────────────────────────────────────────────┤
│  ScipSolverCallBack                                │  Callback management
│  ScipVariable                                      │  Type mappings
├────────────────────────────────────────────────────┤
│  ScipSolver (abstract)                             │  Base — init, solve, analyzeStatus
│                                                     │  Native library loading (JNA)
│                                                     │  Concurrent solve support
│  PluginSolverAsync                                 │  Coroutine scope
└────────────────────────────────────────────────────┘
```

## File Structure

| File | Description |
|------|-------------|
| `ScipSolver.kt` | Abstract base class — native library loading via JNA, environment initialization, concurrent solving, and status analysis |
| `ScipLinearSolver.kt` | Linear solver implementation |
| `ScipQuadraticSolver.kt` | Quadratic solver implementation |
| `ScipColumnGenerationSolver.kt` | Column generation strategy (LP relaxation with dual extraction) |
| `ScipBendersDecompositionSolver.kt` | Benders decomposition strategy |
| `ScipSolverCallBack.kt` | Callback manager — configuration, solution analysis, failure handling |
| `ScipVariable.kt` | Variable type mapping (Binary, Integer, Continuous) |
| `PluginSolverAsync.kt` | Coroutine scope for async solving |

## Usage

### Dependency

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-scip:1.1.0")
```

> **Note:** The SCIP Java binding (`jscip`) must be provided separately with `provided` scope. Ensure the SCIP native library is available in your system or packaged in the JAR.

### Native Library Loading for JAR Deployment

When deploying as a JAR, use `ScipSolver.loadLibraryInJar()` to load the bundled native library:

```kotlin
ScipSolver.loadLibraryInJar()  // Loads native lib from JAR resources
```

For local development with system-installed SCIP, the library will be loaded automatically via JNA.

### Basic Solving

```kotlin
val solver = ScipLinearSolver(
    config = SolverConfig(time = 60.seconds)
)
val result: Ret<SolveReport<Flt64>> = solver(model)
```

### Concurrent Solving

```kotlin
val solver = ScipLinearSolver(
    config = SolverConfig(
        threadNum = UInt64(4),
        backendConfiguration = SCIPSolverConfig(presolve = true)
    )
)
```

### With Callbacks

```kotlin
val solver = ScipLinearSolver(
    config = SolverConfig(),
    callBack = ScipSolverCallBack()
        .configuration { status, scipModel, variables, constraints ->
            // Configure SCIP parameters here
            ok
        }
        .analyzingSolution { status, scipModel, variables, constraints ->
            // Post-solve analysis
            ok
        }
)
```

### Constraint Programming capability boundary

`ScipConstraintProgrammingSolver` accepts the portable CP snapshot and compiles the supported constraint family into a SCIP model. Fixed intervals, optional intervals, and variable duration are exposed as `ExactLowering`; the compiler preserves presence, start/size/end, resource, and global-constraint semantics and maps auxiliary artifacts back to source IDs. The plugin does not claim SCIP-native optional intervals, variable-duration jobs, incremental JSCIP sessions, or native checkpoint/resume. Unsupported features return a structured capability error and retain the Fake/MIP-backed fallback where declared by core.

### SolveReport migration and follow-up scope

`Ret<SolveReport<Flt64>>` is the primary result contract. Use `SolverConfig(time = ...)`,
`threadNum`, and typed `backendConfiguration = SCIPSolverConfig(...)`; the old `timeLimit` and
`concurrentConfig` constructors are not part of the 1.1.0 source line. Other plugin migration and
capability gates are tracked in [`plans/solver_cp.md`](../../plans/solver_cp.md).

## Dependencies

| Dependency | Scope | Description |
|------------|-------|-------------|
| `ospf-kotlin-core` | compile | Solver abstraction interfaces |
| `jscip` | provided | SCIP Java binding (not bundled) |

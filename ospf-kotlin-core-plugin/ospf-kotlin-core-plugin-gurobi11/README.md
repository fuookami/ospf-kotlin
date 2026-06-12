# ospf-kotlin-core-plugin-gurobi11

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

Gurobi 11+ solver plugin for the OSPF Kotlin framework. This module provides concrete solver implementations that bridge the core solver abstraction layer to the [Gurobi Optimizer 11+](https://www.gurobi.com/) (using `com.gurobi.gurobi.*` relocated package imports).

> For Gurobi 10, use the [`ospf-kotlin-core-plugin-gurobi`](../ospf-kotlin-core-plugin-gurobi) module instead, which uses the `gurobi.*` package.

## Why a Separate Module?

Gurobi 11 relocated its Java package from `gurobi.*` to `com.gurobi.gurobi.*`. This module tracks the new package structure while maintaining identical functionality to the Gurobi 10 module.

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
| Native Callback | :white_check_mark: |

## Architecture

The architecture mirrors the Gurobi 10 module. See [`ospf-kotlin-core-plugin-gurobi`](../ospf-kotlin-core-plugin-gurobi) for the full diagram. All class names and APIs are identical — only the Gurobi package import path differs.

## File Structure

| File | Description |
|------|-------------|
| `GurobiSolver.kt` | Abstract base class (Gurobi 11 package) |
| `GurobiLinearSolver.kt` | Linear solver implementation |
| `GurobiQuadraticSolver.kt` | Quadratic solver implementation |
| `GurobiColumnGenerationSolver.kt` | Column generation strategy |
| `GurobiBendersDecompositionSolver.kt` | Benders decomposition strategy |
| `GurobiSolverCallBack.kt` | Callback managers |
| `GurobiVariable.kt` | Variable type mapping |
| `GurobiConstraint.kt` | Constraint sign mapping |
| `PluginSolverAsync.kt` | Coroutine scope for async solving |

## Usage

### Dependency

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-gurobi11:1.1.0")
```

> **Note:** The Gurobi 11+ native library (`gurobi.jar`) must be on the classpath. The dependency uses `compile` scope (not `provided`) as Gurobi 11 is published to Maven repositories.

### API Compatibility

The API is fully compatible with the Gurobi 10 module. Simply swap the dependency:

```kotlin
// Gurobi 10
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-gurobi:1.1.0")

// Gurobi 11+
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-gurobi11:1.1.0")
```

All class names (`GurobiLinearSolver`, `GurobiQuadraticSolver`, etc.) remain the same.

## Dependencies

| Dependency | Scope | Description |
|------------|-------|-------------|
| `ospf-kotlin-core` | compile | Solver abstraction interfaces |
| `com.gurobi:gurobi` | compile | Gurobi 11+ Java SDK |

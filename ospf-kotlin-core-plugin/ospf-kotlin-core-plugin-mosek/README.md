# ospf-kotlin-core-plugin-mosek

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

MOSEK solver plugin for the OSPF Kotlin framework. This module provides concrete solver implementations that bridge the core solver abstraction layer to [MOSEK](https://www.mosek.com), a specialized solver for conic optimization (linear, quadratic, semidefinite programming).

> **:construction: Status: Partially Implemented**
>
> This module is currently under development. Only the `analyzeStatus` functionality is implemented. The `init` and `solve` methods return "not implemented yet". Full LP and QP capabilities will be added in future releases.

## Features

| Capability | Status |
|------------|--------|
| Linear Programming (LP) | :construction: (partial) |
| Quadratic Programming (QP) | :x: |
| Column Generation | :x: |
| Benders Decomposition | :x: |

## Architecture

```
┌────────────────────────────────────────────────────┐
│  MosekLinearSolver                                 │  Public API (partial)
├────────────────────────────────────────────────────┤
│  MosekSolverCallBack                               │  Callback management
├────────────────────────────────────────────────────┤
│  MosekSolver (abstract)                            │  Base — init/solve: "not implemented"
│                                                     │  analyzeStatus: functional
└────────────────────────────────────────────────────┘
```

## File Structure

| File | Description |
|------|-------------|
| `MosekSolver.kt` | Abstract base class — partial implementation (init/solve return "not implemented yet", only analyzeStatus is functional) |
| `MosekLinearSolver.kt` | Linear solver placeholder |
| `MosekSolverCallBack.kt` | Callback manager — configuration, solution analysis, failure handling |

## Usage

### Dependency

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-mosek:1.1.0")
```

> **Note:** The MOSEK native library (`mosek`) must be provided separately with `provided` scope. Ensure the MOSEK runtime is installed and the license is valid.

> **:construction: Warning:** This module is not yet ready for production use. Solving functionality is not implemented.

## Dependencies

| Dependency | Scope | Description |
|------------|-------|-------------|
| `ospf-kotlin-core` | compile | Solver abstraction interfaces |
| `mosek` | provided | MOSEK Java SDK (not bundled) |
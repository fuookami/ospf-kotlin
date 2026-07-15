# ospf-kotlin-core-plugin-lingo

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

LINGO solver plugin for the OSPF Kotlin framework. This module will provide concrete solver implementations that bridge the core solver abstraction layer to [LINGO](https://www.lindo.com), an optimization modeling solver developed by LINDO Systems.

> **:construction: Status: Placeholder Module**
>
> This module is currently a placeholder with no implemented functionality. All solver capabilities are planned but not yet implemented. The module structure is defined for future development.

## Features

| Capability | Status |
|------------|--------|
| Linear Programming (LP) | :construction: |
| Mixed-Integer Linear Programming (MILP) | :construction: |
| Quadratic Programming (QP) | :construction: |
| Mixed-Integer Quadratic Programming (MIQP) | :construction: |
| Column Generation | :construction: |
| Benders Decomposition | :construction: |

## Architecture

```
┌────────────────────────────────────────────────────┐
│  LingoLinearSolver                                 │  Placeholder
└────────────────────────────────────────────────────┘
```

## File Structure

| File | Description |
|------|-------------|
| `LingoLinearSolver.kt` | Empty placeholder class — no implementation yet |

## Usage

### Dependency

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-lingo:1.1.0")
```

> **Note:** The LINGO native library (`lingo`) must be provided separately with `provided` scope when implementation is complete.

> **:construction: Warning:** This module is not yet ready for use. No solving functionality is implemented.

## Dependencies

| Dependency | Scope | Description |
|------------|-------|-------------|
| `ospf-kotlin-core` | compile | Solver abstraction interfaces |
| `lingo` | provided | LINGO Java SDK (not bundled) |
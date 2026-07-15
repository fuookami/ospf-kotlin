# ospf-kotlin-core-plugin-optverse

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

OPTVerse solver plugin for the OSPF Kotlin framework. This module will provide concrete solver implementations that bridge the core solver abstraction layer to [OPTVerse](https://www.huaweicloud.com/product/optverse.html), Huawei Cloud's optimization service.

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
│  OPTVerseLinearSolver                              │  Placeholder
└────────────────────────────────────────────────────┘
```

## File Structure

| File | Description |
|------|-------------|
| `OPTVerseLinearSolver.kt` | Empty placeholder class — no implementation yet |

## Usage

### Dependency

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-optverse:1.1.0")
```

Unlike other solver plugins, OPTVerse uses Huawei Cloud SDK as compile dependencies:

```kotlin
dependencies {
    implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-optverse:1.1.0")
    // Huawei Cloud SDK is already bundled as compile dependency
}
```

> **:construction: Warning:** This module is not yet ready for use. No solving functionality is implemented.

## Dependencies

| Dependency | Scope | Description |
|------------|-------|-------------|
| `ospf-kotlin-core` | compile | Solver abstraction interfaces |
| `com.huaweicloud.sdk:huaweicloud-sdk-optverse` | compile | Huawei Cloud OPTVerse SDK (bundled) |
| `com.huaweicloud.sdk:huaweicloud-sdk-core` | compile | Huawei Cloud SDK core (bundled) |
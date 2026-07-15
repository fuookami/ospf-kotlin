# ospf-kotlin-core-plugin-gurobi11

:us: [English](README.md) | :cn: 简体中文

## 简介

OSPF Kotlin 框架的 Gurobi 11+ 求解器插件模块。本模块提供了将核心求解器抽象层桥接到 [Gurobi 优化器 11+](https://www.gurobi.com/)的具体实现（使用 `com.gurobi.gurobi.*` 重定位包导入）。

> 对于 Gurobi 10，请使用 [`ospf-kotlin-core-plugin-gurobi`](../ospf-kotlin-core-plugin-gurobi) 模块，该模块使用 `gurobi.*` 包。

## 为什么需要独立模块？

Gurobi 11 将其 Java 包从 `gurobi.*` 重定位到 `com.gurobi.gurobi.*`。本模块跟踪新的包结构，同时保持与 Gurobi 10 模块完全相同的功能。

## 功能特性

| 能力 | 状态 |
|------|------|
| 线性规划 (LP) | :white_check_mark: |
| 混合整数线性规划 (MILP) | :white_check_mark: |
| 二次规划 (QP) | :white_check_mark: |
| 混合整数二次规划 (MIQP) | :white_check_mark: |
| 列生成 | :white_check_mark: |
| Benders 分解 | :white_check_mark: |
| 远程计算服务器 | :white_check_mark: |
| 多解池 | :white_check_mark: |
| 原生回调 | :white_check_mark: |

## 架构

架构与 Gurobi 10 模块完全相同。详见 [`ospf-kotlin-core-plugin-gurobi`](../ospf-kotlin-core-plugin-gurobi)。所有类名和 API 均一致——仅 Gurobi 包导入路径不同。

## 文件结构

| 文件 | 说明 |
|------|------|
| `GurobiSolver.kt` | 抽象基类（Gurobi 11 包） |
| `GurobiLinearSolver.kt` | 线性求解器实现 |
| `GurobiQuadraticSolver.kt` | 二次求解器实现 |
| `GurobiColumnGenerationSolver.kt` | 列生成策略 |
| `GurobiBendersDecompositionSolver.kt` | Benders 分解策略 |
| `GurobiSolverCallBack.kt` | 回调管理器 |
| `GurobiVariable.kt` | 变量类型映射 |
| `GurobiConstraint.kt` | 约束符号映射 |
| `PluginSolverAsync.kt` | 异步求解协程作用域 |

## 使用

### 依赖

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-gurobi11:1.1.0")
```

> **注意：** Gurobi 11+ 原生库（`gurobi.jar`）必须在类路径上。该依赖使用 `compile` 作用域（非 `provided`），因为 Gurobi 11 已发布到 Maven 仓库。

### API 兼容性

API 与 Gurobi 10 模块完全兼容。只需切换依赖：

```kotlin
// Gurobi 10
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-gurobi:1.1.0")

// Gurobi 11+
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-gurobi11:1.1.0")
```

所有类名（`GurobiLinearSolver`、`GurobiQuadraticSolver` 等）保持不变。

## 依赖

| 依赖 | 作用域 | 说明 |
|------|--------|------|
| `ospf-kotlin-core` | compile | 求解器抽象接口 |
| `com.gurobi:gurobi` | compile | Gurobi 11+ Java SDK |

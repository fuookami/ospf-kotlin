# ospf-kotlin-core-plugin-gurobi

:us: [English](README.md) | :cn: 简体中文

## 简介

OSPF Kotlin 框架的 Gurobi 10+ 求解器插件模块。本模块提供了将核心求解器抽象层桥接到 [Gurobi 优化器](https://www.gurobi.com/)的具体实现（使用 `gurobi.*` 包导入，对应 Gurobi 10 API）。

> 对于 Gurobi 11+，请使用 [`ospf-kotlin-core-plugin-gurobi11`](../ospf-kotlin-core-plugin-gurobi11) 模块，该模块使用重定位后的 `com.gurobi.gurobi.*` 包。

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
| 原生回调 (GRBCallback) | :white_check_mark: |

## 架构

```
┌────────────────────────────────────────────────────┐
│  GurobiLinearSolver / GurobiQuadraticSolver        │  公共 API
├────────────────────────────────────────────────────┤
│  GurobiColumnGenerationSolver                      │  列生成
│  GurobiLinearBendersDecompositionSolver            │  Benders 分解（线性）
│  GurobiQuadraticBendersDecompositionSolver         │  Benders 分解（二次）
├────────────────────────────────────────────────────┤
│  GurobiLinearSolverCallBack                        │  线性回调管理器
│  GurobiQuadraticSolverCallBack                     │  二次回调管理器
│  GurobiVariable / GurobiConstraintSign             │  类型映射
├────────────────────────────────────────────────────┤
│  GurobiSolver (abstract)                           │  基类——init（本地/远程）、solve、analyzeStatus
│  PluginSolverAsync                                 │  协程作用域
└────────────────────────────────────────────────────┘
```

## 文件结构

| 文件 | 说明 |
|------|------|
| `GurobiSolver.kt` | 抽象基类——环境初始化（本地/远程）、求解和状态分析 |
| `GurobiLinearSolver.kt` | 线性求解器实现，支持多解 |
| `GurobiQuadraticSolver.kt` | 二次求解器实现 |
| `GurobiColumnGenerationSolver.kt` | 列生成策略（LP 松弛 + 对偶解提取） |
| `GurobiBendersDecompositionSolver.kt` | Benders 分解策略（线性 + 二次） |
| `GurobiSolverCallBack.kt` | 线性和二次求解器的回调管理器 |
| `GurobiVariable.kt` | 变量类型映射（Binary → `GRB.BINARY`、Integer → `GRB.INTEGER`、Continuous → `GRB.CONTINUOUS`） |
| `GurobiConstraint.kt` | 约束符号映射（GE/EQ/LE → GRB 约束符号） |
| `PluginSolverAsync.kt` | 异步求解协程作用域 |

## 使用

### 依赖

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-gurobi:1.1.0")
```

> **注意：** Gurobi 原生库（`gurobi.jar`）需要以 `provided` 作用域单独提供。请确保 Gurobi 已安装且许可证有效。

### 基本求解

```kotlin
val solver = GurobiLinearSolver(
    config = SolverConfig(timeLimit = 60.seconds)
)
val result: Ret<FeasibleSolverOutput<Flt64>> = solver(model)
```

### 使用回调

```kotlin
val solver = GurobiLinearSolver(
    config = SolverConfig(),
    callBack = GurobiLinearSolverCallBack()
        .configuration { status, gurobi, variables, constraints ->
            gurobi.set(GRB.IntParam.Threads, 4)
            ok
        }
)
```

### 远程计算服务器

```kotlin
val solver = GurobiLinearSolver(
    config = SolverConfig(
        serverConfig = ServerConfig(
            server = "gurobi-server.example.com",
            password = "secret"
        )
    )
)
```

## 依赖

| 依赖 | 作用域 | 说明 |
|------|--------|------|
| `ospf-kotlin-core` | compile | 求解器抽象接口 |
| `gurobi` | provided | Gurobi Java SDK（未捆绑） |

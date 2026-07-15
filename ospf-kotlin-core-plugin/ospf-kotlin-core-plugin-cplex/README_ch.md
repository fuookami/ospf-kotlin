# ospf-kotlin-core-plugin-cplex

:us: [English](README.md) | :cn: 简体中文

## 简介

OSPF Kotlin 框架的 IBM ILOG CPLEX 求解器插件模块。本模块提供了将核心求解器抽象层桥接到 [IBM ILOG CPLEX 优化器](https://www.ibm.com/products/ilog-cplex-optimization-studio)的具体实现。

## 功能特性

| 能力 | 状态 |
|------|------|
| 线性规划 (LP) | :white_check_mark: |
| 混合整数线性规划 (MILP) | :white_check_mark: |
| 二次规划 (QP) | :white_check_mark: |
| 混合整数二次规划 (MIQP) | :white_check_mark: |
| 列生成 | :white_check_mark: |
| Benders 分解 | :white_check_mark: |
| 多解池 | :white_check_mark: |

## 架构

```
┌────────────────────────────────────────────────────┐
│  CplexLinearSolver / CplexQuadraticSolver          │  公共 API
├────────────────────────────────────────────────────┤
│  CplexColumnGenerationSolver                       │  列生成
│  CplexLinearBendersDecompositionSolver             │  Benders 分解（线性）
│  CplexQuadraticBendersDecompositionSolver          │  Benders 分解（二次）
├────────────────────────────────────────────────────┤
│  CplexSolverCallBack                              │  回调管理
│  CplexVariable                                    │  类型映射
├────────────────────────────────────────────────────┤
│  CplexSolver (abstract)                           │  基类——init、analyzeStatus
│  PluginSolverAsync                                │  协程作用域
└────────────────────────────────────────────────────┘
```

## 文件结构

| 文件 | 说明 |
|------|------|
| `CplexSolver.kt` | 抽象基类——环境初始化和状态分析 |
| `CplexLinearSolver.kt` | 线性求解器实现，支持多解 |
| `CplexQuadraticSolver.kt` | 二次求解器实现 |
| `CplexColumnGenerationSolver.kt` | 列生成策略（LP 松弛 + 对偶解提取） |
| `CplexBendersDecompositionSolver.kt` | Benders 分解策略（线性 + 二次） |
| `CplexSolverCallBack.kt` | 回调管理器——配置、解分析、失败处理 |
| `CplexVariable.kt` | 变量类型映射（Binary → `Bool`、Integer → `Int`、Continuous → `Float`） |
| `PluginSolverAsync.kt` | 异步求解协程作用域 |

## 使用

### 依赖

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-cplex:1.1.0")
```

> **注意：** CPLEX Java 库（`cplex.jar`）需要以 `provided` 作用域单独提供。请确保 CPLEX 已安装且许可证有效。

### 基本求解

```kotlin
val solver = CplexLinearSolver(
    config = SolverConfig(timeLimit = 60.seconds)
)
val result: Ret<FeasibleSolverOutput<Flt64>> = solver(model)
```

### 使用回调

```kotlin
val solver = CplexLinearSolver(
    config = SolverConfig(),
    callBack = CplexSolverCallBack()
        .configuration { status, cplex, variables, constraints ->
            // 在此配置 CPLEX 参数
            ok
        }
)
```

## 依赖

| 依赖 | 作用域 | 说明 |
|------|--------|------|
| `ospf-kotlin-core` | compile | 求解器抽象接口 |
| `cplex` | provided | CPLEX Java SDK（未捆绑） |

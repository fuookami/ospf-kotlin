# ospf-kotlin-core-plugin-hexaly

:us: [English](README.md) | :cn: 简体中文

## 简介

OSPF Kotlin 框架的 [Hexaly](https://www.hexaly.com/)（原名 LocalSolver）求解器插件模块。Hexaly 是一款专注于大规模组合和混合整数非线性问题的全局优化求解器。

## 功能特性

| 能力 | 状态 |
|------|------|
| 线性规划 | :white_check_mark: |
| 二次规划 | :white_check_mark: |
| 列生成 | :white_check_mark: |
| 多解 | :construction:（部分支持） |
| Benders 分解 | — |

## 架构

```
┌────────────────────────────────────────────────────┐
│  HexalyLinearSolver / HexalyQuadraticSolver        │  公共 API
├────────────────────────────────────────────────────┤
│  HexalyColumnGenerationSolver                      │  列生成
├────────────────────────────────────────────────────┤
│  HexalySolverCallBack                              │  回调管理
│  HexalyVariable                                    │  变量类型映射（密封接口）
├────────────────────────────────────────────────────┤
│  HexalySolver (abstract)                           │  基类——init、solve、analyzeStatus
│  PluginSolverAsync                                 │  协程作用域
└────────────────────────────────────────────────────┘
```

## 文件结构

| 文件 | 说明 |
|------|------|
| `HexalySolver.kt` | 抽象基类——Hexaly 优化器初始化、求解和状态分析 |
| `HexalyLinearSolver.kt` | 线性求解器实现 |
| `HexalyQuadraticSolver.kt` | 二次求解器实现 |
| `HexalyColumnGenerationSolver.kt` | 列生成策略 |
| `HexalySolverCallBack.kt` | 回调管理器，支持原生 Hexaly 回调 |
| `HexalyVariable.kt` | 密封接口变量类型映射（Binary → `boolVar`、Integer → `intVar`、Continuous → `floatVar`） |
| `PluginSolverAsync.kt` | 异步求解协程作用域 |

## 使用

### 依赖

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-hexaly:1.1.0")
```

> **注意：** Hexaly 原生库需要以 `provided` 作用域单独提供。请确保 Hexaly 已安装且许可证有效。

### 基本求解

```kotlin
val solver = HexalyLinearSolver(
    config = SolverConfig(timeLimit = 60.seconds)
)
val result = solver(model)
```

### 使用回调

```kotlin
val solver = HexalyLinearSolver(
    callBack = HexalySolverCallBack()
        .configuration { status, hexaly, variables, constraints ->
            hexaly.getParamDouble(HxParamType.VTimeLimit) = 30.0
            ok
        }
)
```

## 依赖

| 依赖 | 作用域 | 说明 |
|------|--------|------|
| `ospf-kotlin-core` | compile | 求解器抽象接口 |
| `hexaly` | provided | Hexaly Java SDK（未捆绑） |
| `localsolver` | provided | LocalSolver 遗留 SDK（未捆绑） |
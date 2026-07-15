# ospf-kotlin-core-plugin-copt

:us: [English](README.md) | :cn: 简体中文

## 简介

OSPF Kotlin 框架的 COPT（杉数求解器）插件模块。本模块提供了将核心求解器抽象层桥接到杉数科技开发的 [COPT](https://www.shanshu.ai/copt) 优化求解器的具体实现。

## 功能特性

| 能力 | 状态 |
|------|------|
| 线性规划 (LP) | :white_check_mark: |
| 混合整数线性规划 (MILP) | :white_check_mark: |
| 二次规划 (QP) | :white_check_mark: |
| 混合整数二次规划 (MIQP) | :white_check_mark: |
| 列生成 | :white_check_mark: |
| Benders 分解 | :white_check_mark: |
| 远程服务器连接 | :white_check_mark: |
| 多解池 | :white_check_mark: |

## 架构

```
┌────────────────────────────────────────────────────┐
│  CoptLinearSolver / CoptQuadraticSolver            │  公共 API
├────────────────────────────────────────────────────┤
│  CoptColumnGenerationSolver                        │  列生成
│  CoptLinearBendersDecompositionSolver              │  Benders 分解（线性）
│  CoptQuadraticBendersDecompositionSolver           │  Benders 分解（二次）
├────────────────────────────────────────────────────┤
│  CoptSolverCallBack                               │  回调管理
│  CoptVariable / CoptConstraintSign                │  类型映射
├────────────────────────────────────────────────────┤
│  CoptSolver (abstract)                            │  基类——init、solve、analyzeStatus
│  PluginSolverAsync                                │  协程作用域
└────────────────────────────────────────────────────┘
```

## 文件结构

| 文件 | 说明 |
|------|------|
| `CoptSolver.kt` | 抽象基类——环境初始化（本地/远程）、求解和状态分析 |
| `CoptLinearSolver.kt` | 线性求解器实现，支持多解 |
| `CoptQuadraticSolver.kt` | 二次求解器实现 |
| `CoptColumnGenerationSolver.kt` | 列生成策略（LP 松弛 + 对偶解提取） |
| `CoptBendersDecompositionSolver.kt` | Benders 分解策略（线性 + 二次） |
| `CoptSolverCallBack.kt` | 回调管理器——配置、解分析、失败处理 |
| `CoptVariable.kt` | 变量类型映射（Binary → `COPT.BINARY`、Integer → `COPT.INTEGER`、Continuous → `COPT.CONTINUOUS`） |
| `CoptConstraint.kt` | 约束符号映射（GE/EQ/LE → COPT 约束符号） |
| `Copt.kt` | COPT 原生常量定义 |
| `PluginSolverAsync.kt` | 异步求解协程作用域 |

## 使用

### 依赖

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-copt:1.1.0")
```

> **注意：** COPT 原生库（`copt.jar`）需要以 `provided` 作用域单独提供。请确保 COPT 运行时已安装且许可证有效。

### 基本求解

```kotlin
val solver = CoptLinearSolver(
    config = SolverConfig(timeLimit = 60.seconds)
)
val result: Ret<FeasibleSolverOutput<Flt64>> = solver(model)
```

### 使用回调

```kotlin
val solver = CoptLinearSolver(
    config = SolverConfig(),
    callBack = CoptSolverCallBack()
        .configuration { status, coptModel, variables, constraints ->
            // 在此配置 COPT 参数
            ok
        }
        .analyzingSolution { status, coptModel, variables, constraints ->
            // 求解后分析
            ok
        }
)
```

### 远程服务器

```kotlin
val solver = CoptLinearSolver(
    config = SolverConfig(
        serverConfig = ServerConfig(
            server = "copt-server.example.com",
            port = UInt64(7878),
            password = "secret"
        )
    )
)
```

## 依赖

| 依赖 | 作用域 | 说明 |
|------|--------|------|
| `ospf-kotlin-core` | compile | 求解器抽象接口 |
| `copt` | provided | COPT Java SDK（未捆绑） |

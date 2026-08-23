# ospf-kotlin-core-plugin-scip

:us: [English](README.md) | :cn: 简体中文

## 简介

OSPF Kotlin 框架的 SCIP（求解约束整数规划）求解器插件模块。本模块提供了将核心求解器抽象层桥接到柏林 Zuse 研究所（ZIB）开发的开源混合整数规划求解器 [SCIP](https://scipopt.org) 的具体实现。

## 功能特性

| 能力 | 状态 |
|------|------|
| 线性规划 (LP) | :white_check_mark: |
| 混合整数线性规划 (MILP) | :white_check_mark: |
| 二次规划 (QP) | :white_check_mark: |
| 混合整数二次规划 (MIQP) | :white_check_mark: |
| 列生成 | :white_check_mark: |
| Benders 分解 | :white_check_mark: |
| 并发求解 | :white_check_mark: |
| 约束规划 snapshot 求解 | :white_check_mark:（精确编译 lowering） |
| Optional interval / variable duration | :white_check_mark:（精确 lowering） |
| Native optional interval / native checkpoint | :x:（未声明） |
| JAR 打包原生库 | :white_check_mark: |
| 远程服务器连接 | :x: |

## 架构

```
┌────────────────────────────────────────────────────┐
│  ScipLinearSolver / ScipQuadraticSolver            │  公共 API
├────────────────────────────────────────────────────┤
│  ScipColumnGenerationSolver                        │  列生成
│  ScipBendersDecompositionSolver                    │  Benders 分解
├────────────────────────────────────────────────────┤
│  ScipSolverCallBack                                │  回调管理
│  ScipVariable                                      │  类型映射
├────────────────────────────────────────────────────┤
│  ScipSolver (abstract)                             │  基类——init、solve、analyzeStatus
│                                                     │  原生库加载（JNA）
│                                                     │  并发求解支持
│  PluginSolverAsync                                 │  协程作用域
└────────────────────────────────────────────────────┘
```

## 文件结构

| 文件 | 说明 |
|------|------|
| `ScipSolver.kt` | 抽象基类——通过 JNA 加载原生库、环境初始化、并发求解和状态分析 |
| `ScipLinearSolver.kt` | 线性求解器实现 |
| `ScipQuadraticSolver.kt` | 二次求解器实现 |
| `ScipColumnGenerationSolver.kt` | 列生成策略（LP 松弛 + 对偶解提取） |
| `ScipBendersDecompositionSolver.kt` | Benders 分解策略 |
| `ScipSolverCallBack.kt` | 回调管理器——配置、解分析、失败处理 |
| `ScipVariable.kt` | 变量类型映射（Binary、Integer、Continuous） |
| `PluginSolverAsync.kt` | 异步求解协程作用域 |

## 使用

### 依赖

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-scip:1.1.0")
```

> **注意：** SCIP Java 绑定（`jscip`）需要以 `provided` 作用域单独提供。请确保 SCIP 原生库在系统中可用或打包在 JAR 中。

### JAR 部署时的原生库加载

当部署为 JAR 时，使用 `ScipSolver.loadLibraryInJar()` 加载打包的原生库：

```kotlin
ScipSolver.loadLibraryInJar()  // 从 JAR 资源加载原生库
```

对于本地开发环境中系统安装的 SCIP，库将通过 JNA 自动加载。

### 基本求解

```kotlin
val solver = ScipLinearSolver(
    config = SolverConfig(time = 60.seconds)
)
val result: Ret<SolveReport<Flt64>> = solver(model)
```

### 并发求解

```kotlin
val solver = ScipLinearSolver(
    config = SolverConfig(
        threadNum = UInt64(4),
        backendConfiguration = SCIPSolverConfig(presolve = true)
    )
)
```

### 使用回调

```kotlin
val solver = ScipLinearSolver(
    config = SolverConfig(),
    callBack = ScipSolverCallBack()
        .configuration { status, scipModel, variables, constraints ->
            // 在此配置 SCIP 参数
            ok
        }
        .analyzingSolution { status, scipModel, variables, constraints ->
            // 求解后分析
            ok
        }
)
```

### 约束规划能力边界

`ScipConstraintProgrammingSolver` 接收 portable CP snapshot，并将已支持的约束族编译到 SCIP 模型。fixed interval、optional interval 和 variable duration 均以 `ExactLowering` 暴露；编译器保留 presence、start/size/end、资源约束和全局约束语义，并将辅助产物映射回源 ID。本插件不声明 SCIP 原生 optional interval、variable-duration job、JSCIP 增量 session 或 native checkpoint/resume。超出能力边界时返回结构化 capability 错误，core 已声明的场景仍可使用 Fake/MIP-backed fallback。

### SolveReport 迁移与后续范围

`Ret<SolveReport<Flt64>>` 是主结果契约。请使用 `SolverConfig(time = ...)`、`threadNum` 和类型化的
`backendConfiguration = SCIPSolverConfig(...)`；`timeLimit` 与 `concurrentConfig` 构造参数不属于
1.1.0 源码线。其余插件迁移和能力门禁见 [`plans/solver_cp.md`](../../plans/solver_cp.md)。

## 依赖

| 依赖 | 作用域 | 说明 |
|------|--------|------|
| `ospf-kotlin-core` | compile | 求解器抽象接口 |
| `jscip` | provided | SCIP Java 绑定（未捆绑） |

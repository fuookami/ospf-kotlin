# ospf-kotlin-core-plugin-mindopt

:us: [English](README.md) | :cn: 简体中文

## 简介

OSPF Kotlin 框架的 MindOPT（达摩院求解器）插件模块。本模块提供了将核心求解器抽象层桥接到阿里巴巴达摩院开发的 [MindOPT](https://opt.aliyun.com) 优化求解器的具体实现。

## 功能特性

| 能力 | 状态 |
|------|------|
| 线性规划 (LP) | :white_check_mark: |
| 混合整数线性规划 (MILP) | :white_check_mark: |
| 二次规划 (QP) | :white_check_mark: |
| 混合整数二次规划 (MIQP) | :white_check_mark: |
| 列生成 | :white_check_mark: |
| Benders 分解 | :white_check_mark: |
| 远程服务器连接 | :x: |

## 架构

```
┌────────────────────────────────────────────────────┐
│  MindOPTLinearSolver / MindOPTQuadraticSolver      │  公共 API
├────────────────────────────────────────────────────┤
│  MindOPTColumnGenerationSolver                     │  列生成
│  MindOPTBendersDecompositionSolver                 │  Benders 分解
├────────────────────────────────────────────────────┤
│  MindOPTSolverCallBack                             │  回调管理
│  MindOPTVariable / MindOPTConstraint               │  类型映射
├────────────────────────────────────────────────────┤
│  MindOPTSolver (abstract)                          │  基类——init、solve、analyzeStatus
│  PluginSolverAsync                                 │  协程作用域
└────────────────────────────────────────────────────┘
```

## 文件结构

| 文件 | 说明 |
|------|------|
| `MindOPTSolver.kt` | 抽象基类——环境初始化、求解和状态分析 |
| `MindOPTLinearSolver.kt` | 线性求解器实现 |
| `MindOPTQuadraticSolver.kt` | 二次求解器实现 |
| `MindOPTColumnGenerationSolver.kt` | 列生成策略（LP 松弛 + 对偶解提取） |
| `MindOPTBendersDecompositionSolver.kt` | Benders 分解策略 |
| `MindOPTSolverCallBack.kt` | 回调管理器——配置、解分析、失败处理 |
| `MindOPTVariable.kt` | 变量类型映射（Binary、Integer、Continuous） |
| `MindOPTConstraint.kt` | 约束类型映射 |
| `PluginSolverAsync.kt` | 异步求解协程作用域 |

## 使用

### 依赖

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-mindopt:1.1.0")
```

> **注意：** MindOPT 原生库（`mindopt`）需要以 `provided` 作用域单独提供。请确保 MindOPT 运行时已安装且许可证有效。

### 基本求解

```kotlin
val solver = MindOPTLinearSolver(
    config = SolverConfig(timeLimit = 60.seconds)
)
val result: Ret<FeasibleSolverOutput<Flt64>> = solver(model)
```

### 使用回调

```kotlin
val solver = MindOPTLinearSolver(
    config = SolverConfig(),
    callBack = MindOPTSolverCallBack()
        .configuration { status, mindoptModel, variables, constraints ->
            // 在此配置 MindOPT 参数
            ok
        }
        .analyzingSolution { status, mindoptModel, variables, constraints ->
            // 求解后分析
            ok
        }
)
```

## 依赖

| 依赖 | 作用域 | 说明 |
|------|--------|------|
| `ospf-kotlin-core` | compile | 求解器抽象接口 |
| `mindopt` | provided | MindOPT Java SDK（未捆绑） |
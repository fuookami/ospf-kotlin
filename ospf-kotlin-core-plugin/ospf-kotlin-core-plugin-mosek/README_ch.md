# ospf-kotlin-core-plugin-mosek

:us: [English](README.md) | :cn: 简体中文

## 简介

OSPF Kotlin 框架的 MOSEK 求解器插件模块。本模块提供了将核心求解器抽象层桥接到 [MOSEK](https://www.mosek.com) 的具体实现，MOSEK 是专门用于锥优化（线性、二次、半定规划）的求解器。

> **:construction: 状态：部分实现**
>
> 本模块目前正在开发中。仅实现了 `analyzeStatus` 功能。`init` 和 `solve` 方法返回"尚未实现"。完整的 LP 和 QP 功能将在后续版本中添加。

## 功能特性

| 能力 | 状态 |
|------|------|
| 线性规划 (LP) | :construction:（部分） |
| 二次规划 (QP) | :x: |
| 列生成 | :x: |
| Benders 分解 | :x: |

## 架构

```
┌────────────────────────────────────────────────────┐
│  MosekLinearSolver                                 │  公共 API（部分）
├────────────────────────────────────────────────────┤
│  MosekSolverCallBack                               │  回调管理
├────────────────────────────────────────────────────┤
│  MosekSolver (abstract)                            │  基类——init/solve："尚未实现"
│                                                     │  analyzeStatus：可用
└────────────────────────────────────────────────────┘
```

## 文件结构

| 文件 | 说明 |
|------|------|
| `MosekSolver.kt` | 抽象基类——部分实现（init/solve 返回"尚未实现"，仅 analyzeStatus 可用） |
| `MosekLinearSolver.kt` | 线性求解器占位符 |
| `MosekSolverCallBack.kt` | 回调管理器——配置、解分析、失败处理 |

## 使用

### 依赖

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-mosek:1.1.0")
```

> **注意：** MOSEK 原生库（`mosek`）需要以 `provided` 作用域单独提供。请确保 MOSEK 运行时已安装且许可证有效。

> **:construction: 警告：** 本模块尚未准备好用于生产环境。求解功能未实现。

## 依赖

| 依赖 | 作用域 | 说明 |
|------|--------|------|
| `ospf-kotlin-core` | compile | 求解器抽象接口 |
| `mosek` | provided | MOSEK Java SDK（未捆绑） |
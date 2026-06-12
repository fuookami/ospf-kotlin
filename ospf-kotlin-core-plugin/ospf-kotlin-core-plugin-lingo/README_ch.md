# ospf-kotlin-core-plugin-lingo

:us: [English](README.md) | :cn: 简体中文

## 简介

OSPF Kotlin 框架的 LINGO 求解器插件模块。本模块将提供将核心求解器抽象层桥接到 LINDO Systems 开发的 [LINGO](https://www.lindo.com) 优化建模求解器的具体实现。

> **:construction: 状态：占位符模块**
>
> 本模块目前为占位符，未实现任何功能。所有求解器能力已规划但尚未实现。模块结构已定义以供未来开发。

## 功能特性

| 能力 | 状态 |
|------|------|
| 线性规划 (LP) | :construction: |
| 混合整数线性规划 (MILP) | :construction: |
| 二次规划 (QP) | :construction: |
| 混合整数二次规划 (MIQP) | :construction: |
| 列生成 | :construction: |
| Benders 分解 | :construction: |

## 架构

```
┌────────────────────────────────────────────────────┐
│  LingoLinearSolver                                 │  占位符
└────────────────────────────────────────────────────┘
```

## 文件结构

| 文件 | 说明 |
|------|------|
| `LingoLinearSolver.kt` | 空占位符类——尚未实现 |

## 使用

### 依赖

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-lingo:1.1.0")
```

> **注意：** 实现完成后，LINGO 原生库（`lingo`）需要以 `provided` 作用域单独提供。

> **:construction: 警告：** 本模块尚未准备好使用。未实现任何求解功能。

## 依赖

| 依赖 | 作用域 | 说明 |
|------|--------|------|
| `ospf-kotlin-core` | compile | 求解器抽象接口 |
| `lingo` | provided | LINGO Java SDK（未捆绑） |
# ospf-kotlin-core-plugin-optverse

:us: [English](README.md) | :cn: 简体中文

## 简介

OSPF Kotlin 框架的 OPTVerse 求解器插件模块。本模块将提供将核心求解器抽象层桥接到华为云优化服务 [OPTVerse](https://www.huaweicloud.com/product/optverse.html) 的具体实现。

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
│  OPTVerseLinearSolver                              │  占位符
└────────────────────────────────────────────────────┘
```

## 文件结构

| 文件 | 说明 |
|------|------|
| `OPTVerseLinearSolver.kt` | 空占位符类——尚未实现 |

## 使用

### 依赖

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-optverse:1.1.0")
```

与其他求解器插件不同，OPTVerse 使用华为云 SDK 作为编译依赖：

```kotlin
dependencies {
    implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-optverse:1.1.0")
    // 华为云 SDK 已作为编译依赖捆绑
}
```

> **:construction: 警告：** 本模块尚未准备好使用。未实现任何求解功能。

## 依赖

| 依赖 | 作用域 | 说明 |
|------|--------|------|
| `ospf-kotlin-core` | compile | 求解器抽象接口 |
| `com.huaweicloud.sdk:huaweicloud-sdk-optverse` | compile | 华为云 OPTVerse SDK（已捆绑） |
| `com.huaweicloud.sdk:huaweicloud-sdk-core` | compile | 华为云 SDK 核心（已捆绑） |
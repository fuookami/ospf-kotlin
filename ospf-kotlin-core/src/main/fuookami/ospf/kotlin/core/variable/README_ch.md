# variable — 变量定义包

:us: [English](README.md) | :cn: 简体中文

## 概述

`variable` 包定义了 OSPF（Open Solver Platform Framework）中数学优化模型的**变量体系**。它提供了变量项（Variable Item）的抽象基类、变量类型系统、变量组合容器、独立变量项以及变量范围定义，是构建优化模型的基础设施层。

## 包结构

```
variable/
├── AbstractVariableItem.kt    # 变量项抽象基类
├── AnyVariable.kt             # 通用变量类型
├── Type.kt                    # 变量类型系统
├── VariableCombinationItem.kt # 基于多维数组的变量组合项
├── VariableIndependentItem.kt # 独立变量项
└── VariableRange.kt           # 变量范围定义
```

## 核心概念

### 变量类型系统 (`Type.kt`)

定义了完整的变量类型层次结构，通过密封类（sealed class）确保类型安全：

- **`Binary`** — 二值变量 {0, 1}（底层类型 `UInt8`）
- **`Ternary`** — 三值变量 {0, 1, 2}（`UInt8`）
- **`BalancedTernary`** — 平衡三值变量 {-1, 0, 1}（`Int8`）
- **`Percentage`** — 百分比变量 [0, 1]（`Flt64`）
- **整数类型** — `Int8` ~ `Int256`、`UInt8` ~ `UInt256`
- **连续类型** — `Flt32`、`Flt64`

类型通过以下接口分类：
- `IntegerVariableType<T>` — 有符号整数变量
- `UIntegerVariableType<T>` — 无符号整数变量
- `ContinuesVariableType<T>` — 有符号连续变量
- `UContinuesVariableType<T>` — 无符号连续变量

每种类型提供 `minimum`、`maximum` 边界以及 `RealNumberConstants<T>` 常量定义。

### 变量项 (`AbstractVariableItem.kt`)

所有变量项的抽象基类，定义了变量的核心属性：
- `key` — 变量唯一键（`VariableItemKey`）
- `name` — 变量名称
- `type` — 变量类型（`VariableType<*>`）
- `lowerBound` / `upperBound` — 变量边界
- `fixedValue` — 固定值（用于变量固定策略）

### 独立变量项 (`VariableIndependentItem.kt`)

独立的单一变量项实现，通常用于模型中的标量决策变量。

### 变量组合项 (`VariableCombinationItem.kt`)

基于 `MultiArray` 的多维变量组合容器。允许以数组形式批量创建和管理变量，支持 1D ~ 4D 以及动态维度。组合中的每个变量项自动获得组引用和索引。

### 变量范围 (`VariableRange.kt`)

定义变量的值域范围，包含下界、上界和固定值信息，用于求解器边界的传递和验证。

## 与其他包的关系

- **token** — `Token` 持有 `AbstractVariableItem` 引用，建立变量与求解器索引的映射
- **symbol** — 中间符号（`IntermediateSymbol`）的求值依赖变量项的求解结果
- **model** — 模型通过变量项注册和管理决策变量
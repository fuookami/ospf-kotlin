# solver/output — 求解输出包

:us: [English](README.md) | :cn: 简体中文

## 概述

`output` 子包定义了 OSPF 框架中**求解结果的数据结构**。提供密封接口和数据类，表示可行解、不可行输出、求解器状态和求解统计信息。

## 包结构

```
output/
├── SolverOutput.kt           # 求解器输出数据结构
├── SolverStatus.kt           # 求解器状态枚举
├── SolvingStatus.kt          # 求解过程状态
└── InfeasibleOutputFields.kt # 不可行输出字段
```

## 核心概念

### SolverOutput (`SolverOutput.kt`)

求解器输出的密封接口层次：

- **`SolverOutput`** — 基础密封接口
- **`UnifiedSolverOutput`** — 统一统计信息（迭代数、节点数、最优界、MIP 间隙、求解时间）
- **`LinearSolverOutput`** — 线性求解器输出标记
- **`QuadraticSolverOutput`** — 二次求解器输出标记

**`FeasibleSolverOutput<V>`** — 可行解输出，包含：
- `obj` / `objValue` — 目标值（Flt64 和 V 类型双视图）
- `solution` — 解向量
- `time` — 求解时间
- `gap` — 最优间隙
- `bestBound` / `bestBoundValue` — 最优界
- `mipGap` — MIP 间隙
- `iterations` / `nodeCount` — 求解器统计

**`LinearInfeasibleSolverOutput`** / **`QuadraticInfeasibleSolverOutput`** — 不可行输出，含 IIS 信息。

### SolverStatus (`SolverStatus.kt`)

求解器状态枚举（最优、不可行、无界、超时等）。

### SolvingStatus (`SolvingStatus.kt`)

长时间求解过程中的回调状态。

### InfeasibleOutputFields (`InfeasibleOutputFields.kt`)

不可行求解输出的特定字段。

## 与其他包的关系

- **solver** — 求解器接口返回 `SolverOutput` 子类型
- **solver/iis** — IIS 结果嵌入在不可行输出类型中
- **solver/value** — `FeasibleSolverOutput` 使用 `IntoValue<V>` 进行类型转换
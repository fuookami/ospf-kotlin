# solver/output — 求解输出包

:us: [English](README.md) | :cn: 简体中文

## 概述

`output` 子包定义求解结果的兼容数据结构。主结果契约位于 `solver.report.SolveReport`；本包保留
求解器状态视图以及显式 IIS 兼容入口所需的物化 artifact。

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

**`SolveReport<V>`** — 唯一主统一结果，包含正交问题状态、终止原因、解/incumbent、证明、
统计、诊断、provenance 和指纹。

**`LinearInfeasibleSolverOutput`** / **`QuadraticInfeasibleSolverOutput`** — 不可行输出，含 IIS 信息。`iisAvailable=false` 表示 IIS 编排失败，`iis` 仅为原模型快照，具体失败原因保存在 `diagnostics.errors` 中；`withIIS()` 此时返回空 IIS。

### SolverStatus (`SolverStatus.kt`)

求解器状态枚举（最优、不可行、无界、超时等）。

### SolvingStatus (`SolvingStatus.kt`)

长时间求解过程中的回调状态。

### InfeasibleOutputFields (`InfeasibleOutputFields.kt`)

不可行求解输出的特定字段。

## 与其他包的关系

- **solver.report** — 求解器接口返回 `SolveReport<V>`
- **solver** — 显式 IIS 兼容入口返回 `SolverOutput` artifact
- **solver/iis** — IIS 结果嵌入在不可行输出类型中
- **solver/value** — `SolveReport<Flt64>.convertTo(converter)` 使用 `IntoValue<V>` 转换解和诊断值类型

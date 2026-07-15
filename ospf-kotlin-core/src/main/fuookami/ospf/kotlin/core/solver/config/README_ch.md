# solver/config — 求解器配置包

:us: [English](README.md) | :cn: 简体中文

## 概述

`config` 子包定义了 OSPF 框架中各种求解器后端的配置接口和实现。配置控制模型转储行为、求解器特定参数和性能调优选项。

## 包结构

```
config/
├── SolverConfig.kt       # 通用求解器配置接口
├── CoptSolverConfig.kt   # COPT 求解器配置
├── GurobiSolverConfig.kt # Gurobi 求解器配置
└── SCIPSolverConfig.kt   # SCIP 求解器配置
```

## 核心概念

### SolverConfig (`SolverConfig.kt`)

通用求解器配置接口，控制模型转储行为：

- `dumpIntermediateModelBounds` — 是否转储中间模型边界
- `dumpIntermediateModelForceBounds` — 是否强制转储边界
- `dumpIntermediateModelConcurrent` — 是否启用并发模型转储
- `dumpMechanismModelConcurrent` — 是否启用并发机制模型转储

### CoptSolverConfig (`CoptSolverConfig.kt`)

COPT 求解器特定配置参数。

### GurobiSolverConfig (`GurobiSolverConfig.kt`)

Gurobi 求解器特定配置参数。

### SCIPSolverConfig (`SCIPSolverConfig.kt`)

SCIP 求解器特定配置参数。

## 与其他包的关系

- **solver** — `LinearSolver` 和 `QuadraticSolver` 接口接受 `SolverConfig` 以控制转储行为
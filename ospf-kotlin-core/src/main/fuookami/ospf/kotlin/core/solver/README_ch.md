# solver — 求解器抽象层

:us: [English](README.md) | :cn: 简体中文

## 概述

`solver` 包是 OSPF 框架的**求解器抽象层**，定义了线性求解器和二次求解器的核心接口、求解输出结构、值类型转换、求解配置、启发式求解算法以及 IIS（Irreducible Infeasible Subsystem）诊断能力。它是连接上层模型与底层求解器插件（Gurobi、CPLEX、SCIP 等）的统一抽象。

## 包结构

```
solver/
├── LinearSolver.kt               # 线性求解器接口
├── QuadraticSolver.kt            # 二次求解器接口
├── CoreSolverAsync.kt            # 异步求解作用域
├── Gap.kt                        # 间隙计算
├── ModelingPreparation.kt        # 模型准备
├── SolveOptions.kt               # 求解选项
├── SolverExt.kt                  # 求解器扩展函数
├── SolverFailureSupport.kt       # 求解失败支持
├── SolverMemoryCleanupSupport.kt # 内存清理支持
├── SolverStatusSupport.kt        # 求解状态支持
├── UnsupportedFeatureNotice.kt   # 不支持特性通知
├── config/                       # 求解器配置
│   ├── SolverConfig.kt           # 通用求解器配置
│   ├── CoptSolverConfig.kt       # COPT 配置
│   ├── GurobiSolverConfig.kt     # Gurobi 配置
│   └── SCIPSolverConfig.kt       # SCIP 配置
├── heuristic/                    # 启发式求解器
│   ├── ParticleSwarmHeuristicSolver.kt  # 粒子群优化
│   ├── Population.kt             # 种群管理
│   ├── Selection.kt / SelectionMode.kt  # 选择策略
│   ├── Cross.kt / CrossMode.kt   # 交叉策略
│   ├── Mutation.kt / MutationMode.kt    # 变异策略
│   ├── Migration.kt              # 迁移策略
│   ├── Normalization.kt          # 归一化
│   ├── Iteration.kt              # 迭代控制
│   └── Policy.kt                 # 策略定义
├── iis/                          # 不可行子系统诊断
│   ├── IISComputingStatus.kt     # IIS 计算状态
│   ├── IISConfig.kt              # IIS 配置
│   ├── Linear.kt                 # 线性 IIS
│   └── Quadratic.kt              # 二次 IIS
├── output/                       # 求解输出
│   ├── SolverOutput.kt           # 求解器输出数据结构
│   ├── SolverStatus.kt           # 求解器状态
│   ├── SolvingStatus.kt          # 求解过程状态
│   └── InfeasibleOutputFields.kt # 不可行输出字段
└── value/                        # 值类型转换
    ├── IntoValue.kt              # 值类型转换接口
    ├── SolveValue.kt             # 求解值
    ├── SolveValueConversionContext.kt  # 转换上下文
    └── SolveValueValidation.kt   # 值验证
```

## 核心概念

### 线性求解器接口 (`LinearSolver.kt`)

`LinearSolver` 接口定义了线性规划求解的完整能力：

**核心求解方法**：
- `invoke(model, callback)` — 求解线性模型，返回 `FeasibleSolverOutput<Flt64>`
- `invoke(model, solutionAmount, callback)` — 求解多个解
- `solve(model, converter, callback)` — 泛型求解，支持任意数值类型 V

**全链路求解**：
```
MetaModel<V> → dump → MechanismModel<Flt64> → dump → LinearTriadModel → invoke → SolverOutput
```

**异步支持**：
- `solveAsync(...)` — 返回 `CompletableFuture`，基于协程作用域

**IIS 诊断**：
- `invoke(model, callback, iisConfig)` — 求解并进行不可行子系统分析

### 二次求解器接口 (`QuadraticSolver.kt`)

与 `LinearSolver` 对称，处理二次规划问题，使用 `QuadraticTetradModel` 作为求解器输入。

### 求解输出 (`output/SolverOutput.kt`)

密封接口层次：

```
SolverOutput
├── UnifiedSolverOutput     — 统一统计信息（迭代数、节点数、最优界、间隙、时间）
├── LinearSolverOutput      — 线性求解器输出
└── QuadraticSolverOutput   — 二次求解器输出

FeasibleSolverOutput<V>     — 可行解输出（目标值、解、统计信息）
LinearInfeasibleSolverOutput  — 线性不可行输出（含 IIS）
QuadraticInfeasibleSolverOutput — 二次不可行输出（含 IIS）
```

`FeasibleSolverOutput<V>` 同时提供 Flt64 和 V 类型的双视图访问：
- `obj` / `objValue` — 目标值
- `possibleBestObj` / `possibleBestObjValue` — 可能的最优目标值
- `bestBound` / `bestBoundValue` — 最优界

### 值类型转换 (`value/IntoValue.kt`)

`IntoValue<V>` 接口是求解器边界的核心转换机制：

- `intoValue(Flt64) → V` — Flt64 转换为泛型值类型
- `fromValue(V) → Flt64` — 泛型值类型转换回 Flt64
- `zero` / `one` — V 类型常量
- `IntoValue.Identity` — Flt64 恒等转换器

### 求解器配置 (`config/`)

- `SolverConfig` — 通用配置接口，控制模型转储行为（并发、边界约束等）
- `CoptSolverConfig` — COPT 求解器专用配置
- `GurobiSolverConfig` — Gurobi 求解器专用配置
- `SCIPSolverConfig` — SCIP 求解器专用配置

### 启发式求解器 (`heuristic/`)

内置的元启发式求解框架，包含：

- **粒子群优化** (`ParticleSwarmHeuristicSolver`) — PSO 算法实现
- **种群管理** (`Population`) — 个体集合的管理
- **选择策略** (`Selection`, `SelectionMode`) — 轮盘赌、锦标赛等
- **交叉策略** (`Cross`, `CrossMode`) — 基因交叉操作
- **变异策略** (`Mutation`, `MutationMode`) — 基因变异操作
- **迁移策略** (`Migration`) — 种群间个体迁移
- **迭代控制** (`Iteration`) — 迭代次数和收敛条件

### IIS 诊断 (`iis/`)

不可约不可行子系统（Irreducible Infeasible Subsystem）分析：

- `IISConfig` — IIS 计算配置
- `IISComputingStatus` — 计算状态枚举
- 线性/二次 IIS 模型视图，标识导致不可行的最小约束子集

## 与其他包的关系

- **model** — 求解器消费 `LinearTriadModel` / `QuadraticTetradModel` 作为输入
- **token** — 求解器通过 `TokenList.setSolverSolution(Flt64)` 写入求解结果
- **variable** — 求解器通过 Token 间接访问变量的类型和边界
- **symbol** — `IntoValue` 接口被中间符号求值广泛使用
# model/intermediate — 标准形式模型层

:us: [English](README.md) | :cn: 简体中文

## 概述

`intermediate` 子包是 OSPF 框架的**标准形式模型层**。它将机制模型转换为求解器可消费的稀疏矩阵表示——线性三元组模型（LP 标准形式）和二次四元组模型（QP 标准形式）。

## 包结构

```
intermediate/
├── LinearTriadModel.kt              # 线性三元组模型（LP 标准形式）
├── QuadraticTetradModel.kt          # 二次四元组模型（QP 标准形式）
├── LinearTriadDumpBuilders.kt       # 线性三元组模型构建器
├── LinearTriadElasticBuilder.kt     # 弹性约束构建器（线性）
├── QuadraticTetradDumpBuilders.kt   # 二次四元组模型构建器
├── QuadraticTetradElasticBuilder.kt # 弹性约束构建器（二次）
├── DumpHelpers.kt                   # 转储辅助工具
├── SparseMatrix.kt                  # 稀疏矩阵实现
├── Cell.kt                          # 矩阵单元格
├── BatchDispatchPolicy.kt           # 批量分派策略
├── MemoryCleanupPolicy.kt           # 内存清理策略
├── TriadDualSolverSupport.kt        # 对偶求解支持
├── IntermediateModelDumpingStatus.kt # 中间模型转储状态
└── MechanismModelDumpingStatus.kt    # 机制模型转储状态
```

## 核心概念

### LinearTriadModel (`LinearTriadModel.kt`)

**线性三元组模型**是线性规划的标准形式：

```
min/max  c^T x
s.t.     A x {≤,=,≥} b
         l ≤ x ≤ u
```

以稀疏矩阵 `A` 存储约束系数，目标向量 `c`，右端向量 `b` 和变量边界 `l`/`u`。

### QuadraticTetradModel (`QuadraticTetradModel.kt`)

**二次四元组模型**在三元组模型基础上增加二次项，用于二次规划。

### 稀疏矩阵 (`SparseMatrix.kt`)

高效的稀疏矩阵实现，用于存储约束系数矩阵，针对优化模型中的典型稀疏模式进行了优化。

### 弹性构建器

- **`LinearTriadElasticBuilder`** — 自动为不可行约束添加松弛变量，用于弹性求解
- **`QuadraticTetradElasticBuilder`** — 二次模型的相同功能

### 转储构建器

将机制模型转换为标准形式：
- `LinearTriadDumpBuilders` — 线性机制模型 → 线性三元组模型
- `QuadraticTetradDumpBuilders` — 二次机制模型 → 二次四元组模型

### 其他

- **`Cell`** — 稀疏矩阵单元格表示
- **`BatchDispatchPolicy`** — 控制模型转储过程中的批量处理
- **`MemoryCleanupPolicy`** — 控制大型模型处理过程中的内存清理
- **`TriadDualSolverSupport`** — 对偶变量求解支持

## 与其他包的关系

- **model/mechanism** — 中间模型通过转储机制模型产生
- **solver** — 求解器消费 `LinearTriadModel` / `QuadraticTetradModel` 作为输入
- **model/basic** — 使用 `ModelView` 接口进行模型的只读访问
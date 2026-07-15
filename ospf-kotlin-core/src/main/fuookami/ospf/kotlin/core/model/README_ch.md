# model — 优化模型层

:us: [English](README.md) | :cn: 简体中文

## 概述

`model` 包是 OSPF 框架的**核心模型层**，实现了数学优化模型的完整生命周期管理。它采用**四层架构**设计，从用户友好的元模型（MetaModel）逐步转换为求解器可消费的标准形式模型（Triad/Tetrad Model），最终交由求解器求解。

## 四层架构

```
用户定义层                    MetaModel<V>         ← 用户构建模型的地方
    ↓ dump
机制模型层               MechanismModel<V>       ← 展开中间符号、注册约束
    ↓ dump
标准形式层         LinearTriadModel / QuadraticTetradModel  ← 稀疏矩阵形式
    ↓ invoke
求解器层                  SolverOutput           ← 求解结果
```

## 包结构

```
model/
├── basic/        # 基础层 — 接口、枚举、视图类型
├── mechanism/    # 机制层 — 元模型与机制模型
├── intermediate/ # 中间层 — 标准形式模型（三元组/四元组）
└── callback/     # 回调层 — 启发式求解器的回调模型接口
```

## basic/ — 基础层

定义模型体系的基础接口和枚举类型。

### 核心接口 (`Model.kt`)

模型接口层次：

```
Model<V>                    — 基础模型接口（变量注册、目标设置、解管理）
├── LinearModel<V>          — 线性模型（线性约束、线性目标）
├── QuadraticModel<V>       — 二次模型（二次约束、二次目标）
└── ExpressionModel<V>      — 表达式模型（中间符号管理）
```

`Model<V>` 核心能力：
- `add(variable)` — 注册决策变量
- `addObject()` / `minimize()` / `maximize()` — 设置目标函数
- `setSolution()` / `clearSolution()` — 管理求解结果

### 约束与目标

- **`ConstraintPriority`** (`ConstraintPriority.kt`) — 约束优先级枚举（`Mandatory`、`Suggested`、`NiceToHave` 等）
- **`ConstraintSign`** (`ConstraintSign.kt`) — 约束符号（`LessEqual`、`Equal`、`GreaterEqual`）
- **`ObjectCategory`** (`ObjectCategory.kt`) — 目标类别（`Minimum`、`Maximum`）
- **`MultiObject`** (`MultiObject.kt`) — 多目标优化支持

### 模型视图

- **`ModelView`** (`ModelView.kt`) — 模型视图接口，提供模型的只读访问
- **`ExpressionRange`** (`ExpressionRange.kt`) — 表达式值域，包含下界、上界和固定值

### 其他

- **`ModelBuildingStage`** / **`ModelBuildingStatus`** — 模型构建阶段和状态
- **`ModelFileFormat`** — 模型文件格式（LP、MPS 等）
- **`RegistrationStatus`** — 变量/符号注册状态回调

## mechanism/ — 机制模型层

机制模型层是用户与求解器之间的核心桥梁，处理模型的展开、约束构建和目标管理。

### MetaModel (`MetaModel.kt`)

**元模型**是用户构建优化模型的入口。用户在此注册变量、中间符号、约束和目标。

核心能力：
- 继承 `Model<V>`，提供完整的变量和目标管理
- 支持中间符号注册和管理
- 约束输入通过 `MetaConstraint` DSL 构建

### MechanismModel (`MechanismModel.kt`)

**机制模型**是 MetaModel 展开后的产物，是求解器可消费的形式。展开过程包括：
- 中间符号的多项式展开
- 约束的扁平化和标准化
- 目标函数的展开

子类型：
- `LinearMechanismModel<V>` — 线性机制模型
- `QuadraticMechanismModel<V>` — 二次机制模型

### 约束系统

- **`Constraint`** (`Constraint.kt`) — 约束数据结构
- **`MetaConstraint`** (`MetaConstraint.kt`) — 元约束（用户层约束 DSL）
- **`LinearConstraintInput`** (`LinearConstraintInput.kt`) — 线性约束输入
- **`Relation`** (`Relation.kt`) — 约束关系定义
- **`MathInequalityDsl`** / **`MathInequalityFlatten`** — 数学不等式 DSL 和展开

### 目标系统

- **`Object`** (`Object.kt`) — 目标函数定义
- **`SubObject`** (`SubObject.kt`) — 子目标（多目标优化）

### 辅助功能

- **`MechanismModelDumpSupport`** — 模型转储支持
- **`MechanismModelCutSupport`** — 切割平面支持
- **`MechanismModelObjectiveSupport`** — 目标函数支持
- **`MechanismModelFlt64Conversion`** — Flt64 类型转换
- **`MetaModelExportSupport`** — 模型导出支持（LP/MPS 格式）

## intermediate/ — 标准形式模型层

将机制模型转换为求解器可直接消费的稀疏矩阵形式。

### LinearTriadModel (`LinearTriadModel.kt`)

**线性三元组模型**是线性规划的标准形式：

```
min/max  c^T x
s.t.     A x {≤,=,≥} b
         l ≤ x ≤ u
```

以稀疏矩阵形式存储约束系数、目标向量和边界。

### QuadraticTetradModel (`QuadraticTetradModel.kt`)

**二次四元组模型**是二次规划的标准形式，在三元组基础上增加二次项矩阵。

### 稀疏矩阵 (`SparseMatrix.kt`)

高效的稀疏矩阵实现，用于存储约束系数矩阵。

### 转储构建器

- **`LinearTriadDumpBuilders`** — 线性三元组模型的构建器
- **`LinearTriadElasticBuilder`** — 弹性约束构建器（自动松弛）
- **`QuadraticTetradDumpBuilders`** — 二次四元组模型的构建器
- **`QuadraticTetradElasticBuilder`** — 二次弹性约束构建器
- **`DumpHelpers`** — 转储辅助工具

### 其他

- **`Cell`** (`Cell.kt`) — 矩阵单元格
- **`BatchDispatchPolicy`** — 批量分派策略
- **`MemoryCleanupPolicy`** — 内存清理策略
- **`TriadDualSolverSupport`** — 对偶求解支持
- **`IntermediateModelDumpingStatus`** / **`MechanismModelDumpingStatus`** — 转储状态

## callback/ — 回调模型层

为启发式/元启发式求解器提供的回调模型接口。

- **`CallBackModel`** (`CallBackModel.kt`) — 回调模型实现
- **`CallBackModelInterface`** (`CallBackModelInterface.kt`) — 回调模型接口定义

启发式求解器通过回调接口获取目标函数值和约束违反度，而非直接操作稀疏矩阵。

## 数据流示意

```
用户代码
  │
  ▼
MetaModel<V>
  │  add() 变量、符号、约束、目标
  │
  ▼  dump()
MechanismModel<V>
  │  展开中间符号 → 线性/二次多项式
  │  扁平化约束 → 标准不等式
  │
  ▼  dump()
LinearTriadModel / QuadraticTetradModel
  │  稀疏矩阵 A, 向量 b, c, 边界 l/u
  │
  ▼  invoke()
SolverOutput
  │  obj, solution, time, gap, ...
  │
  ▼  setSolution()
TokenTable → Token.result (V?)
```

## 与其他包的关系

- **variable** — 模型通过 `Model.add()` 注册决策变量
- **token** — 模型通过 `TokenTable` 管理变量到求解器索引的映射和求解结果
- **symbol** — 模型注册中间符号，展开时调用符号的多项式转换
- **solver** — 求解器消费标准形式模型（Triad/Tetrad），返回 `SolverOutput`
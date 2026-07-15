# model/basic — 模型基础层

:us: [English](README.md) | :cn: 简体中文

## 概述

`basic` 子包定义了 OSPF 模型体系的**基础接口、枚举和视图类型**。它建立了所有其他模型子包使用的类型层次结构和核心抽象。

## 包结构

```
basic/
├── Model.kt                # 核心模型接口
├── ModelView.kt            # 模型视图接口
├── ExpressionRange.kt      # 表达式值域
├── ConstraintPriority.kt   # 约束优先级枚举
├── ConstraintSign.kt       # 约束符号枚举
├── ObjectCategory.kt       # 目标类别枚举
├── MultiObject.kt          # 多目标支持
├── ModelBuildingStage.kt   # 模型构建阶段
├── ModelBuildingStatus.kt  # 模型构建状态
├── ModelFileFormat.kt      # 模型文件格式（LP、MPS 等）
└── RegistrationStatus.kt   # 注册状态回调
```

## 核心概念

### 模型接口 (`Model.kt`)

层次化的模型接口：

- **`Model<V>`** — 基础接口：变量注册（`add`）、目标设置（`addObject`、`minimize`、`maximize`）、解管理（`setSolution`、`clearSolution`）
- **`LinearModel<V>`** — 线性模型：线性约束和线性目标
- **`QuadraticModel<V>`** — 二次模型：二次约束和二次目标
- **`ExpressionModel<V>`** — 表达式模型：中间符号管理

### 模型视图 (`ModelView.kt`)

模型的只读视图接口，提供安全访问而不具备修改能力。

### 表达式值域 (`ExpressionRange.kt`)

表达式值域，包含：
- `lowerBound` — 下界
- `upperBound` — 上界
- `fixedValue` — 固定值（变量被固定时）

### 枚举类型

- **`ConstraintPriority`** — `Mandatory`、`Suggested`、`NiceToHave` 等
- **`ConstraintSign`** — `LessEqual`、`Equal`、`GreaterEqual`
- **`ObjectCategory`** — `Minimum`、`Maximum`
- **`ModelBuildingStage`** — 构建阶段（注册、转储等）
- **`ModelFileFormat`** — `LP`、`MPS` 等

### 多目标优化 (`MultiObject.kt`)

支持带优先级权重的多目标优化。

## 与其他包的关系

- **model/mechanism** — MetaModel 和 MechanismModel 实现这些接口
- **model/intermediate** — 三元组/四元组模型使用这些视图类型
- **token** — 模型接口引用 `TokenTable` 进行变量管理
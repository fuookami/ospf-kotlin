# model

[English README (README.md)](./README.md)

`model` 包定义了 OSPF Kotlin 中 LP/QP 优化的三层模型流水线。每一层在将用户级模型转换为求解器可用的矩阵表示中承担不同职责。

## 导入路径

```kotlin
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.callback.*
```

## 架构

```
用户代码
    │
    ▼
┌─────────────────────────────────────────┐
│  basic/   — 核心接口与枚举              │  Model, LinearModel, QuadraticModel,
│            （与求解器无关）              │  ConstraintRelation, ObjectCategory,
│                                         │  ExpressionRange, ModelBuildingStage
└──────────────────┬──────────────────────┘
                   │
    ┌──────────────┴──────────────┐
    ▼                             ▼
┌──────────────────┐   ┌──────────────────┐
│ mechanism/       │   │ callback/        │
│ MetaModel →      │   │ CallBackModel    │
│ MechanismModel   │   │ （基于函数）     │
│ （符号层）       │   │ （启发式求解）   │
└────────┬─────────┘   └──────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  intermediate/ — 稀疏矩阵模型           │
│  LinearTriadModel / QuadraticTetradModel│
│  （求解器就绪）                         │
└──────────────────┬──────────────────────┘
                   │
                   ▼
           求解器 (LP/QP)
```

## 子包

| 子包 | 说明 | 文档 |
|---|---|---|
| [basic/](./basic/) | 核心接口（`Model`、`LinearModel`、`QuadraticModel`）、枚举（`ConstraintRelation`、`ObjectCategory`、`ModelBuildingStage`）及类型（`ExpressionRange`、`Variable`、`Objective`） | [EN](./basic/README.md) · [中文](./basic/README_ch.md) |
| [mechanism/](./mechanism/) | 符号模型层：`MetaModel` → `MechanismModel` 流水线，含约束注册、不等式 DSL 和多目标支持 | [EN](./mechanism/README.md) · [中文](./mechanism/README_ch.md) |
| [intermediate/](./intermediate/) | 稀疏矩阵模型层：`LinearTriadModel` / `QuadraticTetradModel`，含约束单元、目标向量和求解器就绪视图 | [EN](./intermediate/README.md) · [中文](./intermediate/README_ch.md) |
| [callback/](./callback/) | 基于函数的模型层：`CallBackModel` / `MultiObjectCallBackModel`，用于启发式求解器，约束和目标以解向量的函数形式求值 | [EN](./callback/README.md) · [中文](./callback/README_ch.md) |

## 各层职责

### basic/

定义与求解器无关的核心接口和枚举，供所有模型类型共享。不包含任何求解器特定逻辑。

### mechanism/

符号层，用户在此注册变量（Token）、使用 DSL 算子（`geq`、`leq`、`eq`）定义约束并设置目标。`MetaModel` 持有符号表达式；`MechanismModel` 将其展平为 Token 表表示。

### intermediate/

将机制模型转换为稀疏矩阵形式（`LinearTriadModel` / `QuadraticTetradModel`），供 LP/QP 求解器直接消费。处理批量调度、内存清理策略和对偶解提取。

### callback/

启发式求解器的替代模型路径。不构建显式矩阵，而是将约束和目标定义为直接对解向量求值的函数。

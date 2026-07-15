# model/callback — 回调模型包

:us: [English](README.md) | :cn: 简体中文

## 概述

`callback` 子包为 OSPF 框架中的启发式和元启发式求解器提供**回调模型接口**。与消费稀疏矩阵模型的标准求解器不同，启发式求解器通过回调函数评估目标和约束。

## 包结构

```
callback/
├── CallBackModelInterface.kt  # 回调模型接口定义
└── CallBackModel.kt           # 回调模型实现
```

## 核心概念

### CallBackModelInterface (`CallBackModelInterface.kt`)

基于回调的模型评估接口定义。启发式求解器使用此接口来：
- 评估候选解的目标函数值
- 评估约束违反度
- 查询变量边界和类型

### CallBackModel (`CallBackModel.kt`)

回调模型接口的实现，将标准模型表示与启发式求解器使用的基于回调的评估范式桥接起来。

## 设计理念

标准求解器（Gurobi、CPLEX、SCIP）操作稀疏矩阵表示（`LinearTriadModel` / `QuadraticTetradModel`）。然而，启发式求解器需要不同的接口，因为它们：
- 生成候选解作为变量赋值
- 需要评估每个候选解的目标和约束
- 可能无法从矩阵表示中获益

回调模型通过提供函数式评估接口来弥合这一差距。

## 与其他包的关系

- **solver/heuristic** — 启发式求解器消费 `CallBackModel` 进行目标和约束评估
- **model/basic** — 回调模型实现基础模型接口
- **token** — 回调模型使用 `TokenTable` 访问变量求解结果
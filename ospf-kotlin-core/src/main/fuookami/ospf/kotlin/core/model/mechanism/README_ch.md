# model/mechanism — 机制模型层

:us: [English](README.md) | :cn: 简体中文

## 概述

`mechanism` 子包是 OSPF 框架中**用户与求解器之间的核心桥梁**。它包含元模型（MetaModel，用户面向的模型构建器）、机制模型（MechanismModel，展平后的求解器就绪模型）以及约束/目标 DSL 系统。该层处理中间符号的展开、约束标准化和目标函数展开。

## 包结构

```
mechanism/
├── MetaModel.kt                      # 用户面向的模型构建器
├── BasicModel.kt                     # 基础模型实现
├── BasicMechanismModel.kt            # 基础机制模型
├── MechanismModel.kt                 # 展平后的机制模型
├── Constraint.kt                     # 约束数据结构
├── MetaConstraint.kt                 # 元约束 DSL
├── LinearConstraintInput.kt          # 线性约束输入
├── Relation.kt                       # 约束关系定义
├── Object.kt                         # 目标函数定义
├── SubObject.kt                      # 子目标（多目标）
├── MathInequalityDsl.kt             # 数学不等式 DSL
├── MathInequalityFlatten.kt         # 数学不等式展开
├── MechanismModelDumpSupport.kt      # 模型转储支持
├── MechanismModelCutSupport.kt       # 切割平面支持
├── MechanismModelObjectiveSupport.kt # 目标函数支持
├── MechanismModelFlt64Conversion.kt # Flt64 类型转换
└── MetaModelExportSupport.kt         # 模型导出（LP/MPS）
```

## 核心概念

### MetaModel (`MetaModel.kt`)

**元模型**是用户构建优化模型的入口。它提供：
- 通过 `Model.add()` 注册变量
- 注册中间符号
- 通过 `MetaConstraint` DSL 构建约束
- 通过 `minimize()` / `maximize()` 设置目标

### MechanismModel (`MechanismModel.kt`)

**机制模型**是通过转储 MetaModel 产生的展平后的求解器就绪形式。展平过程：
1. 将中间符号展开为线性/二次多项式
2. 将约束展平为标准不等式
3. 展开目标函数

子类型：
- `LinearMechanismModel<V>` — 线性机制模型
- `QuadraticMechanismModel<V>` — 二次机制模型

### 约束系统

- **`Constraint`** — 核心约束数据结构，包含优先级、符号和表达式
- **`MetaConstraint`** — 用户面向的约束 DSL，用于声明式构建约束
- **`LinearConstraintInput`** — 线性约束输入结构
- **`Relation`** — 约束关系定义（≤、=、≥）
- **`MathInequalityDsl`** — 数学不等式表达式 DSL
- **`MathInequalityFlatten`** — 不等式表达式的展开逻辑

### 目标系统

- **`Object`** — 目标函数定义，包含类别（最小化/最大化）和表达式
- **`SubObject`** — 多目标优化的子目标，带优先级权重

### 辅助功能

- **`MechanismModelDumpSupport`** — 将机制模型转储为标准形式的支持
- **`MechanismModelCutSupport`** — 切割平面生成支持
- **`MechanismModelObjectiveSupport`** — 目标函数操作支持
- **`MechanismModelFlt64Conversion`** — 为求解器兼容性进行 Flt64 类型转换
- **`MetaModelExportSupport`** — 将模型导出为 LP/MPS 文件格式

## 与其他包的关系

- **model/basic** — 机制模型实现基础模型接口（`Model`、`LinearModel` 等）
- **model/intermediate** — 机制模型被转储为 `LinearTriadModel` / `QuadraticTetradModel`
- **token** — 模型使用 `TokenTable` 进行变量和符号管理
- **symbol** — 中间符号通过机制层注册和展开
# symbol — 中间符号包

:us: [English](README.md) | :cn: 简体中文

## 概述

`symbol` 包是 OSPF 框架的**符号表达式层**，定义了数学优化模型中的中间符号（Intermediate Symbol）体系。中间符号封装了表达式求值逻辑，支持缓存、依赖追踪和边界管理。此外，该包还提供了丰富的函数符号库（`function` 子包），用于构建复杂的约束表达式。

## 包结构

```
symbol/
├── IntermediateSymbol.kt                  # 中间符号核心接口
├── IntermediateSymbolExpressionSupport.kt  # 中间符号表达式支持
├── QuantitySymbolConversion.kt             # 量纲符号转换
├── SolverBoundaryCasts.kt                  # 求解器边界类型转换
├── SymbolCombination.kt                    # 基于多维数组的符号组合容器
├── flatten/
│   └── FlattenUtility.kt                  # 表达式展开工具
└── function/                              # 函数符号库（见 function/README.md）
```

## 核心概念

### 中间符号接口 (`IntermediateSymbol.kt`)

中间符号是数学优化模型中**可求值的符号表达式**，它们不是直接的决策变量，而是由变量和常量组合而成的派生值。

#### 接口层次

```
IntermediateSymbol<V>              — 基础接口
├── LinearIntermediateSymbol<V>    — 线性中间符号（可转换为线性多项式）
└── QuadraticIntermediateSymbol<V> — 二次中间符号（可转换为二次多项式）
```

#### `IntermediateSymbol<V>` 核心能力

| 能力 | 方法/属性 | 说明 |
|------|----------|------|
| 求值 | `prepare()`, `evaluate()` | 根据 TokenTable 和固定值求值 |
| 缓存 | `prepareAndCache()`, `cached`, `flush()` | 缓存求值结果，支持惰性刷新 |
| 依赖追踪 | `dependencies`, `parent` | 追踪符号间的依赖关系 |
| 边界管理 | `range`, `lowerBound`, `upperBound`, `fixedValue` | 表达式值域 |
| 注册 | `registerAuxiliaryTokens()` | 注册辅助 Token 到集合 |
| 序列化 | `toRawString()` | 获取原始字符串表示 |

#### `LinearIntermediateSymbol<V>`

可转换为线性多项式（`LinearPolynomial<V>`）的中间符号。提供：
- `polynomial` — 对应的线性多项式
- `asMutable()` — 获取可变线性多项式表示

#### `QuadraticIntermediateSymbol<V>`

可转换为二次多项式（`QuadraticPolynomial<V>`）的中间符号。提供：
- `polynomial` — 对应的二次多项式
- `asMutable()` — 获取可变二次多项式表示

### 符号组合 (`SymbolCombination.kt`)

基于 `MultiArray` 的多维符号组合容器，用于批量创建和管理中间符号。

- **`SymbolCombination<Sym, S>`** — 符号组合，创建时自动为每个元素设置组引用和索引
- **`QuantitySymbolCombination<Sym, S>`** — 量纲符号组合

提供了 `LinearIntermediateSymbols` 和 `QuadraticIntermediateSymbols` 两个工厂对象，支持 1D ~ 4D 以及动态维度的符号组合创建。同时提供 `map` / `flatMap` 便捷函数用于从可迭代对象创建符号组合。

类型别名一览：
- `LinearExpressionSymbols1~4<V>`, `DynLinearExpressionSymbols<V>`
- `QuadraticExpressionSymbols1~4<V>`, `DynQuadraticExpressionSymbols<V>`
- 以及对应的 `Quantity...` 变体

### 表达式支持 (`IntermediateSymbolExpressionSupport.kt`)

为中间符号提供表达式操作的扩展支持，包括符号间的算术运算和表达式构建。

### 求解器边界转换 (`SolverBoundaryCasts.kt`)

提供求解器边界类型之间的安全转换，处理不同数值类型间的边界值传递。

### 表达式展开 (`flatten/FlattenUtility.kt`)

将中间符号表达式展开为求解器可消费的单项式（Monomial）列表形式，是符号层与模型层之间的桥梁。

## function 子包 — 函数符号库

`function` 子包提供了 30+ 种函数符号，用于在优化模型中构建复杂约束。详见 [function/README.md](function/README.md)。

主要分类：

| 分类 | 函数符号 | 说明 |
|------|---------|------|
| 松弛 | `Slack`, `SlackRange` | 松弛变量和松弛范围 |
| 取整 | `Ceiling`, `Floor`, `Rounding` | 向上/向下/四舍五入取整 |
| 最值 | `Max`, `MinMax`, `QuadraticMin` | 最大值、最小值 |
| 条件 | `If`, `IfIn`, `IfThen` | 条件表达式 |
| 逻辑 | `And`, `Imply`, `OneOf`, `SameAs` | 逻辑运算 |
| 转换 | `Binaryzation`, `BalanceTernaryzation`, `Semi` | 二值化、三值化、半连续 |
| 分段 | `UnivariateLinearPiecewise`, `BivariateLinearPiecewise` | 线性分段函数 |
| 范围 | `InStepRange`, `Masking`, `QuadraticMaskingRange` | 阶梯范围、掩码 |
| 数学 | `Abs`, `Mod`, `Product`, `Sigmoid`, `Sin`, `Cos` | 数学函数 |
| 约束 | `Inequality`, `SatisfiedAmount`, `BigM` | 不等式、满足数量、大M法 |

## 与其他包的关系

- **variable** — 中间符号的求值最终依赖变量项（通过 TokenTable 间接访问）
- **token** — 中间符号通过 `TokenTable` 查询变量求解结果和缓存求值数据
- **model** — 模型将中间符号注册到 TokenTable，符号展开后生成约束
- **solver** — 求解器边界转换确保符号边界正确传递给求解器
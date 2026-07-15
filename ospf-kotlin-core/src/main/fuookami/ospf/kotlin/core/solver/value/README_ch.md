# solver/value — 值类型转换包

:us: [English](README.md) | :cn: 简体中文

## 概述

`value` 子包提供了 OSPF 框架中求解器边界的**值类型转换**机制。它定义了 `IntoValue<V>` 接口，用于在求解器标准类型 `Flt64` 与泛型值类型之间进行转换，以及求解值类型和验证工具。

## 包结构

```
value/
├── IntoValue.kt                    # 值类型转换接口
├── SolveValue.kt                   # 求解值类型
├── SolveValueConversionContext.kt  # 转换上下文
└── SolveValueValidation.kt        # 值验证
```

## 核心概念

### IntoValue (`IntoValue.kt`)

`IntoValue<V>` 接口是求解器边界的核心转换机制：

- `intoValue(Flt64) → V` — 将 Flt64 转换为泛型值类型 V
- `fromValue(V) → Flt64` — 将泛型值类型 V 转换回 Flt64
- `zero` / `one` — V 类型常量（消除不安全的 `Flt64.zero as V` 强制转换）
- `negativeInfinity` / `infinity` — V 类型无穷大值
- `IntoValue.Identity` — Flt64 恒等转换器（无操作）

还提供了从 `Flt64ValueConverter<V>`（math 层）到 `IntoValue<V>`（core 层）的适配器。

### SolveValue (`SolveValue.kt`)

封装求解器结果及其元数据的求解值类型。

### SolveValueConversionContext (`SolveValueConversionContext.kt`)

提供求解过程中值类型转换的配置上下文。

### SolveValueValidation (`SolveValueValidation.kt`)

检查求解值正确性和边界的验证工具。

## 与其他包的关系

- **token** — `Token<V>` 使用 `IntoValue<V>` 提供 V 类型的 `result` 视图
- **symbol** — 中间符号求值使用 `IntoValue<V>` 进行值转换
- **solver/output** — `FeasibleSolverOutput` 提供 `convertTo(converter)` 方法使用 `IntoValue<V>`
- **solver** — 求解器 `solve()` 方法接受 `IntoValue<V>` 进行泛型求解
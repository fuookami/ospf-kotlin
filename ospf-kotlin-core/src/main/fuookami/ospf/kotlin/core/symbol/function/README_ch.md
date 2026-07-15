# function — 函数符号库

:us: [English](README.md) | :cn: 简体中文

## 概述

`function` 子包提供了 OSPF 框架中用于构建优化约束的**函数符号**集合。每种函数符号封装了一类常见的数学/逻辑运算，可直接在模型约束表达式中使用。所有函数符号均实现 `IntermediateSymbol` 接口，支持求值、缓存和边界管理。

## 函数符号一览

### 松弛与范围

| 文件 | 符号 | 说明 |
|------|------|------|
| `Slack.kt` | `Slack` | 松弛变量，将不等式转换为等式 |
| `SlackRange.kt` | `SlackRange` | 松弛变量的范围约束 |
| `InStepRange.kt` | `InStepRange` | 阶梯范围约束 |
| `QuadraticInStepRange.kt` | `QuadraticInStepRange` | 二次阶梯范围约束 |
| `Masking.kt` | `Masking` | 掩码范围，选择性激活/屏蔽变量 |
| `QuadraticMaskingRange.kt` | `QuadraticMaskingRange` | 二次掩码范围 |

### 取整与数学

| 文件 | 符号 | 说明 |
|------|------|------|
| `Ceiling.kt` | `Ceiling` | 向上取整（整除） |
| `Floor.kt` | `Floor` | 向下取整 |
| `Rounding.kt` | `Rounding` | 四舍五入 |
| `Abs.kt` | `Abs` | 绝对值 |
| `Mod.kt` | `Mod` | 取模运算 |
| `Product.kt` | `Product` | 乘积运算 |
| `Sigmoid.kt` | `Sigmoid` | Sigmoid 函数 |
| `Sin.kt` | `Sin` | 正弦函数 |
| `Cos.kt` | `Cos` | 余弦函数 |

### 最值

| 文件 | 符号 | 说明 |
|------|------|------|
| `Max.kt` | `Max` | 最大值 |
| `MinMax.kt` | `MinMax` | 最小值 / 最大值组合 |
| `QuadraticMin.kt` | `QuadraticMin` | 二次最小值 |
| `First.kt` | `First` | 取第一个满足条件的值 |

### 条件与逻辑

| 文件 | 符号 | 说明 |
|------|------|------|
| `If.kt` | `If` | 条件表达式 `if (cond) then a else b` |
| `IfIn.kt` | `IfIn` | 值在集合内的条件判断 |
| `IfThen.kt` | `IfThen` | 蕴含关系 `if A then B` |
| `And.kt` | `And` | 逻辑与 |
| `Imply.kt` | `Imply` | 逻辑蕴含 |
| `OneOf.kt` | `OneOf` | 恰好一个为真（XOR 推广） |
| `SameAs.kt` | `SameAs` | 两变量同真同假 |
| `SatisfiedAmount.kt` | `SatisfiedAmount` | 满足条件的数量 |
| `SatisfiedAmountInequality.kt` | `SatisfiedAmountInequality` | 满足不等式条件的数量 |

### 变量转换

| 文件 | 符号 | 说明 |
|------|------|------|
| `Binaryzation.kt` | `Binaryzation` | 二值化（将连续/整数变量转换为二值表示） |
| `BalanceTernaryzation.kt` | `BalanceTernaryzation` | 平衡三值化 |
| `Semi.kt` | `Semi` | 半连续/半整数变量 |
| `QuadraticLinear.kt` | `QuadraticLinear` | 二次线性化 |

### 分段线性

| 文件 | 符号 | 说明 |
|------|------|------|
| `UnivariateLinearPiecewise.kt` | `UnivariateLinearPiecewise` | 单变量分段线性函数 |
| `BivariateLinearPiecewise.kt` | `BivariateLinearPiecewise` | 双变量分段线性函数 |

### 其他

| 文件 | 符号 | 说明 |
|------|------|------|
| `BigM.kt` | `BigM` | 大M法约束 |
| `Inequality.kt` | `Inequality` | 通用不等式约束 |
| `FunctionSymbol.kt` | `FunctionSymbol` | 函数符号基础接口 |

## 使用示例

函数符号通常在模型定义中通过中间符号表达式使用，例如：

```kotlin
// 创建松弛变量
val slack = Slack("my_slack", lowerBound = Flt64.zero, upperBound = Flt64(100.0))

// 条件约束
val condition = If("is_active", conditionExpr, thenExpr, elseExpr)

// 最大值
val maxVal = Max("max_x_y", listOf(x, y))
```

## 设计原则

- 所有函数符号实现 `IntermediateSymbol<V>` 接口
- 通过 `prepare()` / `evaluate()` 方法求值
- 支持通过 `range` 提供边界信息
- 支持缓存求值结果以提升性能
- 通过 `dependencies` 追踪符号依赖关系
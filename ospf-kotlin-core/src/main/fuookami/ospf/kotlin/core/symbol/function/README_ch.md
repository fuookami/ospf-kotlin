# function — 函数符号库

:us: [English](README.md) | :cn: 简体中文

## 概述

`function` 子包提供了 OSPF 框架中用于构建优化表达式与约束的**函数符号**。清单包含精确 MILP 表达、LP 松弛、依赖目标方向的上图形式，以及分段线性近似；这些模型保证各不相同，具体语义见对应函数文档。函数符号通过中间符号接口提供求值、依赖和边界管理能力。

## 函数符号一览

### 二次输入函数

`FunctionSymbolLifecycle<V>` 共享辅助变量注册契约。线性约束仍由
`MathFunctionSymbolBase<V>` 注册，二次约束由公开的
`QuadraticMathFunctionSymbolBase<V>` 注册；现有调用无需迁移。

新增 `QuadraticMaxFunction`、`QuadraticAbsFunction`、`QuadraticMinMaxFunction`、
`QuadraticMaxMinFunction`、`QuadraticMaskingFunction`、`QuadraticIfFunction`、
`QuadraticIfInFunction`、`QuadraticIfThenFunction` 和 `QuadraticInequalityFunction`。
这些函数实现 `QuadraticIntermediateSymbol`，可直接加入 `QuadraticMetaModel`，
通过 `polynomial` 用于目标或约束，并支持语义求值和依赖注册。
已有 Min、PositivePart、Slack、SlackRange、InStepRange 的二次实现保持不变。

新增函数通过 `QuadraticFunctionSymbol` 显式复用对应线性函数的语义。
每个实际含二次项的输入增加一个可正可负的实桥接变量及一条精确二次等式；仿射输入不增加绑定变量。下游线性形式若使用 Big-M，仍要求源表达式有有限界。
结果用辅助变量表示，Masking 和 IfThen 不会将原始二次输入相乘而产生高阶项。
这种精确组合可能产生非凸 MIQCP，需要支持非凸二次约束的求解器，不保证凸性或性能。

输入必须能推导出有限上下界，不会以任意默认 Big-M 掩盖未知范围。
范围首次用于构造函数后不得扩大，扩大时注册返回错误；收紧范围仍安全。
Masking 的掩码为 `BinVar`。If/IfIn/IfThen 的比较间隔和 delta、
Inequality 的零容差和严格边界与线性版本一致，间隔内可能返回 null。
IfThen 假分支为零。MinMax/MaxMin 返回精确最值，不依赖目标方向收紧松弛。

二次组合在 MechanismModel 展开，不向线性 native adapter 转发结构。
未添加 PWL/SOS、Sin/Cos/Sigmoid 的二次近似策略。
仅以二值变量为输入的逻辑函数无需复制，其线性约束可直接用于二次模型。

若线性输入函数生成的约束仍是线性的，它也可以注册到二次模型中。
`ProductFunction` 将两个线性表达式展开为二次表达式，不创建乘积结果变量。
`QuadraticLinearFunction` 只在需要复用二次表达式值时，通过等式
`y = p(x)` 引入可正可负的桥接变量；它不是 MILP 线性化。
这两类操作分别需要求解器支持二次表达式或 MIQCP 二次等式。

```kotlin
val absolute = QuadraticAbsFunction(
    polynomial = quadraticInput,
    converter = converter,
    name = "absolute"
)
model.add(absolute)
model.minimize(absolute.polynomial)
```

### 乘积、选择、查表与顺序统计

| 文件 | 符号 | 说明 |
|------|------|------|
| `IntegerProduct.kt` | `IntegerProductFunction` | 有界整数变量与有界线性表达式的精确乘积，采用二进制展开和掩码 |
| `Select.kt` | `SelectFunction` / `IfThenElseFunction` | 按二值变量在两个分支值之间精确选择 |
| `McCormickEnvelope.kt` | `McCormickEnvelopeFunction` | 两个有界线性表达式乘积的四平面 LP 松弛 |
| `Element.kt` | `ElementFunction` / `LookupFunction` | 由有界整数索引从表达式表中选择一项 |
| `OrderStatistic.kt` | `ArgMinFunction`、`ArgMaxFunction` | 返回最值的索引；求解时并列可任取，语义求值返回最小索引 |
| `OrderStatistic.kt` | `KthLargestFunction` | 精确返回从 0 开始编号的第 k 大值 |
| `OrderStatistic.kt` | `TopKSumFunction` | 精确求最大的 k 个值之和，要求 `0 <= k <= n` |

### 建模组合与风险

| 文件 | 符号 | 说明 |
|------|------|------|
| `CompositeFunctions.kt` | `PositivePartFunction`、`ClampFunction`、`DeadZoneFunction` | 精确计算正部、闭区间裁剪和 `max(|x| - delta, 0)` |
| `Complementarity.kt` | `ComplementarityFunction` / `MutuallyExclusivePositiveFunction` | 对两个有界非负表达式精确施加至多一个为正的约束 |
| `CompositeFunctions.kt` | `L1DistanceFunction`、`LInfinityDistanceFunction`、`RangeFunction` | 精确计算向量距离以及最大值减最小值 |
| `Cardinality.kt` | `AtMostFunction`、`ExactlyFunction`、`atMostConstraints`、`exactlyConstraints` | 二值变量上限/精确计数，支持结果指示和纯约束行 |
| `Cvar.kt` | `CvarFunction`、`CvarEpigraphFunction` | 精确离散 CVaR 结果，以及仅供最小化/上界场景使用的 LP 上图形式 |

### 非线性函数的分段线性近似

| 文件 | 符号 | 说明 |
|------|------|------|
| `NonlinearPiecewise.kt` | `ExpFunction`、`LogFunction`、`ReciprocalFunction`、`PowerFunction`、`LogisticApproximationFunction` | 指数、对数、倒数、幂与平滑 logistic 函数的分段线性近似 |

这些符号在断点处精确插值，在断点之间近似原函数，不自动提供误差界。
`SigmoidFunction` 仍是离散关系指示函数，不是平滑 logistic 近似。

### 阶梯费率与固定费用

| 文件 | 组合场景 | 说明 |
|------|----------|------|
| `Tariff.kt` | 增量阶梯计费 | 各数量区间分别按所在区间的费率计费 |
| `Tariff.kt` | 全量折扣 | 按所选档位的费率对全部数量计费 |
| `Tariff.kt` | 固定启用费 | 激活二值变量为 1 时收取一次设置费 |

这些组合复用单变量分段线性与掩码原语；相应页面会给出 Kotlin 和 Rust 构造示例。

### 约束规划全局约束

| 工厂方法 | 符号 | 说明 |
|----------|------|------|
| `GlobalConstraintFunctions.allDifferent(...)` | `AllDifferent` | 整数表达式两两不同 |
| `GlobalConstraintFunctions.noOverlap(...)` | `NoOverlap` | 半开区间互不重叠 |
| `GlobalConstraintFunctions.cumulative(...)` | `Cumulative` | 任意时刻的总需求不超过共享容量 |

工厂返回现有 CP 约束 AST。CP 求解器可原生处理其支持的全局约束；
MIP lowering 要求有限整数域或时域，`Cumulative` 还需要显式开启 lowering policy。

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
| `Product.kt` | `ProductFunction` | 两个线性表达式展开成二次表达式；无独立结果变量，也不做 MILP 线性化 |
| `Sigmoid.kt` | `SigmoidFunction` | 边界间隔外返回 0/1 的关系指示函数；不是平滑 logistic 曲线 |
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
| `If.kt` | `IfFunction` | 关系指示函数：条件真分支为 1，假分支为 0 |
| `IfIn.kt` | `IfIn` | 区间条件：当 a <= x <= b 时 y = 1，否则 y = 0 |
| `IfThen.kt` | `IfThen` | 条件赋值：条件成立时 result = thenPoly，否则为 0 |
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
| `QuadraticLinear.kt` | `QuadraticLinearFunction` | 通过 `y = p(x)` 等式复用二次表达式值的桥接；二次等式会产生 MIQCP |

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

// 按二值变量选择分支值
val selected = SelectFunction(
    mask = active,
    then = thenPolynomial,
    otherwise = otherwisePolynomial,
    converter = converter,
    name = "selected"
)
```

## 设计原则

- 所有函数符号实现 `IntermediateSymbol<V>` 接口
- 通过 `prepare()` / `evaluate()` 方法求值
- 支持通过 `range` 提供边界信息
- 支持缓存求值结果以提升性能
- 通过 `dependencies` 追踪符号依赖关系

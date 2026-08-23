# 约束规划模型

[English](README.md) | 简体中文

## 概述

`constraint_programming` 包是整数约束规划（CP）的 solver-neutral 模型层，提供精确的
`Int64` 值域与表达式、Boolean literal、全局约束和排程约束、独立模型注册表、不可变
snapshot 以及带版本的 JSON codec。

`ConstraintProgrammingModel` 有意独立于线性和二次 `MetaModel` 体系。CP 模型由
`ConstraintProgrammingSolver` 编译；除非所选求解器明确提供精确 lowering，否则不会先被展开为
多项式模型。

```text
OSPF 整数变量
  -> IntegerDomain + ConstraintProgrammingExpression + BooleanLiteral
  -> ConstraintProgrammingConstraint + IntervalVariable
  -> ConstraintProgrammingModel
  -> ConstraintProgrammingModelSnapshot
  -> backend compiler 或 exact lowerer
```

本包不依赖 OR-Tools。SCIP、MIP-backed 等 solver adapter 与模型 AST 相互独立。

## 快速开始

下面的模型让三个整数变量分别取 `0..2` 中的不同值，并最小化第一个变量。factory 和注册方法都
返回 `Ret`；必须传播每个失败，并关闭未交给求解器的模型。

```kotlin
import fuookami.ospf.kotlin.core.model.constraint_programming.*
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.core.variable.IntVar
import fuookami.ospf.kotlin.utils.functional.*

fun buildModel(): Ret<ConstraintProgrammingModel> {
    val model = ConstraintProgrammingModel(name = "all-different")
    var completed = false
    try {
        val domain = when (val result = IntegerDomain.interval(0, 2)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val variables = List(3) { index -> IntVar("value-$index") }
        for (variable in variables) {
            when (val result = model.registerVariable(variable, domain)) {
                is Ok -> Unit
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        val expressions = variables.map { variable ->
            ConstraintProgrammingExpression.Variable(variable, domain)
        }
        val constraint = when (
            val result = ConstraintProgrammingConstraint.allDifferent(expressions)
        ) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (
            val result = model.addConstraint(
                constraint = constraint,
                id = ConstraintId("values-all-different"),
                name = "values all different"
            )
        ) {
            is Ok -> Unit
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (
            val result = model.minimize(
                expression = expressions.first(),
                id = ObjectiveId("minimize-first"),
                name = "minimize first value"
            )
        ) {
            is Ok -> Unit
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (val result = model.validate()) {
            is Ok -> Unit
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        completed = true
        return ok(model)
    } finally {
        if (!completed) {
            model.close()
        }
    }
}
```

将返回的模型交给 `ConstraintProgrammingSolver`，并在求解后关闭模型。当模型需要重复编译、
序列化或发送到远程求解器时，使用 `snapshot()`。

## 核心类型

| 类型 | 作用 |
| --- | --- |
| [`IntegerDomain`](IntegerDomain.kt) | 连续或稀疏的精确整数值域 |
| [`ConstraintProgrammingExpression`](ConstraintProgrammingExpression.kt) | 常量、整数变量引用和整数线性表达式 |
| [`BooleanLiteral`](BooleanLiteral.kt) | 二值变量的正/负 literal 和 Boolean 常量 |
| [`ConstraintProgrammingConstraint`](ConstraintProgrammingConstraint.kt) | solver-neutral 约束 AST 和赋值求值器 |
| [`IntervalVariable`](IntervalVariable.kt) | 固定/可变 duration、必选/optional 的排程 interval |
| [`ConstraintProgrammingModel`](ConstraintProgrammingModel.kt) | 有序注册表和生命周期边界 |
| [`ConstraintProgrammingModelSnapshot`](ConstraintProgrammingModelSnapshot.kt) | 可重复编译的冻结模型视图 |
| [`ConstraintProgrammingSnapshotCodec`](ConstraintProgrammingSnapshotCodec.kt) | 严格、带版本的 JSON 传输 codec |
| [`ConstraintGroupRegistry`](ConstraintGroupRegistry.kt) | framework pipeline 使用的约束组集成边界 |

## 整数值域

CP 值使用精确的有符号 64 位整数。系统不会隐式接受浮点值；业务小数必须显式缩放为整数，并检查
缩放结果的范围。

建模输入应使用带校验的 factory：

```kotlin
val contiguous = IntegerDomain.interval(-5, 10)
val sparse = IntegerDomain.values(listOf(-3, 0, 4, 4)) // 排序并去重
val fixed = IntegerDomain.singleton(7)
val binary = IntegerDomain.boolean                    // {0, 1}
```

当 cardinality 未知或超过配置门禁时，`IntegerDomain.enumerate()` 返回 `null`，避免 fake solver
和 exact lowerer 意外展开过大的值域。

未提供值域时，二值变量默认使用 `{0, 1}`，无符号整数变量默认使用 `0..Long.MAX_VALUE`，有符号
整数变量默认使用完整 `Long` 范围。这些默认值在语义上合法，但通常不适合 lowering 和枚举；应优先
声明紧凑的显式值域。

## 表达式与 Literal

`ConstraintProgrammingExpression` 只支持：

- `Constant`：`Int64` 常量。
- `Variable`：OSPF 整数变量引用及其 CP 值域。
- `Linear`：整数系数和整数常数项。
- `Invalid`：延迟的构造错误，通常由取负溢出产生。

使用 `variable`、`term`、`linear` 和 `sum` 进行带校验的构造。表达式 `+` 和 `-` 返回
`Ret<Linear>`，因为系数或常数项归一化可能溢出。赋值缺失时，`evaluate()` 返回结构化错误；
变量引用求值还会校验其声明值域，线性表达式求值则使用受检累加。

`BooleanLiteral` 只接受 `BinVariable`、否定标记或 Boolean 常量：

```kotlin
val selected = binaryVariable.literal
val notSelected = binaryVariable.negatedLiteral
val alsoNotSelected = !selected
val fixedTrue = BooleanLiteral.True
```

CP 解映射中的 Boolean 赋值使用 `0` 和 `1`；任何其它值都会产生求值错误。

## 约束族

应优先使用 companion factory，而不是直接构造约束 data class。

| 约束族 | 类型与语义 |
| --- | --- |
| 整数比较 | `IntegerComparison`；使用 `equal`、`lessOrEqual`、`greaterOrEqual`，或以 `Int64` 为 rhs 的 `eq`/`leq`/`geq` |
| Boolean 逻辑 | `Literal`、`BoolAnd`、`BoolOr` 和 `BoolXor`；XOR 表示恰好一个 literal 为真 |
| 条件逻辑 | `Implication`，以及方向为 `Implies`、`ImpliedBy` 或 `Equivalent` 的 `Reified` |
| 全异 | 一个或多个整数表达式上的 `AllDifferent` |
| 索引选择 | `Element`；索引从 0 开始，被选元素可以是整数或表达式 |
| 表约束 | `AllowedAssignments` 和 `ForbiddenAssignments`；每个 tuple 的 arity 必须等于表达式数量 |
| 排程 | 注册 interval 上的 `NoOverlap` 和 `Cumulative` |
| 路由 | `Circuit`；successor 必须构成从索引 `0` 出发并访问全部节点的单一置换回路 |
| 序列 | `Automaton`；每个 `(fromState, value)` 对应的转移必须确定且唯一 |
| 库存/资源液位 | `Reservoir`；事件按非递减时间应用，液位必须始终位于边界内 |

每个约束都会暴露其引用的 `VariableId` 集合和 `isSatisfied()` oracle。Fake 求解、远程结果校验
和 differential test 使用这一 oracle；backend compiler 必须保持相同语义。

空 Boolean 集合遵循数学常量语义：`BoolAnd` 为真，`BoolOr` 和“恰好一个”为真的 `BoolXor`
为假。`AllDifferent`、`NoOverlap`、`Cumulative`、`Circuit`、`Automaton` 和 `Reservoir`
等其它 factory 会拒绝必填输入为空。

## Interval 与排程

`IntervalVariable` 包含 `start`、`size`、`end` 和可选的 presence literal。存在的 interval
必须满足 `end = start + size`，且 size 不得为负。size 可以是常量或整数表达式，因此 AST 同时
能够表达固定 duration 和 variable duration。

排程采用半开区间 `[start, end)`。首尾相接的 interval 不重叠，零 duration interval 不占用
时间。optional interval 缺席时，求值返回规范值
`(start=0, size=0, end=0, present=false)`，排程约束会忽略它。

使用以下入口：

- `IntervalVariable.fixed(...)`：构造经过校验的固定 duration interval。
- `IntervalVariable.create(...)`：构造 duration 为表达式的 interval。
- `NoOverlap.create(...)`：禁止 interval 两两重叠。
- `Cumulative.create(...)`：约束非负 demand 不超过非负 capacity。

通过 `model.registerInterval(interval)` 注册每个 interval。interval 表达式和 presence literal
引用的所有标量变量也必须注册到模型中。

AST 能够表达某种 interval，并不意味着所有 backend 都支持它。optional interval 和 variable
duration 的编译尤其依赖 capability。

## 模型生命周期

`ConstraintProgrammingModel` 保留插入顺序，并拒绝重复的变量、interval、表达式、约束和目标
标识。正常生命周期如下：

1. 注册标量变量及其值域。
2. 注册 interval 和可选的具名表达式。
3. 添加约束，建议使用显式 ID。
4. 添加最小化或最大化目标，建议使用显式 ID。
5. 调用 `validate()` 或 `snapshot()`。
6. 求解或序列化模型。
7. 调用 `close()` 释放注册表；关闭后的模型不可复用。

模型实现了 `ConstraintGroupRegistry`。调用 `registerConstraintGroup()` 会为后续约束设置当前组，
除非调用 `addConstraint()` 时显式传入其它组。

`snapshot()` 会先验证所有被引用变量均已注册，然后按注册顺序复制变量、interval、具名表达式、
约束、目标和约束组名称。结果不持有 solver 或 native handle，可以被重复编译。

## Snapshot 传输

`ConstraintProgrammingSnapshotCodec` 将 snapshot 编码为带 schema 版本的 JSON。解码方必须提供
`Map<VariableId, AbstractVariableItem<*, *>>`；传输协议不会序列化发送方的变量对象。

```kotlin
val encoded: Ret<String> = ConstraintProgrammingSnapshotCodec.encode(snapshot)
val decoded: Ret<ConstraintProgrammingModelSnapshot> =
    ConstraintProgrammingSnapshotCodec.decode(encodedJson, variableBindings)
```

当前 codec 采用严格模式：未知字段、不支持的 schema、变量 binding 缺失、非法值域、畸形约束和
不确定的 Automaton 转移都会返回结构化错误。snapshot payload 是可移植模型描述，不是 native
solver checkpoint。

`diagnosticActivations()` 会为原始约束、变量上下界和非 Boolean 稀疏值域生成稳定 activation。
solver 生成的辅助元素不得泄漏到该公共证据清单中。

## 身份边界

模型需要重建、序列化、诊断或参与 Benders 分解时，应显式提供 `ConstraintId`、`ObjectiveId`
和 `IntervalId`。默认约束 ID（`constraint-N`）和目标 ID（`objective-N`）依赖注册顺序。

在当前 API 边界，表达式和 Boolean literal 的变量 ID 由 OSPF variable identity 派生，格式为
`${identifier}:${index}`。显式 `registerVariable(id, variable, domain)` overload 只改变注册表 key，
不会重写表达式中的 ID。因此 registry ID、表达式 ID 和解码 binding key 必须保持一致，否则
`validate()` 或 `decode()` 会失败。不得把 solver 行列索引或展示名称当作跨重建身份。

## Solver Capability

模型 AST 的范围大于任何一个 backend 的支持范围。使用某项能力前，应查询所选 solver 的
descriptor：

```kotlin
val support = solver.descriptor.capabilities.constraintProgrammingSupport(
    ConstraintProgrammingFeature.OptionalInterval
)
```

支持级别为：

- `Native`：编译为 backend 原生约束，或由该 solver 直接处理。
- `ExactLowering`：转换为已经证明等价的 formulation。
- `Unsupported`：solver 必须返回结构化的不支持错误。

Fake solver 是带枚举门禁的确定性契约/测试实现，不是生产搜索引擎。SCIP 和 MIP-backed adapter
各自只支持声明的子集；应读取 runtime descriptor，不能根据 AST class 的存在推断能力。精确 MIP
lowering 还要求安全的有限边界，并可能拒绝超过配置规模门禁的模型。

## 错误处理

建模操作使用 `Ret<T>` 或 `Try`；非法输入不通过业务异常报告。应优先使用以下带校验入口：

- `IntegerDomain.interval`、`IntegerDomain.values` 和 `IntegerDomain.sparse`。
- `ConstraintProgrammingExpression.variable`、`term`、`linear` 和 `sum`。
- `ConstraintProgrammingConstraint` companion factory。
- `IntervalVariable.create`/`fixed`、`NoOverlap.create` 和 `Cumulative.create`。
- 在传输或重复编译前调用 `ConstraintProgrammingModel.validate` 和 `snapshot`。

公开 data class constructor 仍可用于表示 AST，但可能绕过 factory 校验。应原样传播 `Failed`/
`Fatal`，由 backend adapter 在 solver 边界转换 native failure。

## 源码索引

| 文件 | 内容 |
| --- | --- |
| [`IntegerDomain.kt`](IntegerDomain.kt) | 精确值域与受限枚举 |
| [`ConstraintProgrammingExpression.kt`](ConstraintProgrammingExpression.kt) | 整数表达式 AST 与受检运算 |
| [`BooleanLiteral.kt`](BooleanLiteral.kt) | 二值 literal 与 Boolean 求值 |
| [`ConstraintProgrammingConstraint.kt`](ConstraintProgrammingConstraint.kt) | 逻辑、全局、图、序列和 reservoir 约束 |
| [`IntervalVariable.kt`](IntervalVariable.kt) | Interval、NoOverlap 和 Cumulative 语义 |
| [`ConstraintProgrammingModel.kt`](ConstraintProgrammingModel.kt) | 注册表、校验、目标、约束组和生命周期 |
| [`ConstraintProgrammingModelSnapshot.kt`](ConstraintProgrammingModelSnapshot.kt) | 冻结模型与诊断 activation |
| [`ConstraintProgrammingSnapshotCodec.kt`](ConstraintProgrammingSnapshotCodec.kt) | 带版本的 JSON 编解码 |
| [`ConstraintGroupRegistry.kt`](ConstraintGroupRegistry.kt) | framework 约束组集成 |

面向 solver 的 capability 和 SPI 位于
[`core.solver.constraint_programming`](../../solver/constraint_programming/ConstraintProgrammingSolver.kt)。

# ospf-kotlin-math/symbol

[English Documentation (README.md)](./README.md)

OSPF Kotlin 的符号表达式基础与操作。

当前 `symbol` 包有两层表达式体系：

- `symbol.expression.*` 是推荐入口，适合运行时 boolean/scalar expression、`PropertyPath`、求值与规范化。
- `symbol.dsl`、`symbol.parser`、`symbol.serde` 保留为 legacy `Expr` 兼容层，主要服务多项式和不等式转换。
- 需要直接操作旧 AST 时，请显式使用 `legacySymbolExpr`、`parseLegacySymbolExpression`、`legacySymbolExprFromJson`、`toLegacyExpr`、`legacyToCanonicalPolynomial`。

## 核心类型

### 符号

```kotlin
interface Symbol {
    val name: String
    val displayName: String?
}
```

### 单项式

| 类型 | 形式 | 示例 |
|------|------|------|
| `LinearMonomial<T>` | `c * x` | `3x` |
| `QuadraticMonomial<T>` | `c * x * y` | `2xy`, `5x^2` |
| `CanonicalMonomial<T>` | `c * x^n * y^m * ...` | `3x^2y^3` |

### 多项式

| 类型 | 形式 | 示例 |
|------|------|------|
| `LinearPolynomial<T>` | `c0 + c1*x + c2*y + ...` | `1 + 2x + 3y` |
| `QuadraticPolynomial<T>` | `c0 + c1*x + c2*x^2 + c3*xy + ...` | `1 + 2x + 3x^2 + 4xy` |
| `CanonicalPolynomial<T>` | `c0 + Σ ci * Π xj^pj` | `1 + 2x + 3x^2y^3` |

### 不等式

```kotlin
data class LinearInequality<T>(
    val polynomial: LinearPolynomial<T>,
    val comparison: Comparison  // Lt, Le, Eq, Ne, Ge, Gt
)

data class QuadraticInequality<T>(...)
data class CanonicalInequality<T>(...)
```

## 使用示例

### 多项式构造

```kotlin
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.serde.symbolOfSerializedIdentifier
import fuookami.ospf.kotlin.math.algebra.number.Flt64

val x = symbolOfSerializedIdentifier("x")
val y = symbolOfSerializedIdentifier("y")

// 线性多项式
val linear = LinearPolynomial<Flt64>(
    monomials = listOf(
        LinearMonomial(Flt64(2.0), x),
        LinearMonomial(Flt64(3.0), y)
    ),
    constant = Flt64(1.0)
)  // 1 + 2x + 3y

// 规范多项式
val canonical = CanonicalPolynomial<Flt64>(
    monomials = listOf(
        CanonicalMonomial(Flt64(3.0), listOf(x, x)),  // 3x^2
        CanonicalMonomial(Flt64(2.0), listOf(x, y))   // 2xy
    ),
    constant = Flt64(5.0)
)  // 5 + 3x^2 + 2xy
```

### 算术操作

```kotlin
val p1 = CanonicalPolynomial<Flt64>(
    monomials = listOf(CanonicalMonomial(Flt64(2.0), listOf(x))),
    constant = Flt64(1.0)
)  // 1 + 2x

val p2 = CanonicalPolynomial<Flt64>(
    monomials = listOf(CanonicalMonomial(Flt64(3.0), listOf(x))),
    constant = Flt64(2.0)
)  // 2 + 3x

val sum = p1 + p2      // 3 + 5x（列表拼接）
val diff = p1 - p2     // -1 - x（列表拼接）
val scaled = p1 * Flt64(2.0)  // 2 + 4x
val divided = p1 / Flt64(2.0) // 0.5 + x
```

### 求值

```kotlin
import fuookami.ospf.kotlin.math.symbol.operation.*

// 直接求值
val values = mapOf(x to Flt64(2.0), y to Flt64(3.0))
val result = canonical.evaluate(values)  // Flt64(5 + 3*4 + 2*6) = Flt64(29.0)

// 有序求值（更快）
val order = listOf(x, y)
val valueList = listOf(Flt64(2.0), Flt64(3.0))
val result2 = canonical.evaluateOrdered(order, valueList)
```

### 编译与调用

```kotlin
// 编译以重复求值
val compiled = canonical.compileEval(order)

// 调用编译后的函数
val r1 = compiled(listOf(Flt64(1.0), Flt64(2.0)))
val r2 = compiled(listOf(Flt64(2.0), Flt64(3.0)))

// 编译梯度
val compiledGrad = canonical.compileGradient(order)
val grad = compiledGrad(listOf(Flt64(1.0), Flt64(2.0)))
// List<Flt64> - 偏导数
```

### DSL 快捷入口

```kotlin
import fuookami.ospf.kotlin.math.symbol.dsl.*
import fuookami.ospf.kotlin.math.symbol.serde.symbolOfSerializedIdentifier

val symbolOf = ::symbolOfSerializedIdentifier

// 从 DSL 构造线性多项式
val lp = linearPolynomial(symbolOf) {
    num(1) + num(2) * symbol("x") + num(3) * symbol("y")
}

// 从 DSL 构造二次多项式
val qp = quadraticPolynomial(symbolOf) {
    num(1) + num(2) * symbol("x") + num(3) * symbol("x") * symbol("x")
}

// 从 DSL 构造规范不等式
val ineq = canonicalInequality(symbolOf) {
    (symbol("x") * symbol("x")) + (symbol("y") * symbol("y")) le num(1)  // x^2 + y^2 <= 1
}
```

### 序列化

```kotlin
import fuookami.ospf.kotlin.math.symbol.serde.*

// 转 JSON
val json = canonical.toJsonString()

// 从 JSON 恢复
val restored = canonicalPolynomialFromJson(json)

// 不等式序列化
val ineqJson = linearInequality.toJsonString()
val restoredIneq = linearInequalityFromJson(ineqJson)

// legacy Expr 的 JSON round-trip
val exprJson = canonical.toLegacyExpr().toLegacyJsonString()
val restoredExpr = legacySymbolExprFromJson(exprJson)
```

### 矩阵形式

```kotlin
val form = canonical.toMatrixForm(order)
// form.q: 二次系数矩阵（稀疏）
// form.c: 线性系数向量
// form.constant: 常数项

// 对于二次表达式 1 + 2x + 3x^2 + 4xy:
// q = [[3, 2], [2, 0]]（对称）
// c = [2, 0]
// constant = 1
```

### MultiArray 集成

符号多项式可以存储在 MultiArray 中，并使用 FastSum 操作高效求和。

#### 基本用法

```kotlin
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.symbol.serde.symbolOfSerializedIdentifier

val x = symbolOfSerializedIdentifier("x")
val y = symbolOfSerializedIdentifier("y")

// 创建线性多项式的二维数组
val equations = MultiArray.newBy(Shape2(3, 4)) { i, _ ->
    LinearPolynomial<Flt64>(
        monomials = listOf(
            LinearMonomial(Flt64(i + 1.0), x),
            LinearMonomial(Flt64(i + 2.0), y)
        ),
        constant = Flt64(i.toDouble())
    )
}

// 沿轴 0 求和：结果是 1D 数组（形状 [4])
val sum0 = equations.sumAxis(0, LinearPolynomial.fromConstant(Flt64.zero))

// 沿轴 1 求和：结果是 1D 数组（形状 [3])
val sum1 = equations.sumAxis(1, LinearPolynomial.fromConstant(Flt64.zero))

// 全局求和：结果是单个多项式
val total = equations.sumAll(LinearPolynomial.fromConstant(Flt64.zero))

// 沿轴 1 累积求和
val cumsum = equations.cumsumAxis(1, LinearPolynomial.fromConstant(Flt64.zero))
```

#### 使用 Mutable 多项式的 FastSum 模式

高性能累加时，使用 Mutable 多项式延迟合并：

```kotlin
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.combineTerms

// FastSum 模式：先累加不合并，最后一次性合并
val result = MutableLinearPolynomial.fromConstant(Flt64.zero)

for (poly in equations) {
    result += poly  // 快速累加（不合并）
}

// 最后合并同类项
result.combineTerms(Flt64.zero)

// 转换为不可变
val final = result.toImmutable()
```

#### Mutable 多项式数组

```kotlin
// 创建可变多项式数组用于原地修改
val mutableEquations = MutableMultiArray.newBy(Shape2(3, 4)) { i, _ ->
    MutableLinearPolynomial.fromConstant(Flt64.zero)
}

// 原地修改
for (i in 0 until mutableEquations.size) {
    mutableEquations[i] += LinearPolynomial<Flt64>(
        monomials = listOf(LinearMonomial(Flt64(1.0), x)),
        constant = Flt64.zero
    )
}

// 完成后转换为不可变
val immutableEquations = mutableEquations.toImmutable()
```

#### 二次多项式数组

```kotlin
// 二次多项式的二维数组
val quadraticEquations = MultiArray.newBy(Shape2(2, 3)) { i, _ ->
    QuadraticPolynomial<Flt64>(
        monomials = listOf(
            QuadraticMonomial(Flt64(i + 1.0), x, null),  // (i+1) * x
            QuadraticMonomial(Flt64(i + 2.0), x, y)      // (i+2) * xy
        ),
        constant = Flt64.zero
    )
}

// 沿轴 0 求和
val sumQ = quadraticEquations.sumAxis(
    0,
    QuadraticPolynomial.fromConstant(Flt64.zero)
)
```

## 操作汇总

| 操作 | 文件 | 描述 |
|------|------|------|
| `combineTerms` | `operation/CombineTerms.kt` | 合并同类项（不可变） |
| `combineTerms` | `operation/MutableCombineOps.kt` | 合并同类项（可变，原地） |
| `addAssignAndCombine` | `operation/MutableCombineOps.kt` | 加法 + 合并一步完成 |
| `minusAssignAndCombine` | `operation/MutableCombineOps.kt` | 减法 + 合并一步完成 |
| `evaluate` | `operation/Evaluate.kt` | 计算值 |
| `compileEval` | `generic/CompileGeneric.kt` | 编译为函数 |
| `compileGradient` | `generic/CompileGeneric.kt` | 编译梯度 |
| `differentiate` | `operation/Differentiate.kt` | 符号求导 |
| `integrate` | `operation/IntegrateOps.kt` | 符号积分 |
| `factorize` | `operation/Factorization.kt` | 二次因式分解 |
| `toMatrixForm` | `operation/MatrixForm.kt` | 二次型提取 |
| `toLatex` | `operation/Latex.kt` | LaTeX 渲染 |
| `convert` | `operation/Convert.kt` | 类型转换 |

### MultiArray FastSum 操作

| 操作 | 文件 | 描述 |
|------|------|------|
| `sumAll` | `multi_array/FastSum.kt` | 全局求和 |
| `sumAxis` | `multi_array/FastSum.kt` | 沿单轴求和 |
| `sumAxes` | `multi_array/FastSum.kt` | 沿多轴求和 |
| `cumsumAxis` | `multi_array/FastSum.kt` | 沿轴累积求和 |

## 性能建议

### 热点路径（来自 S-PERF-1）

| 路径 | Ops/ms（基线） | 说明 |
|------|----------------|------|
| `combineTermsStress`（300 单项式） | ~30 | **主要热点** |
| `polynomialPlus` | ~25,000 | 列表拼接 |
| `polynomialMinus` | ~19,000 | 列表拼接 + 取反 |
| `polynomialTimesScalar` | ~63,000 | 最小开销 |
| `evaluateOrdered` | ~4,000 | 直接求值 |
| `compileEval` | ~1,600 | 含 combineTerms |

### 优化指南

1. **避免重复调用 combineTerms** - 尽可能缓存结果
2. **重复求值使用 compileEval** - 比直接求值快 2-3 倍
3. **优先使用 evaluateOrdered** - 避免 map 查找开销
4. **批量多项式操作** - 在调用 combineTerms 前合并

## 测试覆盖

- `PolynomialTest.kt`：算术操作
- `SerializationTest.kt`：JSON 往返（17 个测试）
- `DslTest.kt`：DSL 快捷入口（17 个测试）
- `CompileTest.kt`：编译与调用
- `MatrixFormTest.kt`：二次型提取
- `CombineTermsTest.kt`：同类项合并
- `MutableCombineTest.kt`：可变多项式合并（9 个测试）
- `FactorizationTest.kt`：二次因式分解（17 个测试）
- `IntegrationTest.kt`：符号积分（18 个测试）

运行测试：

```powershell
mvn -pl ospf-kotlin-math -Dtest=SerializationTest,DslTest,PolynomialTest,MutableCombineTest test
```

### MultiArray 测试

- `FastSumTest.kt`：MultiArray 求和（14 个测试）

```powershell
mvn -pl ospf-kotlin-math -Dtest=FastSumTest test
```

## 相关链接

- [主 README](../README.md)
- [Geometry 模块](../geometry/README.md)
- [Value Range 模块](../algebra/value_range/README.md)
- [MultiArray 模块](../multiarray/README.md)
- [基准报告](../../benchmark/BENCHMARK_REPORT_TEMPLATE.md)

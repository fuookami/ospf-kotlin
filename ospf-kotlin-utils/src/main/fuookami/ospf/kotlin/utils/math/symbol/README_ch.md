# ospf-kotlin-utils/math/symbol

[English Documentation (README.md)](./README.md)

OSPF Kotlin 的符号表达式基础与操作。

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
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.symbol.monomial.*
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.*
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64

val x = symbolOf("x")
val y = symbolOf("y")

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
import fuookami.ospf.kotlin.utils.math.symbol.operation.*

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
import fuookami.ospf.kotlin.utils.math.symbol.dsl.*

val x = symbolOf("x")
val y = symbolOf("y")

// 从 DSL 构造线性多项式
val lp = linearPolynomial(symbolOf) {
    val x = symbol("x")
    val y = symbol("y")
    1.0 + 2.0 * x + 3.0 * y
}

// 从 DSL 构造二次多项式
val qp = quadraticPolynomial(symbolOf) {
    val x = symbol("x")
    1.0 + 2.0 * x + 3.0 * x * x
}

// 从 DSL 构造规范不等式
val ineq = canonicalInequality(symbolOf) {
    val x = symbol("x")
    val y = symbol("y")
    x * x + y * y le 1.0  // x^2 + y^2 <= 1
}
```

### 序列化

```kotlin
import fuookami.ospf.kotlin.utils.math.symbol.serde.*

// 转 JSON
val json = canonical.toJsonString()

// 从 JSON 恢复
val restored = canonicalPolynomialFromJson<Flt64>(json)

// 不等式序列化
val ineqJson = linearInequality.toJsonString()
val restoredIneq = linearInequalityFromJson<Flt64>(ineqJson)
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

## 操作汇总

| 操作 | 文件 | 描述 |
|------|------|------|
| `combineTerms` | `operation/CombineTerms.kt` | 合并同类项 |
| `evaluate` | `operation/Evaluate.kt` | 计算值 |
| `compileEval` | `generic/CompileGeneric.kt` | 编译为函数 |
| `compileGradient` | `generic/CompileGeneric.kt` | 编译梯度 |
| `differentiate` | `operation/Differentiate.kt` | 符号求导 |
| `toMatrixForm` | `operation/MatrixForm.kt` | 二次型提取 |
| `toLatex` | `operation/Latex.kt` | LaTeX 渲染 |
| `convert` | `operation/Convert.kt` | 类型转换 |

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

运行测试：

```powershell
mvn -pl ospf-kotlin-utils -Dtest=SerializationTest,DslTest,PolynomialTest test
```

## 相关链接

- [主 README](../README.md)
- [Geometry 模块](../geometry/README.md)
- [Value Range 模块](../algebra/value_range/README.md)
- [基准报告](../../benchmark/BENCHMARK_REPORT_TEMPLATE.md)
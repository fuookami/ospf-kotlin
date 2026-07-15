# ospf-kotlin-math/symbol

:us: [English](README.md) | :cn: 简体中文

OSPF Kotlin 的符号表达式基础与操作。

`symbol` 包提供：
- `symbol.expression.*` 用于运行时布尔/标量表达式、属性路径、求值和规范化。
- `symbol.parse` 用于直接多项式和不等式解析。
- `symbol.serde` 用于直接多项式和不等式 JSON 序列化。
- `symbol.parser` 用于向后兼容的解析入口点，委托给 `symbol.parse`。

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
data class LinearInequality<T : Ring<T>>(
    val lhs: LinearPolynomial<T>,
    val rhs: LinearPolynomial<T>,
    val comparison: Comparison,  // LT, LE, EQ, NE, GE, GT
    val name: String = "",
    val displayName: String = ""
)

data class QuadraticInequality<T : Ring<T>>(...)
data class CanonicalInequality<T : Ring<T>>(...)
```

### 表达式 AST

`symbol.expression` 包提供两棵并行的表达式树：`ScalarExpression<T>` 表示标量值（数值/字符串/布尔），`BooleanExpression` 表示布尔谓词。二者在比较节点和条件节点处桥接，支持 `if (x > 0) then y else z fi` 这类混合表达式。

#### ScalarExpression 节点

| 节点 | 形式 | 描述 |
|------|------|------|
| `ScalarConstant<T>(value)` | `42`, `3.14`, `"str"` | 字面常量 |
| `ScalarReference<T>(path)` | `x`, `user.age` | 对属性路径的引用 |
| `ScalarSymbolReference<T>(symbol)` | 符号引用 | 对 `Symbol` 的引用（非路径形式） |
| `ScalarUnary<T>(operator, operand)` | `-x`, `+x` | 一元操作 |
| `ScalarBinary<T>(operator, left, right)` | `x + y`, `x ^ 2` | 二元操作 |
| `ScalarFunction<T>(name, arguments)` | `sqrt(x)`, `max(a, b)` | 函数调用 |
| `ScalarConditional<T>(condition, then, else)` | `if (c) then a else b fi` | 条件表达式；桥接 `BooleanExpression` -> `ScalarExpression` |
| `ScalarBoolean<T>(expr)` | 作为标量的 `x > 0` | 布尔表达式包装为标量值 |
| `ScalarCustom<T>(value, description)` | 用户定义 | 不透明自定义表达式 |

#### BooleanExpression 节点

| 节点 | 形式 | 描述 |
|------|------|------|
| `BooleanConstant(value)` | `true`, `false`, `unknown` | 三值（`Trivalent`）布尔常量 |
| `Comparison<T>(operator, left, right)` | `x > 0`, `a == b` | 比较；桥接 `ScalarExpression` -> `BooleanExpression` |
| `InExpression<T>(value, candidates, negated)` | `x in (1, 2, 3)` | 集合成员判断 |
| `PatternMatch<T>(value, pattern, mode)` | `name like 'A%'` | 模式匹配 |
| `NullCheck(path, type)` | `email is null` | 路径空值检查 |
| `AndExpression(operands)` | `a and b` | 逻辑与 |
| `OrExpression(operands)` | `a or b` | 逻辑或 |
| `NotExpression(operand)` | `not a` | 逻辑非 |
| `BooleanCustom(value, description)` | 用户定义 | 不透明自定义布尔 |

#### 操作符

| 操作符类型 | 取值 |
|-----------|------|
| `UnaryOperator` | `Negate`, `Positive`, `Abs` |
| `BinaryOperator` | `Add`, `Subtract`, `Multiply`, `Divide`, `Modulo`, `Power` |
| `ComparisonOperator` | `Eq`, `Ne`, `Lt`, `Le`, `Gt`, `Ge` |
| `PatternMatchMode` | `Exact`, `Prefix`, `Suffix`, `Contains`, `Like`, `Regex` |
| `NullCheckType` | `IsNull`, `IsNotNull` |

> **桥接方向：** `Comparison` 桥接标量 -> 布尔；`ScalarConditional` 桥接布尔 -> 标量。二者复用对方的树而非重复造节点。

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

### 解析

```kotlin
import fuookami.ospf.kotlin.math.symbol.parse.*
import fuookami.ospf.kotlin.math.symbol.serde.symbolOfSerializedIdentifier

val symbolOf = ::symbolOfSerializedIdentifier

// 解析线性多项式
val lp = parseLinearOrNull("2*x + 3*y + 1", Flt64.numberParser, Flt64.zero, Flt64.one, symbolOf)

// 解析二次多项式
val qp = parseQuadraticOrNull("x^2 + 2*x + 1", Flt64.numberParser, Flt64.zero, Flt64.one, symbolOf)

// 解析线性不等式
val ineq = parseLinearInequalityOrNull("2*x + 3*y <= 1", Flt64.numberParser, Flt64.zero, Flt64.one, symbolOf)
```

### 序列化

```kotlin
import fuookami.ospf.kotlin.math.symbol.operation.*

// 转 JSON
val json = canonical.toJsonString()

// 从 JSON 恢复
val restored = canonicalPolynomialFromJson(json)

// 不等式序列化
val ineqJson = linearInequality.toJsonString()
val restoredIneq = linearInequalityFromJson(ineqJson)
```

### 表达式解析

标量和布尔表达式可从字符串解析。标量解析器支持完整 Aviator 兼容语法（算术、比较、逻辑、三元、`if/then/else/fi`、`math.*` 函数）；布尔解析器支持过滤式谓词。

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.parser.*

// 解析标量表达式
val expr = parseScalarExpression("math.sqrt(x^2 + y^2)").value!!
// -> ScalarFunction("sqrt", [ScalarBinary(Add, Power(x,2), Power(y,2))])

val cond = parseScalarExpression("if (w > 787) then x else y fi").value!!
// -> ScalarConditional(Comparison(Gt, w, 787), Ref(x), Ref(y))

val boolAsScalar = parseScalarExpression("x > 0 && y > 0").value!!
// -> ScalarBoolean(AndExpression([Comparison(Gt, x, 0), Comparison(Gt, y, 0)]))

// 解析布尔过滤表达式（and/or/not、比较、in、is null、like）
val filter = parseBooleanExpression("age > 18 and status = 'active'").value!!
// -> AndExpression([Comparison(Gt, age, 18), Comparison(Eq, status, "active")])
```

标量解析器优先级（从高到低）：

| 优先级 | 操作符 | 结合性 |
|--------|--------|--------|
| 1（最高） | 字面量、标识符、`( )`、`true`、`false`、`null` | - |
| 2 | `^`、`**`（幂） | 右 |
| 3 | `+`（一元正号） | 右 |
| 4 | `*`、`/`、`%` | 左 |
| 5 | `+`、`-`（一元负号在此层处理） | 左 |
| 6 | `>`、`<`、`>=`、`<=`、`==`、`!=` | 左 |
| 7 | `&&`、`and` | 左 |
| 8 | `\|\|`、`or` | 左 |
| 9（最低） | `? :`、`if/then/else/fi` | 右 |

解析说明：
- `-x^2` 解析为 `-(x^2)`（一元负号低于幂运算），与 Python/Excel/Aviator 一致。`-x^2+1` = `-(x^2)+1`。
- `**` 和 `^` 均映射到 `BinaryOperator.Power`。
- `math.PI` 和 `math.E` 在解析期解析为常量（无运行时函数调用）。
- `math.` 前缀在函数调用时剥离：`math.sqrt(x)` -> `ScalarFunction("sqrt", [x])`。
- `if`/`? :` 的条件在逻辑或层解析，因此 `if x > 0 && y > 0 then ... fi` 无需外层括号。
- 词法分析器有两种模式：`LexMode.Boolean`（默认，将 `-3` 合并为数字 token）和 `LexMode.Scalar`（输出 `MINUS` + `NUMBER`）。`parseScalarExpression` 使用标量模式。

### 表达式求值

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.operation.*

// 从字符串路径构建上下文
val context = MapEvaluationContext.fromStringMap(mapOf("x" to 3.0, "y" to 4.0))

// 求值标量表达式（返回 Ret<Any?> 以显式处理错误）
val result = evaluateScalar(expr, context).value!!  // 5.0

// 使用 math.* 函数求值
val withMath = evaluateScalar(
    parseScalarExpression("math.pow(x, 2) + math.pow(y, 2)").value!!,
    context,
    MathFunctionEvaluator
).value!!  // 25.0

// 求值布尔表达式（返回 Trivalent 以支持三值逻辑）
val boolResult = evaluateBoolean(filter, context)  // Trivalent.True / False / Unknown

// 便捷扩展函数
val r = parseScalarExpression("x + y").value!!
    .evaluateWith(mapOf("x" to 1.0, "y" to 2.0))
    .value!!  // 3.0
```

错误处理：`evaluateScalar` 返回 `Ret<Any?>`。除零、未知函数、类型不匹配、未绑定符号引用均返回 `Failed`。`ScalarSymbolReference` 和 `ScalarCustom` 返回 `Failed`（不透明节点无法泛化求值）。布尔参与算术（如 `(x > 0) + 1`）返回 `Failed`——核心求值器不做布尔到数值的隐式强转；如需此能力由公式引擎层补充。

### math.* 函数表

`MathFunctionEvaluator` 实现 `ScalarFunctionEvaluator`，覆盖 17 个 Aviator 白名单数学函数：

| 函数 | 返回类型 | 说明 |
|------|---------|------|
| `sqrt`、`exp`、`log`（自然对数）、`log10` | `Double` | |
| `sin`、`cos`、`tan`、`asin`、`acos`、`atan` | `Double` | 弧度 |
| `floor`、`ceil` | `Double` | |
| `round` | `Long` | 对齐 Aviator `math.round(3.7) = 4L` |
| `pow`、`max`、`min` | `Double` | 双参数 |
| `abs` | 委托 `DefaultScalarFunctionEvaluator` | 已存在 |

`MathFunctionEvaluator` 与 `DefaultScalarFunctionEvaluator` 组合（后者提供 `abs`、`lower`、`upper`、`trim`、`length`、`coalesce`）。若要将调用方限制在 `math.*` 白名单内，应在 AST 层面校验 `ScalarFunction.name` 是否属于 `MathFunctionEvaluator.supportedFunctions`——不能依赖求值器拒绝，因为组合链暴露了字符串函数。

### 表达式 DSL

无需解析，以编程方式构造表达式：

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.dsl.*

// 通过 DSL 构造布尔表达式
val expr = path("age") gt 18 and (path("status") eq "active")

// 类型化路径构建器（编译期检查属性引用）
data class User(val age: Int, val status: String)
val typed = prop(User::age) gt 18

// 标量函数
val absExpr = abs(path("value"))
```

### 表达式序列化

表达式支持独立于多项式的 JSON 往返：

```kotlin
// 标量表达式
val json = expr.toJsonString()
val restored = scalarExpressionFromJson(json)

// 布尔表达式
val boolJson = filter.toJsonString()
val restoredBool = booleanExpressionFromJson(boolJson)
```

### 矩阵形式

```kotlin
val form = canonical.toFlt64MatrixForm(order)
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
val sum0 = equations.sumAxis(0, LinearPolynomial(emptyList(), Flt64.zero))

// 沿轴 1 求和：结果是 1D 数组（形状 [3])
val sum1 = equations.sumAxis(1, LinearPolynomial(emptyList(), Flt64.zero))

// 全局求和：结果是单个多项式
val total = equations.sumAll(LinearPolynomial(emptyList(), Flt64.zero))

// 沿轴 1 累积求和
val cumsum = equations.cumsumAxis(1, LinearPolynomial(emptyList(), Flt64.zero))
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
    QuadraticPolynomial(emptyList(), Flt64.zero)
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
| `compileEval` | `operation/Compile.kt`（Flt64 重载）、`operation/CompileOps.kt`（泛型 Ring） | 编译为函数 |
| `compileGradient` | `operation/Compile.kt`（Flt64 重载）、`operation/CompileOps.kt`（泛型 Ring） | 编译梯度 |
| `differentiate` | `operation/Differentiate.kt` | 符号求导 |
| `integrate` | `operation/IntegrateOps.kt` | 符号积分 |
| `factorize` | `operation/Factorization.kt` | 二次因式分解 |
| `toMatrixForm` | `operation/MatrixForm.kt` | 泛型矩阵形式提取 |
| `toFlt64MatrixForm` | `operation/Flt64MatrixForm.kt` | Flt64 矩阵形式提取 |
| `toLatex` | `operation/Latex.kt` | LaTeX 渲染 |
| `convert` | `operation/Convert.kt` | 类型转换 |
| `evaluateScalar` | `expression/operation/EvaluateScalar.kt` | 求值标量表达式（`Ret<Any?>`） |
| `evaluateBoolean` | `expression/operation/EvaluateBoolean.kt` | 求值布尔表达式（`Trivalent`） |
| `normalize` | `expression/operation/Normalize.kt` | 规范化布尔表达式（扁平化、折叠、去重） |
| `parseScalarExpression` | `expression/parser/ScalarParser.kt` | 解析标量表达式字符串 -> AST |
| `parseBooleanExpression` | `expression/parser/Parser.kt` | 解析布尔过滤字符串 -> AST |

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
- `DirectPolynomialParserTest.kt`：直接多项式解析
- `CompileTest.kt`：编译与调用
- `MatrixFormTest.kt`：二次型提取
- `CombineTermsTest.kt`：同类项合并
- `MutableCombineTest.kt`：可变多项式合并（9 个测试）
- `FactorizationTest.kt`：二次因式分解（17 个测试）
- `IntegrationTest.kt`：符号积分（18 个测试）
- `BooleanParserTest.kt`：布尔表达式解析
- `ScalarParserTest.kt`：标量表达式解析与求值（38 个测试）
- `EvaluateScalarTest.kt`：标量求值器边界用例（21 个测试）
- `EvaluateBooleanTest.kt`：布尔求值器
- `NormalizeTest.kt`：表达式规范化
- `ExpressionSerdeTest.kt`：表达式 JSON 往返

运行测试：

```powershell
mvn -pl ospf-kotlin-math -Dtest=SerializationTest,DirectPolynomialParserTest,PolynomialTest,MutableCombineTest test
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

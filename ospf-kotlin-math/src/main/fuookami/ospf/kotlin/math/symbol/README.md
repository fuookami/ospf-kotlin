# ospf-kotlin-math/symbol

:us: English | :cn: [简体中文](README_ch.md)

Symbolic expression foundation and operations for OSPF Kotlin.

The `symbol` package provides:
- `symbol.expression.*` for runtime boolean/scalar expressions, property paths, evaluation, and normalization.
- `symbol.parse` for direct polynomial and inequality parsing.
- `symbol.serde` for direct polynomial and inequality JSON serialization.
- `symbol.parser` for backward-compatible parse entry points that delegate to `symbol.parse`.

## Core Types

### Symbol

```kotlin
interface Symbol {
    val name: String
    val displayName: String?
}
```

### Monomials

| Type | Form | Example |
|------|------|---------|
| `LinearMonomial<T>` | `c * x` | `3x` |
| `QuadraticMonomial<T>` | `c * x * y` | `2xy`, `5x^2` |
| `CanonicalMonomial<T>` | `c * x^n * y^m * ...` | `3x^2y^3` |

### Polynomials

| Type | Form | Example |
|------|------|---------|
| `LinearPolynomial<T>` | `c0 + c1*x + c2*y + ...` | `1 + 2x + 3y` |
| `QuadraticPolynomial<T>` | `c0 + c1*x + c2*x^2 + c3*xy + ...` | `1 + 2x + 3x^2 + 4xy` |
| `CanonicalPolynomial<T>` | `c0 + Σ ci * Π xj^pj` | `1 + 2x + 3x^2y^3` |

### Inequalities

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

### Expression AST

The `symbol.expression` package provides two parallel expression trees: `ScalarExpression<T>` for scalar values (numeric/string/boolean) and `BooleanExpression` for boolean predicates. They bridge at comparison and conditional nodes, enabling mixed expressions like `if (x > 0) then y else z fi`.

#### ScalarExpression Nodes

| Node | Form | Description |
|------|------|-------------|
| `ScalarConstant<T>(value)` | `42`, `3.14`, `"str"` | Literal constant |
| `ScalarReference<T>(path)` | `x`, `user.age` | Reference to a property path |
| `ScalarSymbolReference<T>(symbol)` | symbol reference | Reference to a `Symbol` (non-path form) |
| `ScalarUnary<T>(operator, operand)` | `-x`, `+x` | Unary operation |
| `ScalarBinary<T>(operator, left, right)` | `x + y`, `x ^ 2` | Binary operation |
| `ScalarFunction<T>(name, arguments)` | `sqrt(x)`, `max(a, b)` | Function call |
| `ScalarConditional<T>(condition, then, else)` | `if (c) then a else b fi` | Conditional; bridges `BooleanExpression` -> `ScalarExpression` |
| `ScalarBoolean<T>(expr)` | `x > 0` as scalar | Boolean expression wrapped as scalar value |
| `ScalarCustom<T>(value, description)` | user-defined | Opaque custom expression |

#### BooleanExpression Nodes

| Node | Form | Description |
|------|------|-------------|
| `BooleanConstant(value)` | `true`, `false`, `unknown` | Three-valued (`Trivalent`) boolean constant |
| `Comparison<T>(operator, left, right)` | `x > 0`, `a == b` | Comparison; bridges `ScalarExpression` -> `BooleanExpression` |
| `InExpression<T>(value, candidates, negated)` | `x in (1, 2, 3)` | Set membership |
| `PatternMatch<T>(value, pattern, mode)` | `name like 'A%'` | Pattern matching |
| `NullCheck(path, type)` | `email is null` | Null check on a path |
| `AndExpression(operands)` | `a and b` | Logical AND |
| `OrExpression(operands)` | `a or b` | Logical OR |
| `NotExpression(operand)` | `not a` | Logical NOT |
| `BooleanCustom(value, description)` | user-defined | Opaque custom boolean |

#### Operators

| Operator Type | Values |
|---------------|--------|
| `UnaryOperator` | `Negate`, `Positive`, `Abs` |
| `BinaryOperator` | `Add`, `Subtract`, `Multiply`, `Divide`, `Modulo`, `Power` |
| `ComparisonOperator` | `Eq`, `Ne`, `Lt`, `Le`, `Gt`, `Ge` |
| `PatternMatchMode` | `Exact`, `Prefix`, `Suffix`, `Contains`, `Like`, `Regex` |
| `NullCheckType` | `IsNull`, `IsNotNull` |

> **Bridge direction:** `Comparison` bridges scalar -> boolean; `ScalarConditional` bridges boolean -> scalar. Both reuse the other tree rather than duplicating nodes.

## Usage Examples

### Polynomial Construction

```kotlin
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.serde.symbolOfSerializedIdentifier
import fuookami.ospf.kotlin.math.algebra.number.Flt64

val x = symbolOfSerializedIdentifier("x")
val y = symbolOfSerializedIdentifier("y")

// Linear polynomial
val linear = LinearPolynomial<Flt64>(
    monomials = listOf(
        LinearMonomial(Flt64(2.0), x),
        LinearMonomial(Flt64(3.0), y)
    ),
    constant = Flt64(1.0)
)  // 1 + 2x + 3y

// Canonical polynomial
val canonical = CanonicalPolynomial<Flt64>(
    monomials = listOf(
        CanonicalMonomial(Flt64(3.0), listOf(x, x)),  // 3x^2
        CanonicalMonomial(Flt64(2.0), listOf(x, y))   // 2xy
    ),
    constant = Flt64(5.0)
)  // 5 + 3x^2 + 2xy
```

### Arithmetic Operations

```kotlin
val p1 = CanonicalPolynomial<Flt64>(
    monomials = listOf(CanonicalMonomial(Flt64(2.0), listOf(x))),
    constant = Flt64(1.0)
)  // 1 + 2x

val p2 = CanonicalPolynomial<Flt64>(
    monomials = listOf(CanonicalMonomial(Flt64(3.0), listOf(x))),
    constant = Flt64(2.0)
)  // 2 + 3x

val sum = p1 + p2      // 3 + 5x (list concatenation)
val diff = p1 - p2     // -1 - x (list concatenation)
val scaled = p1 * Flt64(2.0)  // 2 + 4x
val divided = p1 / Flt64(2.0) // 0.5 + x
```

### Evaluation

```kotlin
import fuookami.ospf.kotlin.math.symbol.operation.*

// Direct evaluation
val values = mapOf(x to Flt64(2.0), y to Flt64(3.0))
val result = canonical.evaluate(values)  // Flt64(5 + 3*4 + 2*6) = Flt64(29.0)

// Ordered evaluation (faster)
val order = listOf(x, y)
val valueList = listOf(Flt64(2.0), Flt64(3.0))
val result2 = canonical.evaluateOrdered(order, valueList)
```

### Compile and Invoke

```kotlin
// Compile for repeated evaluation
val compiled = canonical.compileEval(order)

// Invoke compiled function
val r1 = compiled(listOf(Flt64(1.0), Flt64(2.0)))
val r2 = compiled(listOf(Flt64(2.0), Flt64(3.0)))

// Compile gradient
val compiledGrad = canonical.compileGradient(order)
val grad = compiledGrad(listOf(Flt64(1.0), Flt64(2.0)))
// List<Flt64> - partial derivatives
```

### Parsing

```kotlin
import fuookami.ospf.kotlin.math.symbol.parse.*
import fuookami.ospf.kotlin.math.symbol.serde.symbolOfSerializedIdentifier

val symbolOf = ::symbolOfSerializedIdentifier

// Parse linear polynomial
val lp = parseLinearOrNull("2*x + 3*y + 1", Flt64.numberParser, Flt64.zero, Flt64.one, symbolOf)

// Parse quadratic polynomial
val qp = parseQuadraticOrNull("x^2 + 2*x + 1", Flt64.numberParser, Flt64.zero, Flt64.one, symbolOf)

// Parse linear inequality
val ineq = parseLinearInequalityOrNull("2*x + 3*y <= 1", Flt64.numberParser, Flt64.zero, Flt64.one, symbolOf)
```

### Serialization

```kotlin
import fuookami.ospf.kotlin.math.symbol.operation.*

// To JSON
val json = canonical.toJsonString()

// From JSON
val restored = canonicalPolynomialFromJson(json)

// Inequality serialization
val ineqJson = linearInequality.toJsonString()
val restoredIneq = linearInequalityFromJson(ineqJson)
```

### Expression Parsing

Scalar and boolean expressions can be parsed from strings. The scalar parser supports full Aviator-compatible syntax (arithmetic, comparison, logic, ternary, `if/then/else/fi`, `math.*` functions); the boolean parser supports filter-style predicates.

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.parser.*

// Parse scalar expression
val expr = parseScalarExpression("math.sqrt(x^2 + y^2)").value!!
// -> ScalarFunction("sqrt", [ScalarBinary(Add, Power(x,2), Power(y,2))])

val cond = parseScalarExpression("if (w > 787) then x else y fi").value!!
// -> ScalarConditional(Comparison(Gt, w, 787), Ref(x), Ref(y))

val boolAsScalar = parseScalarExpression("x > 0 && y > 0").value!!
// -> ScalarBoolean(AndExpression([Comparison(Gt, x, 0), Comparison(Gt, y, 0)]))

// Parse boolean filter expression (and/or/not, comparison, in, is null, like)
val filter = parseBooleanExpression("age > 18 and status = 'active'").value!!
// -> AndExpression([Comparison(Gt, age, 18), Comparison(Eq, status, "active")])
```

Scalar parser precedence (high to low):

| Priority | Operators | Associativity |
|----------|-----------|---------------|
| 1 (highest) | literals, identifiers, `( )`, `true`, `false`, `null` | - |
| 2 | `^`, `**` (power) | Right |
| 3 | `+` (unary positive) | Right |
| 4 | `*`, `/`, `%` | Left |
| 5 | `+`, `-` (unary minus handled here) | Left |
| 6 | `>`, `<`, `>=`, `<=`, `==`, `!=` | Left |
| 7 | `&&`, `and` | Left |
| 8 | `\|\|`, `or` | Left |
| 9 (lowest) | `? :`, `if/then/else/fi` | Right |

Parsing notes:
- `-x^2` parses as `-(x^2)` (unary minus below power), matching Python/Excel/Aviator. `-x^2+1` = `-(x^2)+1`.
- `**` and `^` both map to `BinaryOperator.Power`.
- `math.PI` and `math.E` resolve to constants at parse time (no runtime function call).
- `math.` prefix is stripped from function calls: `math.sqrt(x)` -> `ScalarFunction("sqrt", [x])`.
- `if`/`? :` conditions parse at logical-OR level, so `if x > 0 && y > 0 then ... fi` works without outer parentheses.
- The lexer has two modes: `LexMode.Boolean` (default, merges `-3` into a number token) and `LexMode.Scalar` (emits `MINUS` + `NUMBER`). `parseScalarExpression` uses scalar mode.

### Expression Evaluation

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.operation.*

// Build context from string paths
val context = MapEvaluationContext.fromStringMap(mapOf("x" to 3.0, "y" to 4.0))

// Evaluate scalar expression (returns Ret<Any?> for explicit error handling)
val result = evaluateScalar(expr, context).value!!  // 5.0

// Evaluate with math.* functions
val withMath = evaluateScalar(
    parseScalarExpression("math.pow(x, 2) + math.pow(y, 2)").value!!,
    context,
    MathFunctionEvaluator
).value!!  // 25.0

// Evaluate boolean expression (returns Trivalent for three-valued logic)
val boolResult = evaluateBoolean(filter, context)  // Trivalent.True / False / Unknown

// Convenience extension
val r = parseScalarExpression("x + y").value!!
    .evaluateWith(mapOf("x" to 1.0, "y" to 2.0))
    .value!!  // 3.0
```

Error handling: `evaluateScalar` returns `Ret<Any?>`. Division by zero, unknown functions, type mismatches, and unbound symbol references all yield `Failed`. `ScalarSymbolReference` and `ScalarCustom` return `Failed` (opaque nodes cannot be evaluated generically). Boolean-in-arithmetic (e.g. `(x > 0) + 1`) returns `Failed` -- no implicit boolean-to-number coercion; the formula engine layer can add this if needed.

### math.* Function Table

`MathFunctionEvaluator` implements `ScalarFunctionEvaluator` with 17 Aviator-whitelisted math functions:

| Function | Returns | Notes |
|----------|---------|-------|
| `sqrt`, `exp`, `log` (natural), `log10` | `Double` | |
| `sin`, `cos`, `tan`, `asin`, `acos`, `atan` | `Double` | radians |
| `floor`, `ceil` | `Double` | |
| `round` | `Long` | aligns with Aviator `math.round(3.7) = 4L` |
| `pow`, `max`, `min` | `Double` | 2-argument |
| `abs` | delegates to `DefaultScalarFunctionEvaluator` | already exists |

`MathFunctionEvaluator` composes with `DefaultScalarFunctionEvaluator` (which provides `abs`, `lower`, `upper`, `trim`, `length`, `coalesce`). To restrict callers to the `math.*` whitelist, validate `ScalarFunction.name` against `MathFunctionEvaluator.supportedFunctions` at the AST level -- do not rely on evaluator rejection, since the composite chain exposes string functions.

### Expression DSL

Construct expressions programmatically without parsing:

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.dsl.*

// Boolean expression via DSL
val expr = path("age") gt 18 and (path("status") eq "active")

// Typed path builder (compile-checked property references)
data class User(val age: Int, val status: String)
val typed = prop(User::age) gt 18

// Scalar function
val absExpr = abs(path("value"))
```

### Expression Serialization

Expressions support JSON round-trip independent of polynomials:

```kotlin
// Scalar expression
val json = expr.toJsonString()
val restored = scalarExpressionFromJson(json)

// Boolean expression
val boolJson = filter.toJsonString()
val restoredBool = booleanExpressionFromJson(boolJson)
```

### Matrix Form

```kotlin
val form = canonical.toFlt64MatrixForm(order)
// form.q: Quadratic coefficient matrix (sparse)
// form.c: Linear coefficient vector
// form.constant: Constant term

// For quadratic expression 1 + 2x + 3x^2 + 4xy:
// q = [[3, 2], [2, 0]] (symmetric)
// c = [2, 0]
// constant = 1
```

### MultiArray Integration

Symbolic polynomials can be stored in MultiArray and efficiently summed using FastSum operations.

#### Basic Usage

```kotlin
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.symbol.serde.symbolOfSerializedIdentifier

val x = symbolOfSerializedIdentifier("x")
val y = symbolOfSerializedIdentifier("y")

// Create a 2D array of linear polynomials
val equations = MultiArray.newBy(Shape2(3, 4)) { i, _ ->
    LinearPolynomial<Flt64>(
        monomials = listOf(
            LinearMonomial(Flt64(i + 1.0), x),
            LinearMonomial(Flt64(i + 2.0), y)
        ),
        constant = Flt64(i.toDouble())
    )
}

// Sum along axis 0: result is a 1D array (shape [4])
val sum0 = equations.sumAxis(0, LinearPolynomial(emptyList(), Flt64.zero))

// Sum along axis 1: result is a 1D array (shape [3])
val sum1 = equations.sumAxis(1, LinearPolynomial(emptyList(), Flt64.zero))

// Sum all elements: result is a single polynomial
val total = equations.sumAll(LinearPolynomial(emptyList(), Flt64.zero))

// Cumulative sum along axis 1
val cumsum = equations.cumsumAxis(1, LinearPolynomial(emptyList(), Flt64.zero))
```

#### FastSum Pattern with Mutable Polynomials

For high-performance accumulation, use Mutable polynomials with delayed combining:

```kotlin
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.combineTerms

// FastSum pattern: accumulate without combining, then combine once
val result = MutableLinearPolynomial.fromConstant(Flt64.zero)

for (poly in equations) {
    result += poly  // Fast accumulation (no combining)
}

// Combine like terms once at the end
result.combineTerms(Flt64.zero)

// Convert to immutable
val final = result.toImmutable()
```

#### MultiArray of Mutable Polynomials

```kotlin
// Create mutable polynomial array for in-place modifications
val mutableEquations = MutableMultiArray.newBy(Shape2(3, 4)) { i, _ ->
    MutableLinearPolynomial.fromConstant(Flt64.zero)
}

// In-place modification
for (i in 0 until mutableEquations.size) {
    mutableEquations[i] += LinearPolynomial<Flt64>(
        monomials = listOf(LinearMonomial(Flt64(1.0), x)),
        constant = Flt64.zero
    )
}

// Convert to immutable when done
val immutableEquations = mutableEquations.toImmutable()
```

#### Quadratic Polynomial Arrays

```kotlin
// 2D array of quadratic polynomials
val quadraticEquations = MultiArray.newBy(Shape2(2, 3)) { i, _ ->
    QuadraticPolynomial<Flt64>(
        monomials = listOf(
            QuadraticMonomial(Flt64(i + 1.0), x, null),  // (i+1) * x
            QuadraticMonomial(Flt64(i + 2.0), x, y)      // (i+2) * xy
        ),
        constant = Flt64.zero
    )
}

// Sum along axis 0 with quadratic polynomial zero
val sumQ = quadraticEquations.sumAxis(
    0,
    QuadraticPolynomial(emptyList(), Flt64.zero)
)
```

## Operations Summary

| Operation | File | Description |
|-----------|------|-------------|
| `combineTerms` | `operation/CombineTerms.kt` | Merge like terms (immutable) |
| `combineTerms` | `operation/MutableCombineOps.kt` | Merge like terms (mutable, in-place) |
| `addAssignAndCombine` | `operation/MutableCombineOps.kt` | Add + combine in one step |
| `minusAssignAndCombine` | `operation/MutableCombineOps.kt` | Subtract + combine in one step |
| `evaluate` | `operation/Evaluate.kt` | Compute value |
| `compileEval` | `operation/Compile.kt` (Flt64 overloads), `operation/CompileOps.kt` (generic Ring-based) | Compile to function |
| `compileGradient` | `operation/Compile.kt` (Flt64 overloads), `operation/CompileOps.kt` (generic Ring-based) | Compile gradient |
| `differentiate` | `operation/Differentiate.kt` | Symbolic differentiation |
| `integrate` | `operation/IntegrateOps.kt` | Symbolic integration |
| `factorize` | `operation/Factorization.kt` | Quadratic factorization |
| `toMatrixForm` | `operation/MatrixForm.kt` | Generic matrix form extraction |
| `toFlt64MatrixForm` | `operation/Flt64MatrixForm.kt` | Flt64 matrix form extraction |
| `toLatex` | `operation/Latex.kt` | LaTeX rendering |
| `convert` | `operation/Convert.kt` | Type conversion |
| `evaluateScalar` | `expression/operation/EvaluateScalar.kt` | Evaluate scalar expression (`Ret<Any?>`) |
| `evaluateBoolean` | `expression/operation/EvaluateBoolean.kt` | Evaluate boolean expression (`Trivalent`) |
| `normalize` | `expression/operation/Normalize.kt` | Normalize boolean expression (flatten, fold, dedup) |
| `parseScalarExpression` | `expression/parser/ScalarParser.kt` | Parse scalar expression string -> AST |
| `parseBooleanExpression` | `expression/parser/Parser.kt` | Parse boolean filter string -> AST |

## Performance Notes

### Hotspot Paths (from S-PERF-1)

| Path | Ops/ms (baseline) | Notes |
|------|-------------------|-------|
| `combineTermsStress` (300 monomials) | ~30 | **Primary hotspot** |
| `polynomialPlus` | ~25,000 | List concatenation |
| `polynomialMinus` | ~19,000 | List concatenation + negation |
| `polynomialTimesScalar` | ~63,000 | Minimal overhead |
| `evaluateOrdered` | ~4,000 | Direct evaluation |
| `compileEval` | ~1,600 | Includes combineTerms |

### Optimization Guidelines

1. **Avoid repeated combineTerms** - Cache results when possible
2. **Use compileEval for repeated evaluation** - 2-3x faster than direct evaluation
3. **Prefer evaluateOrdered over evaluate** - Avoids map lookup overhead
4. **Batch polynomial operations** - Combine before calling combineTerms

## Test Coverage

- `PolynomialTest.kt`: Arithmetic operations
- `SerializationTest.kt`: JSON round-trip (17 tests)
- `DirectPolynomialParserTest.kt`: Direct polynomial parsing
- `CompileTest.kt`: Compilation and invocation
- `MatrixFormTest.kt`: Quadratic form extraction
- `CombineTermsTest.kt`: Like-term merging
- `MutableCombineTest.kt`: Mutable polynomial combine (9 tests)
- `FactorizationTest.kt`: Quadratic factorization (17 tests)
- `IntegrationTest.kt`: Symbolic integration (18 tests)
- `BooleanParserTest.kt`: Boolean expression parsing
- `ScalarParserTest.kt`: Scalar expression parsing and evaluation (38 tests)
- `EvaluateScalarTest.kt`: Scalar evaluator edge cases (21 tests)
- `EvaluateBooleanTest.kt`: Boolean evaluator
- `NormalizeTest.kt`: Expression normalization
- `ExpressionSerdeTest.kt`: Expression JSON round-trip

Run tests:

```powershell
mvn -pl ospf-kotlin-math -Dtest=SerializationTest,PolynomialTest,MutableCombineTest test
```

### MultiArray Tests

- `FastSumTest.kt`: MultiArray summation (14 tests)

```powershell
mvn -pl ospf-kotlin-math -Dtest=FastSumTest test
```

## Related

- [Main README](../README.md)
- [Geometry Module](../geometry/README.md)
- [Value Range Module](../algebra/value_range/README.md)
- [MultiArray Module](../multiarray/README.md)
- [Benchmark Report](../../benchmark/BENCHMARK_REPORT_TEMPLATE.md)

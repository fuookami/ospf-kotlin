# ospf-kotlin-utils/math/symbol

[中文文档 (README_ch.md)](./README_ch.md)

Symbolic expression foundation and operations for OSPF Kotlin.

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
data class LinearInequality<T>(
    val polynomial: LinearPolynomial<T>,
    val comparison: Comparison  // Lt, Le, Eq, Ne, Ge, Gt
)

data class QuadraticInequality<T>(...)
data class CanonicalInequality<T>(...)
```

## Usage Examples

### Polynomial Construction

```kotlin
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.symbol.monomial.*
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.*
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64

val x = symbolOf("x")
val y = symbolOf("y")

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
import fuookami.ospf.kotlin.utils.math.symbol.operation.*

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

### DSL Quick Entry

```kotlin
import fuookami.ospf.kotlin.utils.math.symbol.dsl.*

val x = symbolOf("x")
val y = symbolOf("y")

// Linear polynomial from DSL
val lp = linearPolynomial(symbolOf) {
    val x = symbol("x")
    val y = symbol("y")
    1.0 + 2.0 * x + 3.0 * y
}

// Quadratic polynomial from DSL
val qp = quadraticPolynomial(symbolOf) {
    val x = symbol("x")
    1.0 + 2.0 * x + 3.0 * x * x
}

// Canonical inequality from DSL
val ineq = canonicalInequality(symbolOf) {
    val x = symbol("x")
    val y = symbol("y")
    x * x + y * y le 1.0  // x^2 + y^2 <= 1
}
```

### Serialization

```kotlin
import fuookami.ospf.kotlin.utils.math.symbol.serde.*

// To JSON
val json = canonical.toJsonString()

// From JSON
val restored = canonicalPolynomialFromJson<Flt64>(json)

// Inequality serialization
val ineqJson = linearInequality.toJsonString()
val restoredIneq = linearInequalityFromJson<Flt64>(ineqJson)
```

### Matrix Form

```kotlin
val form = canonical.toMatrixForm(order)
// form.q: Quadratic coefficient matrix (sparse)
// form.c: Linear coefficient vector
// form.constant: Constant term

// For quadratic expression 1 + 2x + 3x^2 + 4xy:
// q = [[3, 2], [2, 0]] (symmetric)
// c = [2, 0]
// constant = 1
```

## Operations Summary

| Operation | File | Description |
|-----------|------|-------------|
| `combineTerms` | `operation/CombineTerms.kt` | Merge like terms |
| `evaluate` | `operation/Evaluate.kt` | Compute value |
| `compileEval` | `generic/CompileGeneric.kt` | Compile to function |
| `compileGradient` | `generic/CompileGeneric.kt` | Compile gradient |
| `differentiate` | `operation/Differentiate.kt` | Symbolic differentiation |
| `toMatrixForm` | `operation/MatrixForm.kt` | Quadratic form extraction |
| `toLatex` | `operation/Latex.kt` | LaTeX rendering |
| `convert` | `operation/Convert.kt` | Type conversion |

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
- `DslTest.kt`: DSL quick entry (17 tests)
- `CompileTest.kt`: Compilation and invocation
- `MatrixFormTest.kt`: Quadratic form extraction
- `CombineTermsTest.kt`: Like-term merging

Run tests:

```powershell
mvn -pl ospf-kotlin-utils -Dtest=SerializationTest,DslTest,PolynomialTest test
```

## Related

- [Main README](../README.md)
- [Geometry Module](../geometry/README.md)
- [Value Range Module](../algebra/value_range/README.md)
- [Benchmark Report](../../benchmark/BENCHMARK_REPORT_TEMPLATE.md)
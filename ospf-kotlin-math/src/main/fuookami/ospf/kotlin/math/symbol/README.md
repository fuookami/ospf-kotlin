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

### MultiArray Integration

Symbolic polynomials can be stored in MultiArray and efficiently summed using FastSum operations.

#### Basic Usage

```kotlin
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.utils.multi_array.FastSum

val x = symbolOf("x")
val y = symbolOf("y")

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
val sum0 = equations.sumAxis(0, LinearPolynomial.fromConstant(Flt64.zero))

// Sum along axis 1: result is a 1D array (shape [3])
val sum1 = equations.sumAxis(1, LinearPolynomial.fromConstant(Flt64.zero))

// Sum all elements: result is a single polynomial
val total = equations.sumAll(LinearPolynomial.fromConstant(Flt64.zero))

// Cumulative sum along axis 1
val cumsum = equations.cumsumAxis(1, LinearPolynomial.fromConstant(Flt64.zero))
```

#### FastSum Pattern with Mutable Polynomials

For high-performance accumulation, use Mutable polynomials with delayed combining:

```kotlin
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.operation.combineTerms

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
    QuadraticPolynomial.fromConstant(Flt64.zero)
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
| `compileEval` | `generic/CompileGeneric.kt` | Compile to function |
| `compileGradient` | `generic/CompileGeneric.kt` | Compile gradient |
| `differentiate` | `operation/Differentiate.kt` | Symbolic differentiation |
| `integrate` | `operation/IntegrateOps.kt` | Symbolic integration |
| `factorize` | `operation/Factorization.kt` | Quadratic factorization |
| `toMatrixForm` | `operation/MatrixForm.kt` | Quadratic form extraction |
| `toLatex` | `operation/Latex.kt` | LaTeX rendering |
| `convert` | `operation/Convert.kt` | Type conversion |

### MultiArray FastSum Operations

| Operation | File | Description |
|-----------|------|-------------|
| `sumAll` | `multi_array/FastSum.kt` | Sum all elements |
| `sumAxis` | `multi_array/FastSum.kt` | Sum along single axis |
| `sumAxes` | `multi_array/FastSum.kt` | Sum along multiple axes |
| `cumsumAxis` | `multi_array/FastSum.kt` | Cumulative sum along axis |

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
- `MutableCombineTest.kt`: Mutable polynomial combine (9 tests)
- `FactorizationTest.kt`: Quadratic factorization (17 tests)
- `IntegrationTest.kt`: Symbolic integration (18 tests)

Run tests:

```powershell
mvn -pl ospf-kotlin-utils -Dtest=SerializationTest,DslTest,PolynomialTest,MutableCombineTest test
```

### MultiArray Tests

- `FastSumTest.kt`: MultiArray summation (14 tests)

```powershell
mvn -pl ospf-kotlin-utils -Dtest=FastSumTest test
```

## Related

- [Main README](../README.md)
- [Geometry Module](../geometry/README.md)
- [Value Range Module](../algebra/value_range/README.md)
- [MultiArray Module](../multi_array/README.md)
- [Benchmark Report](../../benchmark/BENCHMARK_REPORT_TEMPLATE.md)
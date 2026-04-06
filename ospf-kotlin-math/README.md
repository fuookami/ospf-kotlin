# ospf-kotlin-math

[中文文档 (README_ch.md)](./README_ch.md)

Mathematical algebra and symbol system for OSPF Kotlin. Provides foundational mathematical types, algebraic structures, symbolic expressions, and numerical operations.

## Module Structure

| Package | Description |
|---------|-------------|
| `algebra.concept` | Algebraic structure interfaces (Group, Ring, Field, Monoid) |
| `algebra.law` | Algebraic law validation for structure testing |
| `algebra.number` | Number type implementations (Int64, Flt64, UInt64) |
| `algebra.value_range` | Typed value ranges with bounds validation |
| `chaotic_operator` | Chaotic system attractors and maps (30+ implementations) |
| `combinatorics` | Combinatorial algorithms and operations |
| `fractal_operator` | Fractal generation (Mandelbrot set) |
| `geometry` | Geometric primitives (Point, Circle, Triangle, etc.) |
| `multiarray` | Multi-dimensional array with Einstein summation |
| `operator` | Mathematical operators and functors |
| `ordinary` | Common math operations (GCD, LCM, Prime, Factorization) |
| `parallel` | Parallel computation utilities (chunked fold) |
| `symbol` | Symbolic expression system (polynomials, inequalities) |

## Core Features

### Algebraic Structures

```kotlin
import fuookami.ospf.kotlin.math.algebra.concept.*

// Field supports addition, subtraction, multiplication, division
interface Field<Self> : CommutativeRing<Self>, MultiplicativeGroup<Self>

// Number types implement algebraic structures
val a = Flt64(3.14)
val b = Flt64(2.0)
val c = a + b  // Addition via Group
val d = a * b  // Multiplication via Ring
val e = a / b  // Division via Field
```

### Symbolic Expressions

```kotlin
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*

val x = symbolOf("x")
val y = symbolOf("y")

// Linear polynomial: 1 + 2x + 3y
val linear = LinearPolynomial<Flt64>(
    monomials = listOf(
        LinearMonomial(Flt64(2.0), x),
        LinearMonomial(Flt64(3.0), y)
    ),
    constant = Flt64(1.0)
)

// Evaluate at x=2, y=3
val result = linear.evaluate(mapOf(x to Flt64(2.0), y to Flt64(3.0)))
// Result: 1 + 2*2 + 3*3 = 14
```

### Einstein Summation

```kotlin
import fuookami.ospf.kotlin.math.multiarray.einsum.*

// Matrix multiplication using Einstein notation
val a = MultiArray.newWith(Shape2(2, 3), 1.0)
val b = MultiArray.newWith(Shape2(3, 4), 2.0)

// Method 1: Convenience function
val c1 = matmul(a, b, 0.0)

// Method 2: String notation
val c2 = einsumDouble(a, b, "ij,jk->ik")
```

### GCD/LCM Operations

```kotlin
import fuookami.ospf.kotlin.math.ordinary.*

// GCD supports multiple arguments
val gcd1 = gcd(UInt64(12), UInt64(8))         // 4
val gcd2 = gcd(UInt64(12), UInt64(8), UInt64(4)) // 4
val gcd3 = gcd(listOf(UInt64(0), UInt64(0)))  // 0 (boundary case)

// LCM with zero handling
val lcm1 = lcm(UInt64(4), UInt64(6))    // 12
val lcm2 = lcm(UInt64(4), UInt64(0))    // 0 (zero short-circuit)
```

### Prime Factorization

```kotlin
import fuookami.ospf.kotlin.math.ordinary.Prime
import fuookami.ospf.kotlin.math.ordinary.Factorization

// Get primes up to limit (cached for performance)
val primes = Prime.getPrimesUpTo(UInt64(100))

// Factorize number
val factors = factorize(UInt64(60))  // [(2, 2), (3, 1), (5, 1)]
```

### Parallel Operations

```kotlin
import fuookami.ospf.kotlin.math.parallel.fold

// Chunked parallel fold for large collections
val sum = parallelFold(
    collection = largeList,
    initial = Flt64.zero,
    chunkSize = 100  // Limit coroutine count
) { acc, item -> acc + item }
```

## Performance Optimizations

| Feature | Optimization | Notes |
|---------|-------------|-------|
| GCD multi-arg | Iterator-based fold | Avoids % 0 vulnerability |
| LCM | Zero short-circuit | Prevents entering vulnerable GCD path |
| Prime cache | Sieve of Eratosthenes | O(n log log n) initialization |
| Factorization | O(√n) upper bound | Uses sqrt(n) instead of n |
| Parallel fold | Chunk-based limiting | Controls coroutine count |
| Contract operation | Stride pre-computation | Output-driven iteration |

## Testing

```powershell
# Run all tests
mvn -pl ospf-kotlin-math test

# Run specific tests
mvn -pl ospf-kotlin-math -Dtest=GCDTest,LCMTest test
```

## Dependencies

- `ospf-kotlin-utils`: Utility functions and types
- `ospf-kotlin-multiarray`: Multi-dimensional array foundation

## Related Modules

- [symbol/README.md](src/main/fuookami/ospf/kotlin/math/symbol/README.md) - Symbolic expression documentation
- [geometry/README.md](src/main/fuookami/ospf/kotlin/math/geometry/README.md) - Geometry module documentation
- [algebra/value_range/README.md](src/main/fuookami/ospf/kotlin/math/algebra/value_range/README.md) - Value range documentation
# ospf-kotlin-math/ordinary

[Chinese Documentation (README_ch.md)](./README_ch.md)

Common mathematical operations including GCD, LCM, prime numbers, factorization, and utility functions for OSPF Kotlin.

## Algorithms

### Number Theory

| Function | File | Description |
|----------|------|-------------|
| `gcd(x, y)` | `GCD.kt` | Greatest common divisor (subtraction-based algorithm) |
| `gcdMod(x, y)` | `GCD.kt` | GCD using modulo algorithm |
| `gcd(numbers, constants)` | `GCD.kt` | GCD of multiple integers |
| `extendedGcd(a, b)` | `GCD.kt` | Extended GCD (Bezout identity: gcd(a,b) = ax + by) |
| `lcm(x, y)` | `LCM.kt` | Least common multiple (via GCD formula) |
| `lcm(numbers, constants)` | `LCM.kt` | LCM of multiple integers |
| `lcmByFactorization(numbers)` | `LCM.kt` | LCM via prime factorization |
| `factorize(num)` | `Factorization.kt` | Prime factorization, returns `List<Pair<I, Int>>` |
| `defactorize(factors)` | `Factorization.kt` | Reconstruct integer from factorization |
| `divisors(num)` | `Factorization.kt` | All divisors of a number |
| `divisorCount(num)` | `Factorization.kt` | Count of divisors |
| `eulerTotient(num)` | `Factorization.kt` | Euler's totient function phi(n) |
| `isPrime(num)` | `Prime.kt` | Primality test |
| `getPrimesUpTo(limit)` | `Prime.kt` | All primes up to limit (UInt64, uses cache) |
| `getPrimes(num, constants)` | `Prime.kt` | All primes up to num (generic integer type) |

### Clamping

| Function | File | Description |
|----------|------|-------------|
| `clamp(v, min, max)` | `Clamp.kt` | Restrict value to [min, max] range |

### Min/Max

| Function | File | Description |
|----------|------|-------------|
| `min(lhs, rhs)` | `MinMax.kt` | Smaller of two values |
| `max(lhs, rhs)` | `MinMax.kt` | Larger of two values |
| `minmax(lhs, rhs)` | `MinMax.kt` | Simultaneous min and max of two values |
| `minMax(lhs, vararg rhs)` | `MinMax.kt` | Min and max of multiple values |
| `minOf(lhs, vararg rhs, extractor)` | `MinMax.kt` | Min via extractor function |
| `maxOf(lhs, vararg rhs, extractor)` | `MinMax.kt` | Max via extractor function |
| `Iterable<T>.minMax()` | `MinMax.kt` | Min and max of a collection |
| `Iterable<T>.minMaxOrNull()` | `MinMax.kt` | Min and max of a collection, null if empty |
| `Iterable<U>.minMaxBy(extractor)` | `MinMax.kt` | Min and max elements via extractor |
| `Iterable<U>.minMaxOf(extractor)` | `MinMax.kt` | Min and max extracted values |
| `Iterable<T>.minMaxWith(comparator)` | `MinMax.kt` | Min and max using custom comparator |

### Transcendental (Floating-point)

| Function | File | Description |
|----------|------|-------------|
| `ln(x, constants)` | `Log.kt` | Natural logarithm (Taylor series) |
| `log(x, base, constants)` | `Log.kt` | Arbitrary-base logarithm (change-of-base formula) |
| `pow(base, index)` | `Pow.kt` | Integer-exponent power (fast exponentiation) |
| `powf(base, index)` | `Pow.kt` | Floating-exponent power (via ln and exp) |
| `exp(x)` | `Pow.kt` | Exponential function e^x (Taylor series) |

## Usage

```kotlin
import fuookami.ospf.kotlin.math.ordinary.*
import fuookami.ospf.kotlin.math.algebra.number.*

// GCD and LCM
val g = gcd(UInt64(12), UInt64(8))        // UInt64(4)
val l = lcm(UInt64(4), UInt64(6))         // UInt64(12)
val egcd = extendedGcd(Int64(35), Int64(15))  // ExtendedGcdResult(gcd=5, x=1, y=-2)

// Prime numbers
val prime = isPrime(UInt64(97))            // true
val primes = getPrimesUpTo(UInt64(100))    // [2, 3, 5, 7, 11, ...]

// Factorization
val factors = factorize(UInt64(60))        // [(2, 2), (3, 1), (5, 1)]
val divs = divisors(UInt64(12))            // [1, 2, 3, 4, 6, 12]
val count = divisorCount(UInt64(12))       // 6
val phi = eulerTotient(UInt64(12))         // 4

// Clamp
val clamped = clamp(Flt64(150.0), Flt64(0.0), Flt64(100.0))  // 100.0

// Min/Max
val a = Flt64(3.0)
val b = Flt64(1.0)
val c = Flt64(4.0)
val minimum = min(a, b)                    // Flt64(1.0)
val maximum = max(a, c)                    // Flt64(4.0)
val (lo, hi) = minmax(a, b)               // Pair(Flt64(1.0), Flt64(3.0))
```

## Performance

| Operation | Complexity | Notes |
|-----------|------------|-------|
| `gcd` / `gcdMod` | O(log(min(a,b))) | Euclidean algorithm |
| `lcm` | O(log(min(a,b))) | Uses gcd internally |
| `getPrimesUpTo` | O(n log log n) | Sieve of Eratosthenes |
| `factorize` | O(sqrt(n)) | Trial division with cached primes |
| `divisors` | O(sqrt(n)) | Paired divisor enumeration |
| `eulerTotient` | O(sqrt(n)) | Uses factorization |

## Related

- [Main README](../../README.md)
- [Operator Module](../operator/README.md)
- [Algebra Module](../algebra/README.md)

# ospf-kotlin-math/ordinary

[中文文档 (README_ch.md)](./README_ch.md)

Common mathematical operations including GCD, LCM, prime numbers, factorization, and utility functions for OSPF Kotlin.

## Algorithms

| Function | File | Description |
|----------|------|-------------|
| `gcd` | `GCD.kt` | Greatest common divisor (Euclidean algorithm) |
| `extendedGcd` | `GCD.kt` | Extended GCD (Bezout identity: ax + by = gcd) |
| `lcm` | `LCM.kt` | Least common multiple |
| `getDivisors` | `Factorization.kt` | All divisors of a number |
| `factorize` | `Factorization.kt` | Prime factorization |
| `getPrimeFactors` | `Factorization.kt` | Prime factors with multiplicities |
| `isPrime` | `Prime.kt` | Primality test |
| `getPrimesUpTo` | `Prime.kt` | Sieve of Eratosthenes |
| `getPrimeInRange` | `Prime.kt` | Primes in a range |
| `isPerfectNumber` | `Factorization.kt` | Perfect number check |
| `clamp` | `Clamp.kt` | Value clamping within bounds |
| `min` / `max` | `MinMax.kt` | Minimum and maximum of collections |
| `minBy` / `maxBy` | `MinMax.kt` | Min/max with extractor function |
| `minWith` / `maxWith` | `MinMax.kt` | Min/max with comparator |
| `ln`, `exp`, `pow` | `FltXPowerStrategy.kt` | FltX transcendental operations |
| `log2`, `log10` | `Log.kt` | Base-2 and base-10 logarithms |
| `sqrt`, `cbrt` | `Pow.kt` | Square root and cube root |

## Usage

```kotlin
import fuookami.ospf.kotlin.math.ordinary.*
import fuookami.ospf.kotlin.math.algebra.number.*

// GCD and LCM
val g = gcd(UInt64(12), UInt64(8))        // 4
val l = lcm(UInt64(4), UInt64(6))         // 12

// Prime numbers
val isPrime = Prime.isPrime(UInt64(97))   // true
val primes = Prime.getPrimesUpTo(UInt64(100))  // [2, 3, 5, 7, 11, ...]

// Factorization
val factors = factorize(UInt64(60))       // [(2, 2), (3, 1), (5, 1)]
val divisors = getDivisors(UInt64(12))    // [1, 2, 3, 4, 6, 12]

// Clamp
val clamped = clamp(Flt64(150.0), Flt64(0.0), Flt64(100.0))  // 100.0

// Min/Max
val numbers = listOf(Flt64(3.0), Flt64(1.0), Flt64(4.0))
val minimum = min(numbers)  // Flt64(1.0)
val maximum = max(numbers)  // Flt64(4.0)
```

## Performance

| Operation | Complexity | Notes |
|-----------|------------|-------|
| `gcd` | O(log(min(a,b))) | Euclidean algorithm |
| `lcm` | O(log(min(a,b))) | Uses gcd internally |
| `getPrimesUpTo` | O(n log log n) | Sieve of Eratosthenes |
| `factorize` | O(sqrt(n)) | Trial division |
| `getDivisors` | O(sqrt(n)) | Paired divisor enumeration |

## Related

- [Main README](../../README.md)
- [Operator Module](../operator/README.md)
- [Algebra Module](../algebra/README.md)

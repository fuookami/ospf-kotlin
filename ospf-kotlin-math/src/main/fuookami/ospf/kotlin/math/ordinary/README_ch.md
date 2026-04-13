# ospf-kotlin-math/ordinary

[English Documentation (README.md)](./README.md)

OSPF Kotlin 的常用数学运算，包括 GCD、LCM、质数、因式分解和工具函数。

## 算法

| 函数 | 文件 | 描述 |
|------|------|------|
| `gcd` | `GCD.kt` | 最大公约数（欧几里得算法） |
| `extendedGcd` | `GCD.kt` | 扩展 GCD（贝祖等式：ax + by = gcd） |
| `lcm` | `LCM.kt` | 最小公倍数 |
| `getDivisors` | `Factorization.kt` | 一个数的所有约数 |
| `factorize` | `Factorization.kt` | 质因数分解 |
| `getPrimeFactors` | `Factorization.kt` | 质因子及其重数 |
| `isPrime` | `Prime.kt` | 质数检测 |
| `getPrimesUpTo` | `Prime.kt` | 埃拉托斯特尼筛法 |
| `getPrimeInRange` | `Prime.kt` | 范围内的质数 |
| `isPerfectNumber` | `Factorization.kt` | 完全数检测 |
| `clamp` | `Clamp.kt` | 值范围截断 |
| `min` / `max` | `MinMax.kt` | 集合的最小值和最大值 |
| `minBy` / `maxBy` | `MinMax.kt` | 带提取函数的最小/最大值 |
| `minWith` / `maxWith` | `MinMax.kt` | 带比较器的最小/最大值 |
| `ln`, `exp`, `pow` | `FltXPowerStrategy.kt` | FltX 超越运算 |
| `log2`, `log10` | `Log.kt` | 以 2 和 10 为底的对数 |
| `sqrt`, `cbrt` | `Pow.kt` | 平方根和立方根 |

## 使用示例

```kotlin
import fuookami.ospf.kotlin.math.ordinary.*
import fuookami.ospf.kotlin.math.algebra.number.*

// GCD 和 LCM
val g = gcd(UInt64(12), UInt64(8))        // 4
val l = lcm(UInt64(4), UInt64(6))         // 12

// 质数
val isPrime = Prime.isPrime(UInt64(97))   // true
val primes = Prime.getPrimesUpTo(UInt64(100))  // [2, 3, 5, 7, 11, ...]

// 因式分解
val factors = factorize(UInt64(60))       // [(2, 2), (3, 1), (5, 1)]
val divisors = getDivisors(UInt64(12))    // [1, 2, 3, 4, 6, 12]

// 截断
val clamped = clamp(Flt64(150.0), Flt64(0.0), Flt64(100.0))  // 100.0

// 最小/最大值
val numbers = listOf(Flt64(3.0), Flt64(1.0), Flt64(4.0))
val minimum = min(numbers)  // Flt64(1.0)
val maximum = max(numbers)  // Flt64(4.0)
```

## 性能

| 运算 | 复杂度 | 说明 |
|------|--------|------|
| `gcd` | O(log(min(a,b))) | 欧几里得算法 |
| `lcm` | O(log(min(a,b))) | 内部使用 gcd |
| `getPrimesUpTo` | O(n log log n) | 埃拉托斯特尼筛法 |
| `factorize` | O(sqrt(n)) | 试除法 |
| `getDivisors` | O(sqrt(n)) | 配对约数枚举 |

## 相关链接

- [主 README](../../README.md)
- [Operator 模块](../operator/README_ch.md)
- [Algebra 模块](../algebra/README_ch.md)

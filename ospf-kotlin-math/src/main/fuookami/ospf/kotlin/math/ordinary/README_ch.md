# ospf-kotlin-math/ordinary

[English Documentation (README.md)](./README.md)

OSPF Kotlin 的常用数学运算，包括 GCD、LCM、质数、因式分解和工具函数。

## 算法

### 数论

| 函数 | 文件 | 描述 |
|------|------|------|
| `gcd(x, y)` | `GCD.kt` | 最大公约数（减法算法） |
| `gcdMod(x, y)` | `GCD.kt` | 使用取模算法的 GCD |
| `gcd(numbers, constants)` | `GCD.kt` | 多个整数的 GCD |
| `extendedGcd(a, b)` | `GCD.kt` | 扩展 GCD（贝祖等式：gcd(a,b) = ax + by） |
| `lcm(x, y)` | `LCM.kt` | 最小公倍数（通过 GCD 公式） |
| `lcm(numbers, constants)` | `LCM.kt` | 多个整数的 LCM |
| `lcmByFactorization(numbers)` | `LCM.kt` | 通过质因数分解计算 LCM |
| `factorize(num)` | `Factorization.kt` | 质因数分解，返回 `List<Pair<I, Int>>` |
| `defactorize(factors)` | `Factorization.kt` | 从因式分解结果还原整数 |
| `divisors(num)` | `Factorization.kt` | 一个数的所有因数 |
| `divisorCount(num)` | `Factorization.kt` | 因数个数 |
| `eulerTotient(num)` | `Factorization.kt` | 欧拉函数 phi(n) |
| `isPrime(num)` | `Prime.kt` | 质数检测 |
| `getPrimesUpTo(limit)` | `Prime.kt` | 不超过 limit 的所有素数（UInt64，使用缓存） |
| `getPrimes(num, constants)` | `Prime.kt` | 不超过 num 的所有素数（泛型整数类型） |

### 截断

| 函数 | 文件 | 描述 |
|------|------|------|
| `clamp(v, min, max)` | `Clamp.kt` | 将值限制在 [min, max] 范围内 |

### 最小/最大值

| 函数 | 文件 | 描述 |
|------|------|------|
| `min(lhs, rhs)` | `MinMax.kt` | 两个值中的较小者 |
| `max(lhs, rhs)` | `MinMax.kt` | 两个值中的较大者 |
| `minmax(lhs, rhs)` | `MinMax.kt` | 同时返回两个值的最小值和最大值 |
| `minMax(lhs, vararg rhs)` | `MinMax.kt` | 多个值的最小值和最大值 |
| `minOf(lhs, vararg rhs, extractor)` | `MinMax.kt` | 通过提取函数获取最小值 |
| `maxOf(lhs, vararg rhs, extractor)` | `MinMax.kt` | 通过提取函数获取最大值 |
| `Iterable<T>.minMax()` | `MinMax.kt` | 集合的最小值和最大值 |
| `Iterable<T>.minMaxOrNull()` | `MinMax.kt` | 集合的最小值和最大值，空集合返回 null |
| `Iterable<U>.minMaxBy(extractor)` | `MinMax.kt` | 通过提取器返回最小和最大元素 |
| `Iterable<U>.minMaxOf(extractor)` | `MinMax.kt` | 通过提取器返回最小和最大提取值 |
| `Iterable<T>.minMaxWith(comparator)` | `MinMax.kt` | 使用自定义比较器返回最小值和最大值 |

### 超越函数（浮点数）

| 函数 | 文件 | 描述 |
|------|------|------|
| `ln(x, constants)` | `Log.kt` | 自然对数（泰勒级数展开） |
| `log(x, base, constants)` | `Log.kt` | 任意底数对数（换底公式） |
| `pow(base, index)` | `Pow.kt` | 整数指数幂（快速幂算法） |
| `powf(base, index)` | `Pow.kt` | 浮点指数幂（通过 ln 和 exp 实现） |
| `exp(x)` | `Pow.kt` | 指数函数 e^x（泰勒级数展开） |

## 使用示例

```kotlin
import fuookami.ospf.kotlin.math.ordinary.*
import fuookami.ospf.kotlin.math.algebra.number.*

// GCD 和 LCM
val g = gcd(UInt64(12), UInt64(8))        // UInt64(4)
val l = lcm(UInt64(4), UInt64(6))         // UInt64(12)
val egcd = extendedGcd(Int64(35), Int64(15))  // ExtendedGcdResult(gcd=5, x=1, y=-2)

// 质数
val prime = isPrime(UInt64(97))            // true
val primes = getPrimesUpTo(UInt64(100))    // [2, 3, 5, 7, 11, ...]

// 因式分解
val factors = factorize(UInt64(60))        // [(2, 2), (3, 1), (5, 1)]
val divs = divisors(UInt64(12))            // [1, 2, 3, 4, 6, 12]
val count = divisorCount(UInt64(12))       // 6
val phi = eulerTotient(UInt64(12))         // 4

// 截断
val clamped = clamp(Flt64(150.0), Flt64(0.0), Flt64(100.0))  // 100.0

// 最小/最大值
val a = Flt64(3.0)
val b = Flt64(1.0)
val c = Flt64(4.0)
val minimum = min(a, b)                    // Flt64(1.0)
val maximum = max(a, c)                    // Flt64(4.0)
val (lo, hi) = minmax(a, b)               // Pair(Flt64(1.0), Flt64(3.0))
```

## 性能

| 运算 | 复杂度 | 说明 |
|------|--------|------|
| `gcd` / `gcdMod` | O(log(min(a,b))) | 欧几里得算法 |
| `lcm` | O(log(min(a,b))) | 内部使用 gcd |
| `getPrimesUpTo` | O(n log log n) | 埃拉托斯特尼筛法 |
| `factorize` | O(sqrt(n)) | 试除法（使用缓存质数） |
| `divisors` | O(sqrt(n)) | 配对约数枚举 |
| `eulerTotient` | O(sqrt(n)) | 使用因式分解 |

## 相关链接

- [主 README](../../README.md)
- [Operator 模块](../operator/README_ch.md)
- [Algebra 模块](../algebra/README_ch.md)

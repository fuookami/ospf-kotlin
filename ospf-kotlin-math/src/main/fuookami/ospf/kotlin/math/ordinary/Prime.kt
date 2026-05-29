/**
 * 素数算法
 * Prime Number Algorithm
 *
 * 提供素数判定和素数表生成功能。
 * 素数定义：大二1 且只能被 1 和自身整除的正整数。
 * 边界情况， 和1 不是素数， 是最小的素数。
 * 使用埃拉托斯特尼筛法（Sieve of Eratosthenes）生成素数表，
 * 算法复杂庌O(n log log n)，适用于批量生成素数。
 * PrimeCache 类实现可扩展的素数缓存，支持动态扩展筛法范围，
 * 对于超过缓存范围的大数，使用试除法结合缓存质数进行快速判定。
 * 线程安全：通过 synchronized 锁保证多线程访问的安全性。
 *
 * Provides prime number detection and prime table generation functionality.
 * Prime definition: positive integer greater than 1 divisible only by 1 and itself.
 * Boundary cases: 0 and 1 are not primes; 2 is the smallest prime.
 * Uses Sieve of Eratosthenes to generate prime table,
 * algorithm complexity O(n log log n), suitable for batch prime generation.
 * PrimeCache class implements extensible prime caching with dynamic sieve expansion,
 * for large numbers beyond cache range, uses trial division with cached primes for fast detection.
 * Thread-safe: uses synchronized lock to ensure safe multi-threaded access.
 */
package fuookami.ospf.kotlin.math.ordinary

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*

/**
 * 可扩展的素数缓存
 * Extensible prime cache
 *
 * 使用埃拉托斯特尼筛法维护素数表，支持动态扩展筛法范围。
 * 对于超过缓存范围的大数，使用试除法结合已缓存素数进行快速判定。
 * 通过 synchronized 锁保证多线程访问的安全性。
 *
 * Maintains a prime table using the Sieve of Eratosthenes with dynamic sieve range expansion.
 * For large numbers beyond the cache range, uses trial division with cached primes for fast detection.
 * Thread-safe via synchronized lock for multi-threaded access.
 *
 * @property current 当前筛法的上界 / Current upper bound of the sieve
 * @property isPrime 素性标记数组，isPrime[i] 为 true 表示 i 是素数 / Primality flag array; isPrime[i] is true if i is prime
 * @property primes 已缓存的素数列表 / List of cached primes
 * @property lock 线程同步锁 / Thread synchronization lock
 */
class PrimeCache {
    private var current = UInt64.zero
    private lateinit var isPrime: BooleanArray
    private val primes = mutableListOf<UInt64>()
    private val lock = Any()

    init {
        sieve(UInt64(1000))
    }

    /**
     * 扩展筛法范围至 new
     * Extend sieve range up to new
     *
     * 在已有素数表基础上，将筛法上界从 current 扩展至 new。
     * 先用已知素数标记新区间中的合数，再扫描新区间收集新增素数。
     *
     * Extends the sieve upper bound from current to new on top of the existing prime table.
     * First marks composites in the new interval using known primes, then scans the new interval to collect newly found primes.
     *
     * @param new 新的筛法上界 / New sieve upper bound
     */
    private fun extendSieve(new: UInt64) {
        synchronized(lock) {
            if (new <= current) return

            val old = current
            val oldSize = if (current == UInt64.zero) UInt64.zero else current + UInt64.one

            val newIsPrime = BooleanArray(new.toInt() + 1) { true }
            if (current > UInt64.zero) {
                System.arraycopy(isPrime, 0, newIsPrime, 0, oldSize.toInt())
            } else {
                if (new >= UInt64.zero) newIsPrime[0] = false
                if (new >= UInt64.one) newIsPrime[1] = false
            }

            isPrime = newIsPrime
            current = new

            val sqrtLimit = (new.sqrt() as Flt64).floor().toUInt64()

            for (p in primes) {
                if (p > sqrtLimit) break

                val start = if (p * p > old + UInt64.one) {
                    p * p
                } else {
                    val firstMultiple = ((old + UInt64.one + p - UInt64.one) / p) * p
                    firstMultiple
                }

                for (j in start..new step p) {
                    newIsPrime[j.toInt()] = false
                }
            }

            val start = if (old < UInt64.two) {
                UInt64.two
            } else {
                old + UInt64.one
            }

            for (i in start..new) {
                if (newIsPrime[i.toInt()]) {
                    primes.add(i)

                    if (i <= sqrtLimit) {
                        val startMultiple = maxOf(i * i, start)
                        for (j in startMultiple..new step i) {
                            newIsPrime[j.toInt()] = false
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取不超过 limit 的所有素数
     * Get all primes up to limit
     *
     * @param limit 素数上界 / Prime upper bound
     * @return 不超过 limit 的素数列表 / List of primes up to limit
     */
    fun getPrimes(limit: UInt64): List<UInt64> {
        synchronized(lock) {
            if (limit > current) {
                extendSieve(limit)
            }
            return primes.filter { it <= limit }
        }
    }

    /**
     * 判断 UInt64 是否为素数
     * Check whether a UInt64 value is prime
     *
     * @param num 待判定的数 / Number to check
     * @return 是素数返回 true，否则返回 false / True if prime, false otherwise
     */
    fun isPrime(num: UInt64): Boolean {
        if (num <= UInt64.one) {
            return false
        }
        synchronized(lock) {
            if (num > current) {
                if (num <= UInt64(1000000UL)) {
                    extendSieve(num)
                    return isPrime[num.toInt()]
                } else {
                    return isPrimeQuickCheck(num)
                }
            }
            return isPrime[num.toInt()]
        }
    }

    /**
     * 快速素性判定（试除法）
     * Fast primality check (trial division)
     *
     * 先排除小素数和 2、3 的倍数，再用已缓存素数试除，
     * 最后以 6k±1 步长扫描剩余因子。
     * 适用于超出筛法缓存范围的大数。
     *
     * First eliminates small primes and multiples of 2 and 3, then trial-divides by cached primes,
     * finally scans remaining factors with 6k±1 stride.
     * Suitable for large numbers beyond the sieve cache range.
     *
     * @param n 待判定的正整数 / Positive integer to check
     * @return 是素数返回 true，否则返回 false / True if prime, false otherwise
     */
    private fun isPrimeQuickCheck(n: UInt64): Boolean {
        if (n <= UInt64.one) {
            return false
        }
        if (n <= UInt64.three) {
            return true
        }
        if (n % UInt64.two == UInt64.zero || n % UInt64.three == UInt64.zero) {
            return false
        }

        synchronized(lock) {
            for (p in primes) {
                if (p * p > n) break
                if (n % p == UInt64.zero) return false
            }
        }

        var i = UInt64.five
        while (i * i <= n) {
            if (n % i == UInt64.zero || n % (i + UInt64.two) == UInt64.zero) {
                return false
            }
            i += UInt64(6)
        }
        return true
    }

    /**
     * 初始化埃拉托斯特尼筛法
     * Initialize Sieve of Eratosthenes
     *
     * 生成从 2 到 limit 的完整素数表，填充 isPrime 数组和 primes 列表。
     * 算法复杂度 O(n log log n)。
     *
     * Generates a complete prime table from 2 to limit, populating the isPrime array and primes list.
     * Algorithm complexity O(n log log n).
     *
     * @param limit 筛法上界 / Sieve upper bound
     */
    private fun sieve(limit: UInt64) {
        if (limit <= current) return

        isPrime = BooleanArray(limit.toInt() + 1) { true }
        if (limit >= UInt64.zero) {
            isPrime[0] = false
        }
        if (limit >= UInt64.zero) {
            isPrime[1] = false
        }

        val sqrtLimit = (limit.sqrt() as Flt64).floor().toUInt64()

        for (i in UInt64.two..sqrtLimit) {
            if (isPrime[i.toInt()]) {
                for (j in i * i..limit step i) {
                    isPrime[j.toInt()] = false
                }
            }
        }

        primes.clear()
        for (i in UInt64.two..limit) {
            if (isPrime[i.toInt()]) {
                primes.add(i)
            }
        }

        current = limit
    }
}

internal val cache = PrimeCache()

/**
 * 获取不超过 limit 的所有素数（UInt64 专用，直接使用缓存）
 * Get all primes up to limit (UInt64 only, uses cache directly)
 *
 * @param limit 素数上界 / Prime upper bound
 * @return 不超过 limit 的素数列表 / List of primes up to limit
 */
fun getPrimesUpTo(limit: UInt64): List<UInt64> {

    return cache.getPrimes(limit)
}

/**
 * 判断整数是否为素数
 * Check whether an integer is prime
 *
 * @param I 整数类型 / Integer type
 * @param num 待判定的数 / Number to check
 * @return 是素数返回 true，否则返回 false / True if prime, false otherwise
 */
fun <I> isPrime(num: I): Boolean where I : Integer<I> {

    return cache.isPrime(num.toUInt64())
}

/**
 * 获取不超过 num 的所有素数（内部实现）
 * Get all primes up to num (internal implementation)
 *
 * @param I 整数类型 / Integer type
 * @param num 素数上界 / Prime upper bound
 * @param constants 数值常量提供器 / Real number constants provider
 * @return 不超过 num 的素数列表 / List of primes up to num
 */
fun <I> getPrimesImpl(num: I, constants: RealNumberConstants<I>): List<I> where I : Integer<I> {
    var current = constants.one
    val primes = ArrayList<I>()
    while (current <= num) {
        if (isPrime(current)) {
            primes.add(current)
        }
        current += constants.one
    }
    return primes
}

/**
 * 获取不超过 num 的所有素数
 * Get all primes up to num
 *
 * @param I 整数类型 / Integer type
 * @param num 素数上界 / Prime upper bound
 * @param constants 数值常量提供器 / Real number constants provider
 * @return 不超过 num 的素数列表 / List of primes up to num
 */
fun <I> getPrimes(num: I, constants: RealNumberConstants<I>): List<I> where I : Integer<I> {

    return getPrimesImpl(num, constants)
}

/**
 * 获取不超过 num 的所有素数（自动解析常量）
 * Get all primes up to num (auto-resolve constants)
 *
 * @param I 整数类型 / Integer type
 * @param num 素数上界 / Prime upper bound
 * @return 不超过 num 的素数列表 / List of primes up to num
 */
inline fun <reified I> getPrimes(num: I): List<I> where I : Integer<I> {

    return getPrimes(
        num = num,
        constants = resolveRealNumberConstants<I>("Prime")
    )
}

/**
 * 素数算法 / Prime Number Algorithm
 *
 * 提供素数判定和素数表生成功能。
 * 素数定义：大二1 且只能被 1 和自身整除的正整数。
 * 边界情况， 和1 不是素数， 是最小的素数。
 * 使用埃拉托斯特尼筛法（Sieve of Eratosthenes）生成素数表，
 * 算法复杂庌O(n log log n)，适用于批量生成素数。
 * PrimeCache 类实现可扩展的素数缓存，支持动态扩展筛法范围，
 * 对于超过缓存范围的大数，使用试除法结合缓存质数进行快速判定。
 * 线程安全：读完整快照，扩容使用单写锁。 / Provides prime number detection and prime table generation functionality.
 * Prime definition: positive integer greater than 1 divisible only by 1 and itself.
 * Boundary cases: 0 and 1 are not primes; 2 is the smallest prime.
 * Uses Sieve of Eratosthenes to generate prime table,
 * algorithm complexity O(n log log n), suitable for batch prime generation.
 * PrimeCache class implements extensible prime caching with dynamic sieve expansion,
 * for large numbers beyond cache range, uses trial division with cached primes for fast detection.
 * Thread-safe: readers use complete snapshots; expansion uses a single writer lock.
*/
package fuookami.ospf.kotlin.math.ordinary

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 可扩展的素数缓存 / Extensible prime cache
 *
 * 使用埃拉托斯特尼筛法维护素数表，支持动态扩展筛法范围。
 * 对于超过缓存范围的大数，使用试除法结合已缓存素数进行快速判定。
 * 扩容后原子发布完整快照，已有范围的读取不等待扩容锁。 / Maintains a prime table using the Sieve of Eratosthenes with dynamic sieve range expansion.
 * For large numbers beyond the cache range, uses trial division with cached primes for fast detection.
 * Complete snapshots are published after expansion; covered reads do not wait for the writer lock.
 *
 * @property state 已完整构建且发布后不再修改的筛表 / Complete sieve generation, never mutated after publication
 * @property lock 线程同步锁 / Thread synchronization lock
*/
class PrimeCache {
    private val lock = Any()
    @Volatile
    private var state = sieve(UInt64(1000))

    private data class Sieve(
        val limit: UInt64,
        val flags: BooleanArray,
        val primes: List<UInt64>
    )

    /**
     * 扩展筛法范围至 new / Extend sieve range up to new
     *
     * 在已有素数表基础上，将筛法上界从 current 扩展至 new。
     * 先用已知素数标记新区间中的合数，再扫描新区间收集新增素数。 / Extends the sieve upper bound from current to new on top of the existing prime table.
     * First marks composites in the new interval using known primes, then scans the new interval to collect newly found primes.
     *
     * @param new 新的筛法上界 / New sieve upper bound
     * @return 覆盖目标范围的完整快照 / Complete snapshot covering the requested range
    */
    private fun extendSieve(new: UInt64): Sieve {
        val ready = state
        if (new <= ready.limit) return ready
        return synchronized(lock) {
            val previous = state
            if (new <= previous.limit) return@synchronized previous

            val old = previous.limit
            val oldSize = old + UInt64.one

            val newIsPrime = BooleanArray(new.toInt() + 1) { true }
            System.arraycopy(previous.flags, 0, newIsPrime, 0, oldSize.toInt())
            val primes = ArrayList(previous.primes)

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
            // 旧数组和列表保持只读；不可提前发布半成品。 / Keep old arrays/lists read-only; never publish a partial generation.
            Sieve(
                limit = new,
                flags = newIsPrime,
                primes = primes
            ).also { state = it }
        }
    }

    /**
     * 获取不超过 limit 的所有素数 / Get all primes up to limit
     *
     * @param limit 素数上界 / Prime upper bound
     * @return 不超过 limit 的素数列表 / List of primes up to limit
    */
    fun getPrimes(limit: UInt64): List<UInt64> {
        val snapshot = extendSieve(limit)
        val index = snapshot.primes.binarySearch(limit)
        val end = if (index >= 0) index + 1 else -index - 1
        return snapshot.primes.subList(0, end).toList()
    }

    /**
     * 判断 UInt64 是否为素数 / Check whether a UInt64 value is prime
     *
     * @param num 待判定的数 / Number to check
     * @return 是素数返回 true，否则返回 false / True if prime, false otherwise
    */
    fun isPrime(num: UInt64): Boolean {
        if (num <= UInt64.one) {
            return false
        }
        val snapshot = state
        if (num > snapshot.limit) {
            return if (num <= UInt64(1000000UL)) {
                // 几何扩容避免连续素性查询逐整数复制整张筛表。
                // Grow geometrically so consecutive primality checks do not copy the sieve per integer.
                val expanded = extendSieve(maxOf(num, minOf(snapshot.limit * UInt64.two, UInt64(1000000UL))))
                expanded.flags[num.toInt()]
            } else {
                isPrimeQuickCheck(num, snapshot.primes)
            }
        }
        return snapshot.flags[num.toInt()]
    }

    /**
     * 快速素性判定（试除法） / Fast primality check (trial division)
     *
     * 先排除小素数和 2、3 的倍数，再用已缓存素数试除，
     * 最后以 6k±1 步长扫描剩余因子。
     * 适用于超出筛法缓存范围的大数。 / First eliminates small primes and multiples of 2 and 3, then trial-divides by cached primes,
     * finally scans remaining factors with 6k±1 stride.
     * Suitable for large numbers beyond the sieve cache range.
     *
     * @param n 待判定的正整数 / Positive integer to check
     * @param primes 完整已发布快照的素数前缀 / Prime prefix from a completely published snapshot
     * @return 是素数返回 true，否则返回 false / True if prime, false otherwise
    */
    private fun isPrimeQuickCheck(n: UInt64, primes: List<UInt64>): Boolean {
        if (n <= UInt64.one) {
            return false
        }
        if (n <= UInt64.three) {
            return true
        }
        if (n % UInt64.two == UInt64.zero || n % UInt64.three == UInt64.zero) {
            return false
        }

        for (p in primes) {
            if (p * p > n) break
            if (n % p == UInt64.zero) return false
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
     * 初始化埃拉托斯特尼筛法 / Initialize Sieve of Eratosthenes
     *
     * 生成从 2 到 limit 的完整素数表，填充 isPrime 数组和 primes 列表。
     * 算法复杂度 O(n log log n)。
     *
     * Generates a complete prime table from 2 to limit, populating the isPrime array and primes list.
     * Algorithm complexity O(n log log n).
     *
     * @param limit 筛法上界 / Sieve upper bound
     * @return 尚未发布的完整初始筛表 / Complete initial sieve, not yet published
    */
    private fun sieve(limit: UInt64): Sieve {
        val isPrime = BooleanArray(limit.toInt() + 1) { true }
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

        val primes = ArrayList<UInt64>()
        for (i in UInt64.two..limit) {
            if (isPrime[i.toInt()]) {
                primes.add(i)
            }
        }

        return Sieve(
            limit = limit,
            flags = isPrime,
            primes = primes
        )
    }
}

internal val cache = PrimeCache()

/**
 * 获取不超过 limit 的所有素数（UInt64 专用，直接使用缓存） / Get all primes up to limit (UInt64 only, uses cache directly)
 *
 * @param limit 素数上界 / Prime upper bound
 * @return 不超过 limit 的素数列表 / List of primes up to limit
*/
fun getPrimesUpTo(limit: UInt64): List<UInt64> {

    return cache.getPrimes(limit)
}

/**
 * 判断整数是否为素数 / Check whether an integer is prime
 *
 * @param I 整数类型 / Integer type
 * @param num 待判定的数 / Number to check
 * @return 是素数返回 true，否则返回 false / True if prime, false otherwise
*/
fun <I> isPrime(num: I): Boolean where I : Integer<I> {

    return cache.isPrime(num.toUInt64())
}

/**
 * 获取不超过 num 的所有素数（内部实现） / Get all primes up to num (internal implementation)
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
        if (current == num) {
            break
        }
        current += constants.one
    }
    return primes
}

/**
 * 获取不超过 num 的所有素数 / Get all primes up to num
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
 * 获取不超过 num 的所有素数（自动解析常量） / Get all primes up to num (auto-resolve constants)
 *
 * @param I 整数类型 / Integer type
 * @param num 素数上界 / Prime upper bound
 * @return 不超过 num 的素数列表 / List of primes up to num
*/
inline fun <reified I> getPrimes(num: I): Ret<List<I>> where I : Integer<I> {
    return resolveRealNumberConstantsSafe<I>("Prime").mapResolved { constants ->
        getPrimes(
            num = num,
            constants = constants
        )
    }
}

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

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64

class PrimeCache {
    private var current = UInt64.zero
    private lateinit var isPrime: BooleanArray
    private val primes = mutableListOf<UInt64>()
    private val lock = Any()

    init {
        sieve(UInt64(1000))
    }

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

    fun getPrimes(limit: UInt64): List<UInt64> {
        synchronized(lock) {
            if (limit > current) {
                extendSieve(limit)
            }
            return primes.filter { it <= limit }
        }
    }

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

/** Get primes up to limit directly from the cache (for UInt64). */
fun getPrimesUpTo(limit: UInt64): List<UInt64> {
    return cache.getPrimes(limit)
}

fun <I> isPrime(num: I): Boolean where I : Integer<I> {
    return cache.isPrime(num.toUInt64())
}

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

fun <I> getPrimes(num: I, constants: RealNumberConstants<I>): List<I> where I : Integer<I> {
    return getPrimesImpl(num, constants)
}
inline fun <reified I> getPrimes(num: I): List<I> where I : Integer<I> {
    return getPrimes(
        num = num,
        constants = resolveRealNumberConstants<I>("Prime")
    )
}
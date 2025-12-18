package fuookami.ospf.kotlin.utils.math.ordinary

import kotlin.reflect.full.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*

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

@Suppress("UNCHECKED_CAST")
inline fun <reified I> getPrimes(num: I): List<I> where I : Integer<I> {
    return getPrimesImpl(num, (I::class.companionObjectInstance as RealNumberConstants<I>))
}

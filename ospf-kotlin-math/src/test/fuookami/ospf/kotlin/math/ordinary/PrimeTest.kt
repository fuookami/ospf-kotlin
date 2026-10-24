package fuookami.ospf.kotlin.math.ordinary

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*

class PrimeTest {
    @Test
    fun consecutiveColdQueriesAgreeWithAnIndependentSieve() {
        val cache = PrimeCache()
        val limit = 100_000
        val expected = BooleanArray(limit + 1) { true }
        expected[0] = false
        expected[1] = false
        for (factor in 2..316) {
            if (expected[factor]) {
                for (multiple in factor * factor..limit step factor) expected[multiple] = false
            }
        }
        for (number in 0..limit) {
            assertEquals(expected[number], cache.isPrime(UInt64(number)), "number=$number")
        }
        assertEquals((2..limit).filter { expected[it] }.map { UInt64(it) }, cache.getPrimes(UInt64(limit)))
    }

    @Test
    fun smallerPrimeQueriesReturnIndependentInclusivePrefixes() {
        val cache = PrimeCache()
        val large = cache.getPrimes(UInt64(10_000))
        for (limit in listOf(0, 1, 2, 3, 4, 997, 1000, 1009)) {
            assertEquals(large.filter { it <= UInt64(limit) }, cache.getPrimes(UInt64(limit)))
        }
        val saved = cache.getPrimes(UInt64(1009))
        cache.getPrimes(UInt64(20_000))
        assertEquals(large.filter { it <= UInt64(1009) }, saved)
    }

    @Test
    fun genericPrimeListsHandleSignedBoundsAndMaximumNarrowIntegers() {
        assertTrue(getPrimes(Int64(-1), Int64).isEmpty())
        assertEquals(listOf(2, 3, 5, 7), getPrimes(Int64(10), Int64).map { it.toInt() })
        val expected = getPrimesUpTo(UInt64(255)).map { it.toInt() }
        assertEquals(expected, getPrimes(UInt8.maximum, UInt8).map { it.toInt32().toInt() })
        assertEquals(expected.filter { it <= 127 }, getPrimes(Int8.maximum, Int8).map { it.toInt32().toInt() })
    }

    @Test
    fun isPrime() {
        assert(isPrime(UInt64.two))
        assert(isPrime(UInt64.three))
        assert(!isPrime(UInt64(4)))
        assert(isPrime(UInt64.five))
        assert(!isPrime(UInt64(6)))
        assert(isPrime(UInt64(7)))
        assert(!isPrime(UInt64(8)))
        assert(!isPrime(UInt64(9)))
    }
}

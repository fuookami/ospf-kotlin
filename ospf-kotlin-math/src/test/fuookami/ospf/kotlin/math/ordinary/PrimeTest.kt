package fuookami.ospf.kotlin.math.ordinary

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*

class PrimeTest {
    @Test
    fun readyReadersDoNotWaitForTheExpansionMonitor() {
        val cache = PrimeCache()
        val field = PrimeCache::class.java.getDeclaredField("lock").apply { isAccessible = true }
        val lock = field.get(cache)
        val held = CountDownLatch(1)
        val release = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val writer = pool.submit {
                synchronized(lock) {
                    held.countDown()
                    assertTrue(release.await(10, TimeUnit.SECONDS))
                }
            }
            assertTrue(held.await(10, TimeUnit.SECONDS))
            val reader = pool.submit<Boolean> { cache.isPrime(UInt64(997)) }
            assertTrue(reader.get(2, TimeUnit.SECONDS))
            release.countDown()
            writer.get(10, TimeUnit.SECONDS)
        } finally {
            release.countDown()
            pool.shutdownNow()
        }
    }

    @Test
    fun concurrentExtensionsPublishCompleteGenerationsAndKeepOldPrefixes() {
        val cache = PrimeCache()
        val prefix = cache.getPrimes(UInt64(1000))
        val pool = Executors.newFixedThreadPool(5)
        val start = CountDownLatch(1)
        try {
            val tasks = List(5) { worker -> Callable {
                assertTrue(start.await(10, TimeUnit.SECONDS))
                if (worker == 0) {
                    for (limit in listOf(2000, 10_000, 20_000, 100_000)) cache.getPrimes(UInt64(limit))
                } else {
                    repeat(1000) {
                        assertTrue(cache.isPrime(UInt64(997)))
                        assertFalse(cache.isPrime(UInt64(999)))
                        assertEquals(prefix, cache.getPrimes(UInt64(1000)))
                    }
                }
            } }
            val futures = tasks.map(pool::submit)
            start.countDown()
            futures.forEach { it.get(30, TimeUnit.SECONDS) }
            assertEquals(9592, cache.getPrimes(UInt64(100_000)).size)
            assertEquals(UInt64(997), prefix.last())
            assertTrue(cache.isPrime(UInt64(1_000_003)))
            assertFalse(cache.isPrime(UInt64(1_000_005)))
        } finally {
            start.countDown()
            pool.shutdownNow()
        }
    }

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

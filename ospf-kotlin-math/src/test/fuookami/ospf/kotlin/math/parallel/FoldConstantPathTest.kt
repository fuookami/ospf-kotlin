package fuookami.ospf.kotlin.math.parallel

import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.concept.CompanionConstantProviderResolver
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.parallel.foldParallelly
import fuookami.ospf.kotlin.utils.parallel.foldRightParallelly
import fuookami.ospf.kotlin.utils.parallel.tryFoldParallelly
import fuookami.ospf.kotlin.utils.parallel.exTryFoldParallelly
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FoldConstantPathTest {
    companion object {
        private val propertyKey = CompanionConstantProviderResolver.reflectionFallbackEnabledProperty
        private var previousValue: String? = null

        @JvmStatic
        @BeforeAll
        fun disableReflectionFallback() {
            previousValue = System.getProperty(propertyKey)
            System.setProperty(propertyKey, "false")
        }

        @JvmStatic
        @AfterAll
        fun restoreReflectionFallback() {
            if (previousValue == null) {
                System.clearProperty(propertyKey)
            } else {
                System.setProperty(propertyKey, previousValue)
            }
        }
    }

    @Test
    fun explicitConstantsPathsShouldWorkWhenFallbackDisabled() = runBlocking {
        val numbers = listOf(1, 2, 3)

        val sum = numbers.sumOfParallelly(Int64) { Int64(it.toLong()) }
        assertEquals(Int64(6), sum)

        val trySum = numbers.trySumOfParallelly(Int64) { Ok(Int64(it.toLong())) }
        assertTrue(trySum.ok)
        assertEquals(Int64(6), trySum.value)

        val exTrySum = numbers.exTrySumOfParallelly(Int64) { Ok(Int64(it.toLong())) }
        assertTrue(exTrySum.ok)
        assertEquals(Int64(6), exTrySum.value)
    }

    @Test
    fun reifiedDefaultPathsShouldThrowWhenFallbackDisabled() {
        val numbers = listOf(1, 2, 3)

        assertFailsWith<IllegalStateException> {
            runBlocking {
                numbers.sumOfParallelly<Int, Int64> { Int64(it.toLong()) }
            }
        }
        assertFailsWith<IllegalStateException> {
            runBlocking {
                numbers.trySumOfParallelly<Int, Int64> { Ok(Int64(it.toLong())) }
            }
        }
        assertFailsWith<IllegalStateException> {
            runBlocking {
                numbers.exTrySumOfParallelly<Int, Int64> { Ok(Int64(it.toLong())) }
            }
        }
    }

    @Test
    fun segmentShouldKeepFoldResultsStable() = runBlocking {
        val numbers = listOf(1, 2, 3, 4)

        val leftFoldSegmentOne = numbers.foldParallelly(segment = 1L, initial = 0) { acc, value -> acc + value }
        val leftFoldSegmentThree = numbers.foldParallelly(segment = 3L, initial = 0) { acc, value -> acc + value }
        assertEquals(10, leftFoldSegmentOne)
        assertEquals(10, leftFoldSegmentThree)

        val rightFoldSegmentOne = numbers.foldRightParallelly(segment = 1L, initial = 0) { acc, value -> acc + value }
        val rightFoldSegmentThree = numbers.foldRightParallelly(segment = 3L, initial = 0) { acc, value -> acc + value }
        assertEquals(10, rightFoldSegmentOne)
        assertEquals(10, rightFoldSegmentThree)
    }

    @Test
    fun zeroSegmentShouldBeRejected() {
        assertFailsWith<IllegalArgumentException> {
            runBlocking {
                listOf(1, 2, 3).foldParallelly(segment = 0L, initial = 0) { acc, value -> acc + value }
            }
        }
        assertFailsWith<IllegalArgumentException> {
            runBlocking {
                listOf(1, 2, 3).tryFoldParallelly(segment = 0L, initial = 0) { acc, value -> Ok(acc + value) }
            }
        }
        assertFailsWith<IllegalArgumentException> {
            runBlocking {
                listOf(1, 2, 3).exTryFoldParallelly(segment = 0L, initial = 0) { acc, value -> Ok(acc + value) }
            }
        }
    }
}
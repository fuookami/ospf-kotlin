package fuookami.ospf.kotlin.utils.parallel

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*

class ExParallelTest {
    @Test
    fun exTryMapShouldCollectAllErrors() = runBlocking {
        val invoked = AtomicInteger(0)
        val ret = listOf(1, 2, 3, 4).exTryMapParallelly { value ->
            invoked.incrementAndGet()
            if (value % 2 == 0) {
                Ok(value * 10)
            } else {
                Failed<Int>(ErrorCode.ApplicationException, "bad-$value")
            }
        }
        assertEquals(4, invoked.get())
        assertTrue(ret is Fatal)
        ret as Fatal
        assertEquals(2, ret.errors.size)
    }

    @Test
    fun exTryAllShouldReturnFatalWhenAnyErrorExists() = runBlocking {
        val ret = listOf(1, 2, 3).exTryAllParallelly { value ->
            when (value) {
                1 -> Ok(true)
                2 -> Ok(false)
                else -> Failed<Boolean>(ErrorCode.ApplicationException, "bad")
            }
        }
        assertTrue(ret is Fatal)
        ret as Fatal
        assertEquals(1, ret.errors.size)
    }

    // Test exTryAnyParallelly collects all errors / 测试 exTryAnyParallelly 收集所有错误
    @Test
    fun exTryAnyShouldCollectAllErrors() = runBlocking {
        val invoked = AtomicInteger(0)
        val ret = listOf(1, 2, 3).exTryAnyParallelly { value ->
            invoked.incrementAndGet()
            if (value == 2) {
                Ok(true)
            } else {
                Failed<Boolean>(ErrorCode.ApplicationException, "bad-$value")
            }
        }
        assertEquals(3, invoked.get())
        assertTrue(ret is Fatal)
        ret as Fatal
        assertEquals(2, ret.errors.size)
    }

    // Test exTryCountParallelly collects all errors / 测试 exTryCountParallelly 收集所有错误
    @Test
    fun exTryCountShouldCollectAllErrors() = runBlocking {
        val invoked = AtomicInteger(0)
        val ret = listOf(1, 2, 3, 4).exTryCountParallelly { value ->
            invoked.incrementAndGet()
            if (value % 2 == 0) {
                Ok(true)
            } else {
                Failed<Boolean>(ErrorCode.ApplicationException, "bad-$value")
            }
        }
        assertEquals(4, invoked.get())
        assertTrue(ret is Fatal)
        ret as Fatal
        assertEquals(2, ret.errors.size)
    }

    // Test exTryFilterParallelly collects all errors / 测试 exTryFilterParallelly 收集所有错误
    @Test
    fun exTryFilterShouldCollectAllErrors() = runBlocking {
        val invoked = AtomicInteger(0)
        val ret = listOf(1, 2, 3, 4).exTryFilterParallelly { value ->
            invoked.incrementAndGet()
            if (value % 2 == 0) {
                Ok(true)
            } else {
                Failed<Boolean>(ErrorCode.ApplicationException, "bad-$value")
            }
        }
        assertEquals(4, invoked.get())
        assertTrue(ret is Fatal)
        ret as Fatal
        assertEquals(2, ret.errors.size)
    }

    // Test exTryFlatMapToParallelly collects all errors / 测试 exTryFlatMapToParallelly 收集所有错误
    @Test
    fun exTryFlatMapShouldCollectAllErrors() = runBlocking {
        val invoked = AtomicInteger(0)
        val ret = listOf(1, 2, 3).exTryFlatMapToParallelly { value ->
            invoked.incrementAndGet()
            if (value == 2) {
                Ok(listOf(value * 10, value * 20))
            } else {
                Failed<List<Int>>(ErrorCode.ApplicationException, "bad-$value")
            }
        }
        assertEquals(3, invoked.get())
        assertTrue(ret is Fatal)
        ret as Fatal
        assertEquals(2, ret.errors.size)
    }

    // Test exTryFirstOrNullParallelly collects all errors / 测试 exTryFirstOrNullParallelly 收集所有错误
    @Test
    fun exTryFirstOrNullShouldCollectAllErrors() = runBlocking {
        val invoked = AtomicInteger(0)
        val ret = listOf(1, 2, 3).exTryFirstOrNullParallelly { value ->
            invoked.incrementAndGet()
            if (value == 2) {
                Ok(true)
            } else {
                Failed<Boolean>(ErrorCode.ApplicationException, "bad-$value")
            }
        }
        assertEquals(3, invoked.get())
        assertTrue(ret is Fatal)
        ret as Fatal
        assertEquals(2, ret.errors.size)
    }

    // Test exTryLastOrNullParallelly collects all errors / 测试 exTryLastOrNullParallelly 收集所有错误
    @Test
    fun exTryLastOrNullShouldCollectAllErrors() = runBlocking {
        val invoked = AtomicInteger(0)
        val ret = listOf(1, 2, 3).exTryLastOrNullParallelly { value ->
            invoked.incrementAndGet()
            if (value == 2) {
                Ok(true)
            } else {
                Failed<Boolean>(ErrorCode.ApplicationException, "bad-$value")
            }
        }
        assertEquals(3, invoked.get())
        assertTrue(ret is Fatal)
        ret as Fatal
        assertEquals(2, ret.errors.size)
    }

    // Test exTryAssociateToParallelly collects all errors / 测试 exTryAssociateToParallelly 收集所有错误
    @Test
    fun exTryAssociateShouldCollectAllErrors() = runBlocking {
        val invoked = AtomicInteger(0)
        val ret = listOf(1, 2, 3).exTryAssociateToParallelly { value ->
            invoked.incrementAndGet()
            if (value == 2) {
                Ok(value to value * 10)
            } else {
                Failed<Pair<Int, Int>>(ErrorCode.ApplicationException, "bad-$value")
            }
        }
        assertEquals(3, invoked.get())
        assertTrue(ret is Fatal)
        ret as Fatal
        assertEquals(2, ret.errors.size)
    }

    // Test exTryMaxByOrNullParallelly collects all errors / 测试 exTryMaxByOrNullParallelly 收集所有错误
    @Test
    fun exTryMaxByShouldCollectAllErrors() = runBlocking {
        val invoked = AtomicInteger(0)
        val ret = listOf(1, 2, 3).exTryMaxByOrNullParallelly { value ->
            invoked.incrementAndGet()
            if (value == 2) {
                Ok(value)
            } else {
                Failed<Int>(ErrorCode.ApplicationException, "bad-$value")
            }
        }
        assertEquals(3, invoked.get())
        assertTrue(ret is Fatal)
        ret as Fatal
        assertEquals(2, ret.errors.size)
    }

    // Test exTryMinByOrNullParallelly collects all errors / 测试 exTryMinByOrNullParallelly 收集所有错误
    @Test
    fun exTryMinByShouldCollectAllErrors() = runBlocking {
        val invoked = AtomicInteger(0)
        val ret = listOf(1, 2, 3).exTryMinByOrNullParallelly { value ->
            invoked.incrementAndGet()
            if (value == 2) {
                Ok(value)
            } else {
                Failed<Int>(ErrorCode.ApplicationException, "bad-$value")
            }
        }
        assertEquals(3, invoked.get())
        assertTrue(ret is Fatal)
        ret as Fatal
        assertEquals(2, ret.errors.size)
    }

    // Test exTryFoldParallelly collects all errors / 测试 exTryFoldParallelly 收集所有错误
    @Test
    fun exTryFoldShouldCollectAllErrors() = runBlocking {
        val invoked = AtomicInteger(0)
        val ret = listOf(1, 2, 3).exTryFoldParallelly(0) { acc, value ->
            invoked.incrementAndGet()
            if (value == 2) {
                Ok(acc + value)
            } else {
                Failed<Int>(ErrorCode.ApplicationException, "bad-$value")
            }
        }
        assertEquals(3, invoked.get())
        assertTrue(ret is Fatal)
        ret as Fatal
        assertEquals(2, ret.errors.size)
    }

    // Test exTryFoldRightParallelly collects all errors / 测试 exTryFoldRightParallelly 收集所有错误
    @Test
    fun exTryFoldRightShouldCollectAllErrors() = runBlocking {
        val invoked = AtomicInteger(0)
        val ret = listOf(1, 2, 3).exTryFoldRightParallelly(0) { acc, value ->
            invoked.incrementAndGet()
            if (value == 2) {
                Ok(acc + value)
            } else {
                Failed<Int>(ErrorCode.ApplicationException, "bad-$value")
            }
        }
        assertEquals(3, invoked.get())
        assertTrue(ret is Fatal)
        ret as Fatal
        assertEquals(2, ret.errors.size)
    }

    // Test exTryFoldIndexedParallelly collects all errors / 测试 exTryFoldIndexedParallelly 收集所有错误
    @Test
    fun exTryFoldIndexedShouldCollectAllErrors() = runBlocking {
        val invoked = AtomicInteger(0)
        val ret = listOf(1, 2, 3).exTryFoldIndexedParallelly(0) { index, acc, value ->
            invoked.incrementAndGet()
            if (value == 2) {
                Ok(acc + value + index)
            } else {
                Failed<Int>(ErrorCode.ApplicationException, "bad-$value")
            }
        }
        assertEquals(3, invoked.get())
        assertTrue(ret is Fatal)
        ret as Fatal
        assertEquals(2, ret.errors.size)
    }
}

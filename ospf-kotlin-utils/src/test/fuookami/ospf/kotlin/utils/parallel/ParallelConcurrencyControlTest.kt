package fuookami.ospf.kotlin.utils.parallel

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

/**
 * 并发控制测试
 *
 * Tests for concurrency control in parallel operations.
 *
 * UT-PAR-01: 并发上限参数生效，运行中同时活跃任务数不超过阈值
 * UT-PAR-02: 限流后 try/exTry 语义不变（错误聚合、返回类型一致）
 */
class ParallelConcurrencyControlTest {

    // ==================== UT-PAR-01: 并发上限验证 ====================

    /**
     * 测试 mapParallelly 并发上限控制
     *
     * Test that mapParallelly respects concurrentAmount parameter.
     */
    @Test
    fun mapParallellyShouldRespectConcurrencyLimit() = runBlocking {
        val concurrentLimit = 2uL
        val activeCount = AtomicInteger(0)
        val maxActiveCount = AtomicInteger(0)
        val totalElements = 10

        val result = (1..totalElements).toList().mapParallelly(concurrentAmount = concurrentLimit) { _ ->
            // Track active tasks / 跟踪活跃任务
            val current = activeCount.incrementAndGet()
            maxActiveCount.updateAndGet { max(it, current) }

            // Simulate work / 模拟工作
            delay(50)

            // Decrement when done / 完成时减少
            activeCount.decrementAndGet()
            "result"
        }

        assertEquals(totalElements, result.size)
        assertTrue(maxActiveCount.get() <= concurrentLimit.toInt(),
            "Max active tasks (${maxActiveCount.get()}) should not exceed limit ($concurrentLimit)")
    }

    /**
     * 测试 filterParallelly 并发上限控制
     *
     * Test that filterParallelly respects concurrentAmount parameter.
     */
    @Test
    fun filterParallellyShouldRespectConcurrencyLimit() = runBlocking {
        val concurrentLimit = 3uL
        val activeCount = AtomicInteger(0)
        val maxActiveCount = AtomicInteger(0)
        val totalElements = 15

        val result = (1..totalElements).toList().filterParallelly(concurrentAmount = concurrentLimit) { _ ->
            val current = activeCount.incrementAndGet()
            maxActiveCount.updateAndGet { max(it, current) }
            delay(30)
            activeCount.decrementAndGet()
            true
        }

        assertEquals(totalElements, result.size)
        assertTrue(maxActiveCount.get() <= concurrentLimit.toInt(),
            "Max active tasks (${maxActiveCount.get()}) should not exceed limit ($concurrentLimit)")
    }

    /**
     * 测试 allParallelly 并发上限控制
     *
     * Test that allParallelly respects concurrentAmount parameter.
     */
    @Test
    fun allParallellyShouldRespectConcurrencyLimit() = runBlocking {
        val concurrentLimit = 2uL
        val activeCount = AtomicInteger(0)
        val maxActiveCount = AtomicInteger(0)
        val totalElements = 8

        val result = (1..totalElements).toList().allParallelly(concurrentAmount = concurrentLimit) { _ ->
            val current = activeCount.incrementAndGet()
            maxActiveCount.updateAndGet { max(it, current) }
            delay(20)
            activeCount.decrementAndGet()
            true
        }

        assertTrue(result)
        assertTrue(maxActiveCount.get() <= concurrentLimit.toInt(),
            "Max active tasks (${maxActiveCount.get()}) should not exceed limit ($concurrentLimit)")
    }

    /**
     * 测试默认并发量 (defaultConcurrentAmount) 被使用
     *
     * Test that defaultConcurrentAmount is used when no limit is specified.
     */
    @Test
    fun defaultConcurrentAmountShouldBeUsedWhenNotSpecified() = runBlocking {
        val collection = (1..100).toList()
        val expectedDefault = collection.defaultConcurrentAmount

        // Verify default calculation / 验证默认值计算
        assertTrue(expectedDefault >= 1uL, "Default concurrent amount should be at least 1")
        assertTrue(expectedDefault <= Runtime.getRuntime().availableProcessors().toULong(),
            "Default concurrent amount should not exceed available processors")

        val activeCount = AtomicInteger(0)
        val maxActiveCount = AtomicInteger(0)

        collection.mapParallelly { _ ->
            val current = activeCount.incrementAndGet()
            maxActiveCount.updateAndGet { max(it, current) }
            delay(10)
            activeCount.decrementAndGet()
        }

        // When no limit specified, should still respect some limit
        // 当没有指定限制时，仍应遵守某些限制
        assertTrue(maxActiveCount.get() <= expectedDefault.toInt() * 2, // Allow some overhead
            "Max active tasks (${maxActiveCount.get()}) should be roughly bounded by default ($expectedDefault)")
    }

    // ==================== UT-PAR-02: try/exTry 语义不变 ====================

    /**
     * 测试 tryMapParallelly 并发控制下错误处理语义不变
     *
     * Test that tryMapParallelly error handling semantics remain unchanged under concurrency control.
     */
    @Test
    fun tryMapParallellyShouldMaintainErrorSemantics() = runBlocking {
        val concurrentLimit = 2uL
        val invoked = AtomicInteger(0)

        val ret = listOf(1, 2, 3, 4).tryMapParallelly(concurrentAmount = concurrentLimit) { value ->
            invoked.incrementAndGet()
            if (value % 2 == 0) {
                Ok(value * 10)
            } else {
                Failed<Int>(ErrorCode.ApplicationException, "bad-$value")
            }
        }

        // All elements should be processed / 所有元素应该被处理
        assertEquals(4, invoked.get())
        // Should return first error (Failed semantics) / 应返回第一个错误 (Failed语义)
        assertTrue(ret is Failed)
    }

    /**
     * 测试 exTryMapParallelly 并发控制下错误收集语义不变
     *
     * Test that exTryMapParallelly error collection semantics remain unchanged under concurrency control.
     */
    @Test
    fun exTryMapParallellyShouldCollectAllErrors() = runBlocking {
        val concurrentLimit = 2uL
        val invoked = AtomicInteger(0)

        val ret = listOf(1, 2, 3, 4).exTryMapParallelly(concurrentAmount = concurrentLimit) { value ->
            invoked.incrementAndGet()
            if (value % 2 == 0) {
                Ok(value * 10)
            } else {
                Failed<Int>(ErrorCode.ApplicationException, "bad-$value")
            }
        }

        // All elements should be processed / 所有元素应该被处理
        assertEquals(4, invoked.get())
        // Should collect all errors (Fatal semantics) / 应收集所有错误 (Fatal语义)
        assertTrue(ret is Fatal)
        ret as Fatal
        assertEquals(2, ret.errors.size)
    }

    /**
     * 测试 tryFilterParallelly 并发控制下错误处理语义不变
     *
     * Test that tryFilterParallelly error handling semantics remain unchanged under concurrency control.
     */
    @Test
    fun tryFilterParallellyShouldMaintainErrorSemantics() = runBlocking {
        val concurrentLimit = 3uL
        val invoked = AtomicInteger(0)

        val ret = listOf(1, 2, 3, 4).tryFilterParallelly(concurrentAmount = concurrentLimit) { value ->
            invoked.incrementAndGet()
            if (value % 2 == 0) {
                Ok(true)
            } else {
                Failed<Boolean>(ErrorCode.ApplicationException, "bad-$value")
            }
        }

        assertEquals(4, invoked.get())
        assertTrue(ret is Failed)
    }

    /**
     * 测试 exTryFilterParallelly 并发控制下错误收集语义不变
     *
     * Test that exTryFilterParallelly error collection semantics remain unchanged under concurrency control.
     */
    @Test
    fun exTryFilterParallellyShouldCollectAllErrors() = runBlocking {
        val concurrentLimit = 2uL
        val invoked = AtomicInteger(0)

        val ret = listOf(1, 2, 3, 4).exTryFilterParallelly(concurrentAmount = concurrentLimit) { value ->
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

    /**
     * 测试 exTryAllParallelly 并发控制下语义不变
     *
     * Test that exTryAllParallelly semantics remain unchanged under concurrency control.
     */
    @Test
    fun exTryAllParallellyShouldMaintainSemantics() = runBlocking {
        val concurrentLimit = 2uL
        val invoked = AtomicInteger(0)

        val ret = listOf(1, 2, 3).exTryAllParallelly(concurrentAmount = concurrentLimit) { value ->
            invoked.incrementAndGet()
            when (value) {
                1 -> Ok(true)
                2 -> Ok(false)
                else -> Failed<Boolean>(ErrorCode.ApplicationException, "bad")
            }
        }

        assertEquals(3, invoked.get())
        assertTrue(ret is Fatal)
        ret as Fatal
        assertEquals(1, ret.errors.size)
    }

    /**
     * 测试 exTryAnyParallelly 并发控制下语义不变
     *
     * Test that exTryAnyParallelly semantics remain unchanged under concurrency control.
     */
    @Test
    fun exTryAnyParallellyShouldCollectAllErrors() = runBlocking {
        val concurrentLimit = 2uL
        val invoked = AtomicInteger(0)

        val ret = listOf(1, 2, 3).exTryAnyParallelly(concurrentAmount = concurrentLimit) { value ->
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

    /**
     * 测试 tryMapParallelly 在并发乱序完成时仍保持输入顺序
     *
     * Test that tryMapParallelly preserves input order even when tasks complete out of order.
     */
    @Test
    fun tryMapParallellyShouldPreserveOrderUnderOutOfOrderCompletion() = runBlocking {
        val ret = listOf(1, 2, 3, 4).tryMapParallelly(concurrentAmount = 2uL) { value ->
            delay(((5 - value) * 20).toLong())
            Ok(value * 10)
        }

        assertTrue(ret is Ok)
        ret as Ok
        assertEquals(listOf(10, 20, 30, 40), ret.value)
    }

    /**
     * 测试 exTryMapParallelly 在并发乱序完成时仍保持输入顺序
     *
     * Test that exTryMapParallelly preserves input order even when tasks complete out of order.
     */
    @Test
    fun exTryMapParallellyShouldPreserveOrderUnderOutOfOrderCompletion() = runBlocking {
        val ret = listOf(1, 2, 3, 4).exTryMapParallelly(concurrentAmount = 2uL) { value ->
            delay(((5 - value) * 20).toLong())
            Ok(value * 10)
        }

        assertTrue(ret is Ok)
        ret as Ok
        assertEquals(listOf(10, 20, 30, 40), ret.value)
    }

    // ==================== 边界情况测试 ====================

    /**
     * 测试空集合并发控制
     *
     * Test concurrency control with empty collection.
     */
    @Test
    fun emptyCollectionShouldReturnEmptyResult() = runBlocking {
        val concurrentLimit = 2uL

        val result = emptyList<Int>().mapParallelly(concurrentAmount = concurrentLimit) { it * 2 }
        assertTrue(result.isEmpty())

        val filterResult = emptyList<Int>().filterParallelly(concurrentAmount = concurrentLimit) { true }
        assertTrue(filterResult.isEmpty())

        val allResult = emptyList<Int>().allParallelly(concurrentAmount = concurrentLimit) { true }
        assertTrue(allResult)
    }

    /**
     * 测试单元素集合并发控制
     *
     * Test concurrency control with single element collection.
     */
    @Test
    fun singleElementShouldProcessCorrectly() = runBlocking {
        val concurrentLimit = 2uL

        val result = listOf(1).mapParallelly(concurrentAmount = concurrentLimit) { it * 2 }
        assertEquals(listOf(2), result)

        val filterResult = listOf(1).filterParallelly(concurrentAmount = concurrentLimit) { true }
        assertEquals(listOf(1), filterResult)
    }

    /**
     * 测试并发上限大于集合大小
     *
     * Test concurrentAmount larger than collection size.
     */
    @Test
    fun concurrentLimitLargerThanCollectionSize() = runBlocking {
        val concurrentLimit = 100uL
        val activeCount = AtomicInteger(0)
        val maxActiveCount = AtomicInteger(0)

        val result = (1..5).toList().mapParallelly(concurrentAmount = concurrentLimit) { _ ->
            val current = activeCount.incrementAndGet()
            maxActiveCount.updateAndGet { max(it, current) }
            delay(10)
            activeCount.decrementAndGet()
        }

        // Max active should equal collection size (5)
        // 最大活跃数应等于集合大小 (5)
        assertEquals(5, maxActiveCount.get())
    }
}

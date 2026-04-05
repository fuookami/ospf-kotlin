package fuookami.ospf.kotlin.utils.parallel

import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.functional.ExRet
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min

/**
 * 并行操作工具模块
 *
 * Utility module for parallel operations.
 *
 * RVW-006 改进：使用 Channel 实现真正的协程数量控制。
 * Improvement for RVW-006: Uses Channel to truly control coroutine count.
 * 协程数量与 concurrentAmount 绑定，而非按输入规模预创建。
 * Coroutine count is bound to concurrentAmount, not pre-created by input size.
 */

/**
 * 默认并发量（Collection 版本）
 *
 * 根据集合大小和可用处理器计算合理的并发量。
 * 对于空集合返回 1 以避免 log(0) = -inf。
 *
 * Default concurrent amount based on collection size and available processors.
 * Returns 1 for empty collections to avoid log(0) = -inf.
 *
 * @receiver 集合 / Collection
 * @return 默认并发量 / Default concurrent amount
 */
val Collection<*>.defaultConcurrentAmount: ULong
    get() = if (this.isEmpty()) {
        1uL
    } else {
        maxOf(
            minOf(
                log2(this.size.toDouble()).toInt(),
                Runtime.getRuntime().availableProcessors()
            ),
            1
        ).toULong()
    }

/**
 * 默认并发量（Iterable 版本）
 *
 * 对于非 Collection 的 Iterable，使用可用处理器数作为默认值。
 *
 * Default concurrent amount for Iterables that are not Collections.
 * Uses available processors as the default.
 *
 * @receiver 可迭代对象 / Iterable
 * @return 默认并发量 / Default concurrent amount
 */
val Iterable<*>.defaultConcurrentAmount: ULong
    get() = if (this is Collection<*>) {
        this.defaultConcurrentAmount
    } else {
        // For non-collection Iterables, use available processors
        // 对于非 Collection 的 Iterable，使用可用处理器数
        Runtime.getRuntime().availableProcessors().toULong().coerceAtLeast(1uL)
    }

/**
 * 解析并发量参数
 *
 * Resolve concurrent amount parameter, using default if null.
 *
 * @param concurrentAmount 指定的并发量 / Specified concurrent amount
 * @param default 默认并发量 / Default concurrent amount
 * @return 解析后的并发量 / Resolved concurrent amount
 */
@PublishedApi
internal fun resolveConcurrentAmount(concurrentAmount: ULong?, default: ULong): ULong {
    return concurrentAmount ?: default
}

/**
 * 创建并发控制信号量
 *
 * Create semaphore for concurrency control.
 *
 * @param concurrentAmount 并发上限 / Concurrency limit
 * @return 信号量实例 / Semaphore instance
 */
@PublishedApi
internal fun createConcurrencySemaphore(concurrentAmount: ULong): Semaphore {
    return Semaphore(concurrentAmount.toInt().coerceAtLeast(1))
}

/**
 * 并发限流执行块
 *
 * Execute block with concurrency throttling using semaphore.
 *
 * @param semaphore 信号量 / Semaphore
 * @param block 执行块 / Execution block
 * @return 执行结果 / Execution result
 */
@PublishedApi
internal suspend inline fun <T> withConcurrencyLimit(
    semaphore: Semaphore,
    crossinline block: suspend () -> T
): T {
    return withContext(Dispatchers.Default) {
        semaphore.acquire()
        try {
            block()
        } finally {
            semaphore.release()
        }
    }
}

/**
 * 从 Ret 结果中提取错误并添加到列表
 *
 * Extract errors from a Ret result and append to the list.
 *
 * @receiver 错误列表 / Error list
 * @param ret 结果对象 / Result object
 */
@PublishedApi
internal fun MutableList<Error>.appendFrom(ret: Ret<*>) {
    when (ret) {
        is Ok -> {}
        is Failed -> add(ret.error)
        is Fatal -> addAll(ret.errors)
    }
}

/**
 * 根据错误列表创建结果
 *
 * Create result based on error list.
 *
 * @param value 成功时的值 / Value for success case
 * @param errors 错误列表 / Error list
 * @return 如果无错误返回 Ok，否则返回 Fatal / Ok if no errors, Fatal otherwise
 */
@PublishedApi
internal fun <T> exResultOf(value: T, errors: List<Error>): ExRet<T> {
    return if (errors.isEmpty()) {
        Ok(value)
    } else {
        Fatal(errors)
    }
}

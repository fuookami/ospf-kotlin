package fuookami.ospf.kotlin.utils.parallel

import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.ExRet
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Warn
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min

/**
 * 并行操作工具模块
 *
 * Utility module for parallel operations.
 *
 * RVW-009 改进：使用 Channel + Worker Pool 实现真正的协程数量控制。
 * Improvement for RVW-009: Uses Channel + Worker Pool to truly control coroutine count.
 * 协程数量与 concurrentAmount 绑定，而非按输入规模预创建。
 * Coroutine count is bound to concurrentAmount, not pre-created by input size.
 *
 * 方案说明：
 * 1. 创建 Channel 用于任务分发
 * 2. 启动固定数量的 worker 协程（= concurrentAmount）
 * 3. 每个 worker 从 Channel 接收任务并执行
 * 4. 发送端将所有任务发送到 Channel 后关闭
 * 5. 等待所有 worker 完成
 *
 * 这样可以保证：
 * - 协程创建数量 = concurrentAmount（固定）
 * - 任务分发通过 Channel，不会预创建大量协程
 * - 大集合场景下不会有协程爆发问题
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
 * Worker Pool 任务包装器
 *
 * Task wrapper for Worker Pool execution.
 *
 * @param T 任务输入类型 / Task input type
 * @param index 任务索引 / Task index
 * @param element 任务元素 / Task element
 */
@PublishedApi
internal data class WorkerPoolTask<T>(
    val index: Int,
    val element: T
)

/**
 * Worker Pool 结果包装器
 *
 * Result wrapper for Worker Pool execution.
 *
 * @param R 结果类型 / Result type
 * @param index 任务索引（用于保持顺序）/ Task index (for ordering)
 * @param result 执行结果 / Execution result
 */
@PublishedApi
internal data class WorkerPoolResult<R>(
    val index: Int,
    val result: R
)

/**
 * 使用 Worker Pool 执行并行任务（协程数量 = concurrentAmount）
 *
 * Execute parallel tasks using Worker Pool (coroutine count = concurrentAmount).
 *
 * 这是核心并发控制实现，确保协程创建数量与 concurrentAmount 绑定。
 * This is the core concurrency control implementation, ensuring coroutine count is bound to concurrentAmount.
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param elements 任务元素迭代器 / Task element iterator
 * @param concurrentAmount 并发上限 / Concurrency limit
 * @param extractor 提取器函数 / Extractor function
 * @return 结果列表（保持原顺序）/ Result list (preserves original order)
 */
@PublishedApi
internal suspend inline fun <R, T> executeWithWorkerPool(
    elements: Iterable<T>,
    concurrentAmount: ULong,
    crossinline extractor: suspend (index: Int, element: T) -> R
): List<R> {
    val elementList = elements.toList()
    val limit = concurrentAmount.toInt().coerceAtLeast(1)

    // 当并发量为 1 时，直接顺序执行更高效
    // When concurrentAmount = 1, sequential execution is more efficient
    if (limit == 1) {
        return elementList.mapIndexed { index, element -> extractor(index, element) }
    }

    // 当并发上限 >= 元素数量时，为每个元素创建一个协程（全部并行）
    // When limit >= element count, create one coroutine per element (full parallelism)
    if (limit >= elementList.size) {
        return kotlinx.coroutines.coroutineScope {
            val results = arrayOfNulls<Any?>(elementList.size)
            val jobs = elementList.mapIndexed { index, element ->
                async(Dispatchers.Default) {
                    results[index] = extractor(index, element)
                }
            }
            jobs.forEach { it.await() }
            results.mapIndexed { _, v -> v as R }
        }
    }

    // 使用 Channel + Worker Pool 方案
    // Use Channel + Worker Pool approach
    val taskChannel = Channel<WorkerPoolTask<T>>(capacity = limit)
    val resultChannel = Channel<WorkerPoolResult<R>>(capacity = elementList.size)

    return kotlinx.coroutines.coroutineScope {
        // 启动固定数量的 worker 协程
        // Launch fixed number of worker coroutines
        val workers = List(limit) { workerId ->
            launch(Dispatchers.Default) {
                // 每个 worker 从 Channel 接收任务并执行
                // Each worker receives tasks from Channel and executes
                for (task in taskChannel) {
                    val result = try {
                        extractor(task.index, task.element)
                    } catch (e: Exception) {
                        // 异常处理：将异常作为结果传递
                        // Exception handling: pass exception as result
                        throw e
                    }
                    resultChannel.send(WorkerPoolResult(task.index, result))
                }
            }
        }

        // 发送所有任务到 Channel
        // Send all tasks to Channel
        launch {
            for ((index, element) in elementList.withIndex()) {
                taskChannel.send(WorkerPoolTask(index, element))
            }
            // 发送完毕后关闭 Channel，workers 会自动结束
            // Close Channel after sending, workers will end automatically
            taskChannel.close()
        }

        // 等待所有 worker 完成
        // Wait for all workers to complete
        for (worker in workers) {
            worker.join()
        }
        resultChannel.close()

        // 收集结果并按索引排序（保持原顺序）
        // Collect results and sort by index (preserves original order)
        val results = mutableListOf<WorkerPoolResult<R>>()
        for (result in resultChannel) {
            results.add(result)
        }
        results.sortBy { it.index }
        results.map { it.result }
    }
}

/**
 * 使用 Worker Pool 执行并行任务（带错误处理）
 *
 * Execute parallel tasks using Worker Pool with error handling.
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param elements 任务元素迭代器 / Task element iterator
 * @param concurrentAmount 并发上限 / Concurrency limit
 * @param extractor 提取器函数（返回 Ret）/ Extractor function (returns Ret)
 * @return 结果列表或错误 / Result list or error
 */
@PublishedApi
internal suspend inline fun <R, T> executeTryWithWorkerPool(
    elements: Iterable<T>,
    concurrentAmount: ULong,
    crossinline extractor: suspend (index: Int, element: T) -> Ret<R>
): Ret<List<R>> {
    val elementList = elements.toList()
    val limit = concurrentAmount.toInt().coerceAtLeast(1)

    if (elementList.size <= limit || limit == 1) {
        // 顺序执行
        // Sequential execution
        val results = mutableListOf<R>()
        for ((index, element) in elementList.withIndex()) {
            when (val ret = extractor(index, element)) {
                is Ok -> results.add(ret.value)
                is Failed -> return Failed(ret.error)
                is Fatal -> return Fatal(ret.errors)
            }
        }
        return Ok(results)
    }

    // 使用 Channel + Worker Pool 方案
    // Use Channel + Worker Pool approach
    val taskChannel = Channel<WorkerPoolTask<T>>(capacity = limit)
    val resultChannel = Channel<WorkerPoolResult<Ret<R>>>(capacity = elementList.size)

    return kotlinx.coroutines.coroutineScope {
        // 启动 worker 协程
        // Launch worker coroutines
        val workers = List(limit) {
            launch(Dispatchers.Default) {
                for (task in taskChannel) {
                    val result = extractor(task.index, task.element)
                    resultChannel.send(WorkerPoolResult(task.index, result))
                }
            }
        }

        // 发送任务
        // Send tasks
        launch {
            for ((index, element) in elementList.withIndex()) {
                taskChannel.send(WorkerPoolTask(index, element))
            }
            taskChannel.close()
        }

        // 等待完成并收集结果
        // Wait for completion and collect results
        for (worker in workers) {
            worker.join()
        }
        resultChannel.close()

        // 处理结果（按索引还原顺序）
        // Process results (restore original order by index)
        val orderedResults = arrayOfNulls<Any?>(elementList.size)
        for (resultWrapper in resultChannel) {
            when (val ret = resultWrapper.result) {
                is Ok -> orderedResults[resultWrapper.index] = ret.value
                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(orderedResults.map { it as R })
    }
}

/**
 * 使用 Worker Pool 执行并行任务（收集所有错误）
 *
 * Execute parallel tasks using Worker Pool, collecting all errors.
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param elements 任务元素迭代器 / Task element iterator
 * @param concurrentAmount 并发上限 / Concurrency limit
 * @param extractor 提取器函数（返回 Ret）/ Extractor function (returns Ret)
 * @return 结果列表或错误集合 / Result list or error collection
 */
@PublishedApi
internal suspend inline fun <R, T> executeExTryWithWorkerPool(
    elements: Iterable<T>,
    concurrentAmount: ULong,
    crossinline extractor: suspend (index: Int, element: T) -> Ret<R>
): ExRet<List<R>> {
    val elementList = elements.toList()
    val limit = concurrentAmount.toInt().coerceAtLeast(1)

    if (elementList.size <= limit || limit == 1) {
        // 顺序执行
        // Sequential execution
        val results = mutableListOf<R>()
        val errors = mutableListOf<Error<ErrorCode>>()
        for ((index, element) in elementList.withIndex()) {
            when (val ret = extractor(index, element)) {
                is Ok -> results.add(ret.value)
                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        return exResultOf(results, errors)
    }

    // 使用 Channel + Worker Pool 方案
    // Use Channel + Worker Pool approach
    val taskChannel = Channel<WorkerPoolTask<T>>(capacity = limit)
    val resultChannel = Channel<WorkerPoolResult<Ret<R>>>(capacity = elementList.size)

    return kotlinx.coroutines.coroutineScope {
        // 启动 worker 协程
        // Launch worker coroutines
        val workers = List(limit) {
            launch(Dispatchers.Default) {
                for (task in taskChannel) {
                    val result = extractor(task.index, task.element)
                    resultChannel.send(WorkerPoolResult(task.index, result))
                }
            }
        }

        // 发送任务
        // Send tasks
        launch {
            for ((index, element) in elementList.withIndex()) {
                taskChannel.send(WorkerPoolTask(index, element))
            }
            taskChannel.close()
        }

        // 等待完成并收集结果
        // Wait for completion and collect results
        for (worker in workers) {
            worker.join()
        }
        resultChannel.close()

        // 处理结果（按索引还原顺序）
        // Process results (restore original order by index)
        val orderedResults = arrayOfNulls<Any?>(elementList.size)
        val errors = mutableListOf<Error<ErrorCode>>()
        for (resultWrapper in resultChannel) {
            when (val ret = resultWrapper.result) {
                is Ok -> orderedResults[resultWrapper.index] = ret.value
                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(orderedResults.map { it as R }, errors)
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
internal fun MutableList<Error<ErrorCode>>.appendFrom(ret: Ret<*>) {
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
internal fun <T> exResultOf(value: T, errors: List<Error<ErrorCode>>): ExRet<T> {
    return if (errors.isEmpty()) {
        Ok<T, ErrorCode, Error<ErrorCode>>(value)
    } else {
        Fatal<T, ErrorCode, Error<ErrorCode>>(errors)
    }
}

// ============================================================================
// Worker Pool 扩展函数 - 用于特定操作类型
// Worker Pool extension functions for specific operation types
// ============================================================================

/**
 * 使用 Worker Pool 执行谓词判断（all/any/none/count 操作）
 *
 * Execute predicate operations using Worker Pool.
 *
 * @param T 元素类型 / Element type
 * @param elements 任务元素迭代器 / Task element iterator
 * @param concurrentAmount 并发上限 / Concurrency limit
 * @param extractor 谓词函数 / Predicate function
 * @return 谓词结果列表 / Predicate result list
 */
@PublishedApi
internal suspend inline fun <T> executePredicateWithWorkerPool(
    elements: Iterable<T>,
    concurrentAmount: ULong,
    crossinline extractor: suspend (index: Int, element: T) -> Boolean
): List<Boolean> {
    return executeWithWorkerPool(elements, concurrentAmount, extractor)
}

/**
 * 使用 Worker Pool 执行谓词判断（带错误处理）
 *
 * Execute predicate operations using Worker Pool with error handling.
 *
 * @param T 元素类型 / Element type
 * @param elements 任务元素迭代器 / Task element iterator
 * @param concurrentAmount 并发上限 / Concurrency limit
 * @param extractor 谓词函数（返回 Ret）/ Predicate function (returns Ret)
 * @return 谓词结果列表或错误 / Predicate result list or error
 */
@PublishedApi
internal suspend inline fun <T> executeTryPredicateWithWorkerPool(
    elements: Iterable<T>,
    concurrentAmount: ULong,
    crossinline extractor: suspend (index: Int, element: T) -> Ret<Boolean>
): Ret<List<Boolean>> {
    return executeTryWithWorkerPool(elements, concurrentAmount, extractor)
}

/**
 * 使用 Worker Pool 执行过滤操作
 *
 * Execute filter operations using Worker Pool.
 *
 * @param T 元素类型 / Element type
 * @param elements 任务元素迭代器 / Task element iterator
 * @param concurrentAmount 并发上限 / Concurrency limit
 * @param predicate 谓词函数 / Predicate function
 * @return 过滤结果（元素 + 布尔值）/ Filter result (element + boolean)
 */
@PublishedApi
internal suspend inline fun <T> executeFilterWithWorkerPool(
    elements: Iterable<T>,
    concurrentAmount: ULong,
    crossinline predicate: suspend (index: Int, element: T) -> Boolean
): List<Pair<T, Boolean>> {
    return executeWithWorkerPool(elements, concurrentAmount) { index, element ->
        element to predicate(index, element)
    }
}

/**
 * 使用 Worker Pool 执行过滤操作（带错误处理）
 *
 * Execute filter operations using Worker Pool with error handling.
 *
 * @param T 元素类型 / Element type
 * @param elements 任务元素迭代器 / Task element iterator
 * @param concurrentAmount 并发上限 / Concurrency limit
 * @param predicate 谓词函数（返回 Ret）/ Predicate function (returns Ret)
 * @return 过滤结果或错误 / Filter result or error
 */
@PublishedApi
internal suspend inline fun <T> executeTryFilterWithWorkerPool(
    elements: Iterable<T>,
    concurrentAmount: ULong,
    crossinline predicate: suspend (index: Int, element: T) -> Ret<Boolean>
): Ret<List<Pair<T, Boolean>>> {
    return executeTryWithWorkerPool(elements, concurrentAmount) { index, element ->
        when (val ret = predicate(index, element)) {
            is Ok -> Ok(element to ret.value)
            is Failed -> Failed(ret.error)
            is Fatal -> Fatal(ret.errors)
        }
    }
}

/**
 * 使用 Worker Pool 执行 FlatMap 操作
 *
 * Execute flatMap operations using Worker Pool.
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param elements 任务元素迭代器 / Task element iterator
 * @param concurrentAmount 并发上限 / Concurrency limit
 * @param extractor 提取器函数 / Extractor function
 * @return 展平后的结果列表 / Flattened result list
 */
@PublishedApi
internal suspend inline fun <R, T> executeFlatMapWithWorkerPool(
    elements: Iterable<T>,
    concurrentAmount: ULong,
    crossinline extractor: suspend (index: Int, element: T) -> Iterable<R>
): List<R> {
    val results = executeWithWorkerPool<Iterable<R>, T>(elements, concurrentAmount, extractor)
    return results.flatten()
}

/**
 * 使用 Worker Pool 执行 FlatMap 操作（带错误处理）
 *
 * Execute flatMap operations using Worker Pool with error handling.
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param elements 任务元素迭代器 / Task element iterator
 * @param concurrentAmount 并发上限 / Concurrency limit
 * @param extractor 提取器函数（返回 Ret）/ Extractor function (returns Ret)
 * @return 展平后的结果列表或错误 / Flattened result list or error
 */
@PublishedApi
internal suspend inline fun <R, T> executeTryFlatMapWithWorkerPool(
    elements: Iterable<T>,
    concurrentAmount: ULong,
    crossinline extractor: suspend (index: Int, element: T) -> Ret<Iterable<R>>
): Ret<List<R>> {
    val result = executeTryWithWorkerPool<Iterable<R>, T>(elements, concurrentAmount, extractor)
    return when (result) {
        is Ok -> Ok(result.value.flatten())
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

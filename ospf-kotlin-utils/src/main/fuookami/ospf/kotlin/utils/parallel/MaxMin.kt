/**
 * 并行最小最大值操作
 *
 * Parallel min-max operations (returns both min and max in one pass) with concurrency control.
 */
package fuookami.ospf.kotlin.utils.parallel

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

// ============================================================================
// maxBy 系列
// maxBy series
// ============================================================================

/**
 * 并行查找最大值元素（按选择器）
 *
 * Find the element with maximum value by selector in parallel.
 * 并发查找按选择器值最大的元素。
 *
 * @param T 元素类型 / Element type
 * @param R 可比较的选择器值类型 / Comparable selector value type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param selector 选择器函数 / Selector function
 * @return 最大值元素 / Element with maximum value
 * @throws NoSuchElementException 如果集合为空 / If collection is empty
 */
suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxByParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendExtractor<R, T>
): T {
    return maxByOrNullParallelly(concurrentAmount, selector)
        ?: throw NoSuchElementException("Collection is empty.")
}

/**
 * 并行查找最大值元素（按选择器，带错误处理）
 *
 * Find the element with maximum value by selector in parallel with error handling.
 * 并发查找按选择器值最大的元素，支持错误处理。
 *
 * @param T 元素类型 / Element type
 * @param R 可比较的选择器值类型 / Comparable selector value type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param selector 选择器函数（返回 Ret）/ Selector function (returns Ret)
 * @return 最大值元素或错误 / Element with maximum value or error
 */
suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMaxByParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendTryExtractor<R, T>
): Ret<T> {
    return when (val result = tryMaxByOrNullParallelly(concurrentAmount, selector)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection is empty."))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行查找最大值元素（按选择器，带错误收集）
 *
 * Find the element with maximum value by selector in parallel with error collection.
 * 并发查找按选择器值最大的元素，收集所有错误。
 *
 * @param T 元素类型 / Element type
 * @param R 可比较的选择器值类型 / Comparable selector value type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param selector 选择器函数（返回 Ret）/ Selector function (returns Ret)
 * @return 最大值元素或错误集合 / Element with maximum value or error collection
 */
suspend inline fun <T, R : Comparable<R>> Iterable<T>.exTryMaxByParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendTryExtractor<R, T>
): ExRet<T> {
    return when (val result = exTryMaxByOrNullParallelly(concurrentAmount, selector)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection is empty."))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection is empty."))
    }
}

/**
 * 并行查找最大值元素（按选择器，可能为空）
 *
 * Find the element with maximum value by selector in parallel, or null if empty.
 * 并发查找按选择器值最大的元素，如果集合为空则返回 null。
 *
 * @param T 元素类型 / Element type
 * @param R 可比较的选择器值类型 / Comparable selector value type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param selector 选择器函数 / Selector function
 * @return 最大值元素或 null / Element with maximum value or null
 */
suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxByOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendExtractor<R, T>
): T? {
    val elements = toList()
    if (elements.isEmpty()) return null

    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val results = executeWithWorkerPool<R, T>(elements, limit) { _, element -> selector(element) }

    var bestIndex = 0
    var bestValue = results[0]
    for (index in 1 until results.size) {
        if (results[index] > bestValue) {
            bestValue = results[index]
            bestIndex = index
        }
    }
    return elements[bestIndex]
}

/**
 * 并行查找最大值元素（按选择器，带错误处理，可能为空）
 *
 * Find the element with maximum value by selector in parallel with error handling, or null if empty.
 * 并发查找按选择器值最大的元素，支持错误处理，如果集合为空则返回 null。
 *
 * @param T 元素类型 / Element type
 * @param R 可比较的选择器值类型 / Comparable selector value type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param selector 选择器函数（返回 Ret）/ Selector function (returns Ret)
 * @return 最大值元素或 null 或错误 / Element with maximum value or null or error
 */
suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMaxByOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendTryExtractor<R, T>
): Ret<T?> {
    val elements = toList()
    if (elements.isEmpty()) return Ok(null)

    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val result = executeTryWithWorkerPool<R, T>(elements, limit) { _, element -> selector(element) }

    return when (result) {
        is Ok -> {
            var bestIndex = 0
            var bestValue = result.value[0]
            for (index in 1 until result.value.size) {
                if (result.value[index] > bestValue) {
                    bestValue = result.value[index]
                    bestIndex = index
                }
            }
            Ok(elements[bestIndex])
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行查找最大值元素（按选择器，带错误收集，可能为空）
 *
 * Find the element with maximum value by selector in parallel with error collection, or null if empty.
 * 并发查找按选择器值最大的元素，收集所有错误，如果集合为空则返回 null。
 *
 * @param T 元素类型 / Element type
 * @param R 可比较的选择器值类型 / Comparable selector value type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param selector 选择器函数（返回 Ret）/ Selector function (returns Ret)
 * @return 最大值元素或 null 或错误集合 / Element with maximum value or null or error collection
 */
suspend inline fun <T, R : Comparable<R>> Iterable<T>.exTryMaxByOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendTryExtractor<R, T>
): ExRet<T?> {
    val elements = toList()
    if (elements.isEmpty()) return Ok(null)

    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val result = executeExTryWithWorkerPool<R, T>(elements, limit) { _, element -> selector(element) }

    return when (result) {
        is Ok -> {
            var bestIndex = 0
            var bestValue = result.value[0]
            for (index in 1 until result.value.size) {
                if (result.value[index] > bestValue) {
                    bestValue = result.value[index]
                    bestIndex = index
                }
            }
            Ok(elements[bestIndex])
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            var bestIndex = 0
            var bestValue = result.value[0]
            for (index in 1 until result.value.size) {
                if (result.value[index] > bestValue) {
                    bestValue = result.value[index]
                    bestIndex = index
                }
            }
            Ok(elements[bestIndex])
        }
    }
}

// ============================================================================
// minBy 系列
// minBy series
// ============================================================================

/**
 * 并行查找最小值元素（按选择器）
 *
 * Find the element with minimum value by selector in parallel.
 * 并发查找按选择器值最小的元素。
 *
 * @param T 元素类型 / Element type
 * @param R 可比较的选择器值类型 / Comparable selector value type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param selector 选择器函数 / Selector function
 * @return 最小值元素 / Element with minimum value
 * @throws NoSuchElementException 如果集合为空 / If collection is empty
 */
suspend inline fun <T, R : Comparable<R>> Iterable<T>.minByParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendExtractor<R, T>
): T {
    return minByOrNullParallelly(concurrentAmount, selector)
        ?: throw NoSuchElementException("Collection is empty.")
}

/**
 * 并行查找最小值元素（按选择器，带错误处理）
 *
 * Find the element with minimum value by selector in parallel with error handling.
 * 并发查找按选择器值最小的元素，支持错误处理。
 *
 * @param T 元素类型 / Element type
 * @param R 可比较的选择器值类型 / Comparable selector value type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param selector 选择器函数（返回 Ret）/ Selector function (returns Ret)
 * @return 最小值元素或错误 / Element with minimum value or error
 */
suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinByParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendTryExtractor<R, T>
): Ret<T> {
    return when (val result = tryMinByOrNullParallelly(concurrentAmount, selector)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection is empty."))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行查找最小值元素（按选择器，带错误收集）
 *
 * Find the element with minimum value by selector in parallel with error collection.
 * 并发查找按选择器值最小的元素，收集所有错误。
 *
 * @param T 元素类型 / Element type
 * @param R 可比较的选择器值类型 / Comparable selector value type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param selector 选择器函数（返回 Ret）/ Selector function (returns Ret)
 * @return 最小值元素或错误集合 / Element with minimum value or error collection
 */
suspend inline fun <T, R : Comparable<R>> Iterable<T>.exTryMinByParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendTryExtractor<R, T>
): ExRet<T> {
    return when (val result = exTryMinByOrNullParallelly(concurrentAmount, selector)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection is empty."))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection is empty."))
    }
}

/**
 * 并行查找最小值元素（按选择器，可能为空）
 *
 * Find the element with minimum value by selector in parallel, or null if empty.
 * 并发查找按选择器值最小的元素，如果集合为空则返回 null。
 *
 * @param T 元素类型 / Element type
 * @param R 可比较的选择器值类型 / Comparable selector value type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param selector 选择器函数 / Selector function
 * @return 最小值元素或 null / Element with minimum value or null
 */
suspend inline fun <T, R : Comparable<R>> Iterable<T>.minByOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendExtractor<R, T>
): T? {
    val elements = toList()
    if (elements.isEmpty()) return null

    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val results = executeWithWorkerPool<R, T>(elements, limit) { _, element -> selector(element) }

    var bestIndex = 0
    var bestValue = results[0]
    for (index in 1 until results.size) {
        if (results[index] < bestValue) {
            bestValue = results[index]
            bestIndex = index
        }
    }
    return elements[bestIndex]
}

/**
 * 并行查找最小值元素（按选择器，带错误处理，可能为空）
 *
 * Find the element with minimum value by selector in parallel with error handling, or null if empty.
 * 并发查找按选择器值最小的元素，支持错误处理，如果集合为空则返回 null。
 *
 * @param T 元素类型 / Element type
 * @param R 可比较的选择器值类型 / Comparable selector value type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param selector 选择器函数（返回 Ret）/ Selector function (returns Ret)
 * @return 最小值元素或 null 或错误 / Element with minimum value or null or error
 */
suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinByOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendTryExtractor<R, T>
): Ret<T?> {
    val elements = toList()
    if (elements.isEmpty()) return Ok(null)

    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val result = executeTryWithWorkerPool<R, T>(elements, limit) { _, element -> selector(element) }

    return when (result) {
        is Ok -> {
            var bestIndex = 0
            var bestValue = result.value[0]
            for (index in 1 until result.value.size) {
                if (result.value[index] < bestValue) {
                    bestValue = result.value[index]
                    bestIndex = index
                }
            }
            Ok(elements[bestIndex])
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行查找最小值元素（按选择器，带错误收集，可能为空）
 *
 * Find the element with minimum value by selector in parallel with error collection, or null if empty.
 * 并发查找按选择器值最小的元素，收集所有错误，如果集合为空则返回 null。
 *
 * @param T 元素类型 / Element type
 * @param R 可比较的选择器值类型 / Comparable selector value type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param selector 选择器函数（返回 Ret）/ Selector function (returns Ret)
 * @return 最小值元素或 null 或错误集合 / Element with minimum value or null or error collection
 */
suspend inline fun <T, R : Comparable<R>> Iterable<T>.exTryMinByOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendTryExtractor<R, T>
): ExRet<T?> {
    val elements = toList()
    if (elements.isEmpty()) return Ok(null)

    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val result = executeExTryWithWorkerPool<R, T>(elements, limit) { _, element -> selector(element) }

    return when (result) {
        is Ok -> {
            var bestIndex = 0
            var bestValue = result.value[0]
            for (index in 1 until result.value.size) {
                if (result.value[index] < bestValue) {
                    bestValue = result.value[index]
                    bestIndex = index
                }
            }
            Ok(elements[bestIndex])
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            var bestIndex = 0
            var bestValue = result.value[0]
            for (index in 1 until result.value.size) {
                if (result.value[index] < bestValue) {
                    bestValue = result.value[index]
                    bestIndex = index
                }
            }
            Ok(elements[bestIndex])
        }
    }
}

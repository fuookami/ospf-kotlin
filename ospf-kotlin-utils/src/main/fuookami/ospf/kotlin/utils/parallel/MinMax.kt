/**
 * 并行最大/最小值操作
 *
 * Parallel max/min operations with concurrency control.
 */
package fuookami.ospf.kotlin.utils.parallel

import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.ExRet
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Warn
import fuookami.ospf.kotlin.utils.functional.SuspendExtractor
import fuookami.ospf.kotlin.utils.functional.SuspendTryExtractor

/**
 * 并行查找最小最大值元素（按选择器）
 *
 * Find both minimum and maximum elements by selector in a single pass in parallel.
 * 并发单次遍历查找最小和最大元素。
 *
 * @param T 元素类型 / Element type
 * @param R 可比较的选择器值类型 / Comparable selector value type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param selector 选择器函数 / Selector function
 * @return 最小和最大元素对 / Pair of minimum and maximum elements
 * @throws NoSuchElementException 如果集合为空 / If collection is empty
 */
suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxByParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendExtractor<R, T>
): Pair<T, T> {
    return minMaxByOrNullParallelly(concurrentAmount, selector)
        ?: throw NoSuchElementException("Collection is empty.")
}

/**
 * 并行查找最小最大值元素（按选择器，带错误处理）
 *
 * Find both minimum and maximum elements by selector in a single pass in parallel with error handling.
 * 并发单次遍历查找最小和最大元素，支持错误处理。
 *
 * @param T 元素类型 / Element type
 * @param R 可比较的选择器值类型 / Comparable selector value type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param selector 选择器函数（返回 Ret）/ Selector function (returns Ret)
 * @return 最小和最大元素对或错误 / Pair of minimum and maximum elements or error
 */
suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinMaxByParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendTryExtractor<R, T>
): Ret<Pair<T, T>> {
    return when (val result = tryMinMaxByOrNullParallelly(concurrentAmount, selector)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection is empty."))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行查找最小最大值元素（按选择器，带错误收集）
 *
 * Find both minimum and maximum elements by selector in a single pass in parallel with error collection.
 * 并发单次遍历查找最小和最大元素，收集所有错误。
 *
 * @param T 元素类型 / Element type
 * @param R 可比较的选择器值类型 / Comparable selector value type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param selector 选择器函数（返回 Ret）/ Selector function (returns Ret)
 * @return 最小和最大元素对或错误集合 / Pair of minimum and maximum elements or error collection
 */
suspend inline fun <T, R : Comparable<R>> Iterable<T>.exTryMinMaxByParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendTryExtractor<R, T>
): ExRet<Pair<T, T>> {
    return when (val result = exTryMinMaxByOrNullParallelly(concurrentAmount, selector)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection is empty."))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection is empty."))
    }
}

/**
 * 并行查找最小最大值元素（按选择器，可能为空）
 *
 * Find both minimum and maximum elements by selector in a single pass in parallel, or null if empty.
 * 并发单次遍历查找最小和最大元素，如果集合为空则返回 null。
 *
 * @param T 元素类型 / Element type
 * @param R 可比较的选择器值类型 / Comparable selector value type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param selector 选择器函数 / Selector function
 * @return 最小和最大元素对或 null / Pair of minimum and maximum elements or null
 */
suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxByOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendExtractor<R, T>
): Pair<T, T>? {
    val elements = toList()
    if (elements.isEmpty()) return null

    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val results = executeWithWorkerPool<R, T>(elements, limit) { _, element -> selector(element) }

    var minIndex = 0
    var maxIndex = 0
    var minValue = results[0]
    var maxValue = results[0]

    for (index in 1 until results.size) {
        val value = results[index]
        if (value < minValue) {
            minValue = value
            minIndex = index
        }
        if (value > maxValue) {
            maxValue = value
            maxIndex = index
        }
    }
    return elements[minIndex] to elements[maxIndex]
}

/**
 * 并行查找最小最大值元素（按选择器，带错误处理，可能为空）
 *
 * Find both minimum and maximum elements by selector in a single pass in parallel with error handling, or null if empty.
 * 并发单次遍历查找最小和最大元素，支持错误处理，如果集合为空则返回 null。
 *
 * @param T 元素类型 / Element type
 * @param R 可比较的选择器值类型 / Comparable selector value type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param selector 选择器函数（返回 Ret）/ Selector function (returns Ret)
 * @return 最小和最大元素对或 null 或错误 / Pair of minimum and maximum elements or null or error
 */
suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinMaxByOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendTryExtractor<R, T>
): Ret<Pair<T, T>?> {
    val elements = toList()
    if (elements.isEmpty()) return Ok(null)

    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val result = executeTryWithWorkerPool<R, T>(elements, limit) { _, element -> selector(element) }

    return when (result) {
        is Ok -> {
            var minIndex = 0
            var maxIndex = 0
            var minValue = result.value[0]
            var maxValue = result.value[0]

            for (index in 1 until result.value.size) {
                val value = result.value[index]
                if (value < minValue) {
                    minValue = value
                    minIndex = index
                }
                if (value > maxValue) {
                    maxValue = value
                    maxIndex = index
                }
            }
            Ok(elements[minIndex] to elements[maxIndex])
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行查找最小最大值元素（按选择器，带错误收集，可能为空）
 *
 * Find both minimum and maximum elements by selector in a single pass in parallel with error collection, or null if empty.
 * 并发单次遍历查找最小和最大元素，收集所有错误，如果集合为空则返回 null。
 *
 * @param T 元素类型 / Element type
 * @param R 可比较的选择器值类型 / Comparable selector value type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param selector 选择器函数（返回 Ret）/ Selector function (returns Ret)
 * @return 最小和最大元素对或 null 或错误集合 / Pair of minimum and maximum elements or null or error collection
 */
suspend inline fun <T, R : Comparable<R>> Iterable<T>.exTryMinMaxByOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendTryExtractor<R, T>
): ExRet<Pair<T, T>?> {
    val elements = toList()
    if (elements.isEmpty()) return Ok(null)

    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val result = executeExTryWithWorkerPool<R, T>(elements, limit) { _, element -> selector(element) }

    return when (result) {
        is Ok -> {
            var minIndex = 0
            var maxIndex = 0
            var minValue = result.value[0]
            var maxValue = result.value[0]

            for (index in 1 until result.value.size) {
                val value = result.value[index]
                if (value < minValue) {
                    minValue = value
                    minIndex = index
                }
                if (value > maxValue) {
                    maxValue = value
                    maxIndex = index
                }
            }
            Ok(elements[minIndex] to elements[maxIndex])
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            var minIndex = 0
            var maxIndex = 0
            var minValue = result.value[0]
            var maxValue = result.value[0]

            for (index in 1 until result.value.size) {
                val value = result.value[index]
                if (value < minValue) {
                    minValue = value
                    minIndex = index
                }
                if (value > maxValue) {
                    maxValue = value
                    maxIndex = index
                }
            }
            Ok(elements[minIndex] to elements[maxIndex])
        }
    }
}

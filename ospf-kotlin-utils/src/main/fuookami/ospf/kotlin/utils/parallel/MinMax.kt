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
 * 并行最小最大值操作
 *
 * Parallel min-max operations (returns both min and max in one pass) with concurrency control.
 *
 * RVW-009 改进：使用 Worker Pool 方案实现真正的协程数量控制。
 * Improvement for RVW-009: Uses Worker Pool to truly control coroutine count.
 * 协程数量与 concurrentAmount 绑定，而非按输入规模预创建。
 * Coroutine count is bound to concurrentAmount, not pre-created by input size.
 */

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxByParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendExtractor<R, T>
): Pair<T, T> {
    return minMaxByOrNullParallelly(concurrentAmount, selector)
        ?: throw NoSuchElementException("Collection is empty.")
}

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
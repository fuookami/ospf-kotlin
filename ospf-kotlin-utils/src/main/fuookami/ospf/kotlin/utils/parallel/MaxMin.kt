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
 * 并行最大/最小值操作
 *
 * Parallel max/min operations with concurrency control.
 *
 * RVW-009 改进：使用 Worker Pool 方案实现真正的协程数量控制。
 * Improvement for RVW-009: Uses Worker Pool to truly control coroutine count.
 * 协程数量与 concurrentAmount 绑定，而非按输入规模预创建。
 * Coroutine count is bound to concurrentAmount, not pre-created by input size.
 */

// ============================================================================
// maxBy 系列
// ============================================================================

suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxByParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendExtractor<R, T>
): T {
    return maxByOrNullParallelly(concurrentAmount, selector)
        ?: throw NoSuchElementException("Collection is empty.")
}

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
// ============================================================================

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minByParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendExtractor<R, T>
): T {
    return minByOrNullParallelly(concurrentAmount, selector)
        ?: throw NoSuchElementException("Collection is empty.")
}

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
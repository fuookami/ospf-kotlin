package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
 * 并发控制已实现：使用 Semaphore 限制同时活跃的协程数量。
 * Concurrency control implemented: Uses Semaphore to limit active coroutines.
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
        is Warn -> result.value?.let { Warn(it, result.warnings) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection is empty."))
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxByOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendExtractor<R, T>
): Pair<T, T>? {
    val elements = toList()
    if (elements.isEmpty()) {
        return null
    }
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    selector(element)
                } finally {
                    semaphore.release()
                }
            })
        }
        var minIndex = 0
        var maxIndex = 0
        var minValue: R? = null
        var maxValue: R? = null
        for ((index, promise) in promises.withIndex()) {
            val value = promise.await()
            if (minValue == null || value < minValue!!) {
                minValue = value
                minIndex = index
            }
            if (maxValue == null || value > maxValue!!) {
                maxValue = value
                maxIndex = index
            }
        }
        elements[minIndex] to elements[maxIndex]
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinMaxByOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendTryExtractor<R, T>
): Ret<Pair<T, T>?> {
    val elements = toList()
    if (elements.isEmpty()) {
        return Ok(null)
    }
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R>>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    selector(element)
                } finally {
                    semaphore.release()
                }
            })
        }
        var minIndex = 0
        var maxIndex = 0
        var minValue: R? = null
        var maxValue: R? = null
        for ((index, promise) in promises.withIndex()) {
            when (val ret = promise.await()) {
                is Ok -> {
                    val value = ret.value
                    if (minValue == null || value < minValue!!) {
                        minValue = value
                        minIndex = index
                    }
                    if (maxValue == null || value > maxValue!!) {
                        maxValue = value
                        maxIndex = index
                    }
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(elements[minIndex] to elements[maxIndex])
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.exTryMinMaxByOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendTryExtractor<R, T>
): ExRet<Pair<T, T>?> {
    val elements = toList()
    if (elements.isEmpty()) {
        return Ok(null)
    }
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R>>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    selector(element)
                } finally {
                    semaphore.release()
                }
            })
        }
        val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error>()
        var minIndex = 0
        var maxIndex = 0
        var minValue: R? = null
        var maxValue: R? = null
        for ((index, promise) in promises.withIndex()) {
            when (val ret = promise.await()) {
                is Ok -> {
                    val value = ret.value
                    if (minValue == null || value < minValue!!) {
                        minValue = value
                        minIndex = index
                    }
                    if (maxValue == null || value > maxValue!!) {
                        maxValue = value
                        maxIndex = index
                    }
                }

                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(
            if (minValue != null && maxValue != null) {
                elements[minIndex] to elements[maxIndex]
            } else {
                null
            },
            errors
        )
    }
}
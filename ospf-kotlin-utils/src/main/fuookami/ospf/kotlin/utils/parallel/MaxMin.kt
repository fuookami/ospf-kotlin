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
 * 并行最大/最小值操作
 *
 * Parallel max/min operations with concurrency control.
 *
 * 并发控制已实现：使用 Semaphore 限制同时活跃的协程数量。
 * Concurrency control implemented: Uses Semaphore to limit active coroutines.
 */

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
        is Warn -> result.value?.let { Warn(it, result.warnings) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection is empty."))
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxByOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendExtractor<R, T>
): T? {
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
        var bestIndex = 0
        var bestValue: R? = null
        for ((index, promise) in promises.withIndex()) {
            val value = promise.await()
            if (bestValue == null || value > bestValue!!) {
                bestValue = value
                bestIndex = index
            }
        }
        elements[bestIndex]
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMaxByOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendTryExtractor<R, T>
): Ret<T?> {
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
        var bestIndex = 0
        var bestValue: R? = null
        for ((index, promise) in promises.withIndex()) {
            when (val ret = promise.await()) {
                is Ok -> {
                    val value = ret.value
                    if (bestValue == null || value > bestValue!!) {
                        bestValue = value
                        bestIndex = index
                    }
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(elements[bestIndex])
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.exTryMaxByOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendTryExtractor<R, T>
): ExRet<T?> {
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
        var bestIndex = 0
        var bestValue: R? = null
        for ((index, promise) in promises.withIndex()) {
            when (val ret = promise.await()) {
                is Ok -> {
                    val value = ret.value
                    if (bestValue == null || value > bestValue!!) {
                        bestValue = value
                        bestIndex = index
                    }
                }

                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(bestValue?.let { elements[bestIndex] }, errors)
    }
}

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
        is Warn -> result.value?.let { Warn(it, result.warnings) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection is empty."))
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minByOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendExtractor<R, T>
): T? {
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
        var bestIndex = 0
        var bestValue: R? = null
        for ((index, promise) in promises.withIndex()) {
            val value = promise.await()
            if (bestValue == null || value < bestValue!!) {
                bestValue = value
                bestIndex = index
            }
        }
        elements[bestIndex]
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinByOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendTryExtractor<R, T>
): Ret<T?> {
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
        var bestIndex = 0
        var bestValue: R? = null
        for ((index, promise) in promises.withIndex()) {
            when (val ret = promise.await()) {
                is Ok -> {
                    val value = ret.value
                    if (bestValue == null || value < bestValue!!) {
                        bestValue = value
                        bestIndex = index
                    }
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(elements[bestIndex])
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.exTryMinByOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline selector: SuspendTryExtractor<R, T>
): ExRet<T?> {
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
        var bestIndex = 0
        var bestValue: R? = null
        for ((index, promise) in promises.withIndex()) {
            when (val ret = promise.await()) {
                is Ok -> {
                    val value = ret.value
                    if (bestValue == null || value < bestValue!!) {
                        bestValue = value
                        bestIndex = index
                    }
                }

                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(bestValue?.let { elements[bestIndex] }, errors)
    }
}
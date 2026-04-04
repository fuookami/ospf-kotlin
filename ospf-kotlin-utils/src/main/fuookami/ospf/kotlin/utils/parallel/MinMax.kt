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
 * Parallel min-max operations (returns both min and max in one pass).
 *
 * UTL-005 TODO: 添加 concurrentAmount 参数控制并发上限
 * UTL-005 TODO: Add concurrentAmount parameter for concurrency control.
 */

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxByParallelly(
    crossinline selector: SuspendExtractor<R, T>
): Pair<T, T> {
    return minMaxByOrNullParallelly(selector)
        ?: throw NoSuchElementException("Collection is empty.")
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinMaxByParallelly(
    crossinline selector: SuspendTryExtractor<R, T>
): Ret<Pair<T, T>> {
    return when (val result = tryMinMaxByOrNullParallelly(selector)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection is empty."))

        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.exTryMinMaxByParallelly(
    crossinline selector: SuspendTryExtractor<R, T>
): ExRet<Pair<T, T>> {
    return when (val result = exTryMinMaxByOrNullParallelly(selector)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection is empty."))

        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> result.value?.let { Warn(it, result.warnings) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection is empty."))
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxByOrNullParallelly(
    crossinline selector: SuspendExtractor<R, T>
): Pair<T, T>? {
    val elements = toList()
    if (elements.isEmpty()) {
        return null
    }
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) { selector(element) })
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
    crossinline selector: SuspendTryExtractor<R, T>
): Ret<Pair<T, T>?> {
    val elements = toList()
    if (elements.isEmpty()) {
        return Ok(null)
    }
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R>>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) { selector(element) })
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
    crossinline selector: SuspendTryExtractor<R, T>
): ExRet<Pair<T, T>?> {
    val elements = toList()
    if (elements.isEmpty()) {
        return Ok(null)
    }
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R>>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) { selector(element) })
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

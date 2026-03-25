package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.SuspendExtractor
import fuookami.ospf.kotlin.utils.functional.SuspendTryExtractor

suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxByParallelly(
    crossinline selector: SuspendExtractor<R, T>
): T {
    return maxByOrNullParallelly(selector)
        ?: throw NoSuchElementException("Collection is empty.")
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMaxByParallelly(
    crossinline selector: SuspendTryExtractor<R, T>
): Ret<T> {
    return when (val result = tryMaxByOrNullParallelly(selector)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection is empty."))

        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxByOrNullParallelly(
    crossinline selector: SuspendExtractor<R, T>
): T? {
    val elements = toList()
    if (elements.isEmpty()) {
        return null
    }
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) { selector(element) })
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
    crossinline selector: SuspendTryExtractor<R, T>
): Ret<T?> {
    val elements = toList()
    if (elements.isEmpty()) {
        return Ok(null)
    }
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R>>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) { selector(element) })
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

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minByParallelly(
    crossinline selector: SuspendExtractor<R, T>
): T {
    return minByOrNullParallelly(selector)
        ?: throw NoSuchElementException("Collection is empty.")
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinByParallelly(
    crossinline selector: SuspendTryExtractor<R, T>
): Ret<T> {
    return when (val result = tryMinByOrNullParallelly(selector)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection is empty."))

        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minByOrNullParallelly(
    crossinline selector: SuspendExtractor<R, T>
): T? {
    val elements = toList()
    if (elements.isEmpty()) {
        return null
    }
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) { selector(element) })
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
    crossinline selector: SuspendTryExtractor<R, T>
): Ret<T?> {
    val elements = toList()
    if (elements.isEmpty()) {
        return Ok(null)
    }
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R>>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) { selector(element) })
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

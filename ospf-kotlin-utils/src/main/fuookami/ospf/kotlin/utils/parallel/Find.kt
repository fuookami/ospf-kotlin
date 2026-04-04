package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.ExRet
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Warn
import fuookami.ospf.kotlin.utils.functional.SuspendExtractor
import fuookami.ospf.kotlin.utils.functional.SuspendPredicate
import fuookami.ospf.kotlin.utils.functional.SuspendTryExtractor
import fuookami.ospf.kotlin.utils.functional.SuspendTryPredicate

/**
 * 并行查找操作
 *
 * Parallel find operations (first, firstOrNull, last, lastOrNull).
 *
 * UTL-005 TODO: 添加 concurrentAmount 参数控制并发上限
 * UTL-005 TODO: Add concurrentAmount parameter for concurrency control.
 */

suspend inline fun <T> Iterable<T>.firstParallelly(
    crossinline predicate: SuspendPredicate<T>
): T {
    return firstOrNullParallelly(predicate)
        ?: throw NoSuchElementException("Collection contains no element matching the predicate.")
}

suspend inline fun <T> Iterable<T>.tryFirstParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T> {
    return when (val result = tryFirstOrNullParallelly(predicate)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))

        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

suspend inline fun <T> Iterable<T>.exTryFirstParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<T> {
    return when (val result = exTryFirstOrNullParallelly(predicate)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))

        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> result.value?.let { Warn(it, result.warnings) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))
    }
}

suspend inline fun <T> Iterable<T>.firstOrNullParallelly(
    crossinline predicate: SuspendPredicate<T>
): T? {
    val elements = toList()
    return coroutineScope {
        val promises = ArrayList<Deferred<Boolean>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) { predicate(element) })
        }
        for ((index, promise) in promises.withIndex()) {
            if (promise.await()) {
                return@coroutineScope elements[index]
            }
        }
        null
    }
}

suspend inline fun <T> Iterable<T>.tryFirstOrNullParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    val elements = toList()
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Boolean>>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) { predicate(element) })
        }
        for ((index, promise) in promises.withIndex()) {
            when (val ret = promise.await()) {
                is Ok -> {
                    if (ret.value) {
                        return@coroutineScope Ok(elements[index])
                    }
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(null)
    }
}

suspend inline fun <T> Iterable<T>.exTryFirstOrNullParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<T?> {
    val elements = toList()
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Boolean>>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) { predicate(element) })
        }
        val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error>()
        var matched: T? = null
        for ((index, promise) in promises.withIndex()) {
            when (val ret = promise.await()) {
                is Ok -> {
                    if (matched == null && ret.value) {
                        matched = elements[index]
                    }
                }

                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(matched, errors)
    }
}

suspend inline fun <R, T> Iterable<T>.firstNotNullOfParallelly(
    crossinline extractor: SuspendExtractor<R?, T>
): R {
    return firstNotNullOfOrNullParallelly(extractor)
        ?: throw NoSuchElementException("No element of the collection was transformed to a non-null value.")
}

suspend inline fun <R, T> Iterable<T>.tryFirstNotNullOfParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<R> {
    return when (val result = tryFirstNotNullOfOrNullParallelly(extractor)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(
                Err(
                    ErrorCode.ApplicationException,
                    "No element of the collection was transformed to a non-null value."
                )
            )

        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

suspend inline fun <R, T> Iterable<T>.exTryFirstNotNullOfParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): ExRet<R> {
    return when (val result = exTryFirstNotNullOfOrNullParallelly(extractor)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(
                Err(
                    ErrorCode.ApplicationException,
                    "No element of the collection was transformed to a non-null value."
                )
            )

        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> result.value?.let { Warn(it, result.warnings) }
            ?: Failed(
                Err(
                    ErrorCode.ApplicationException,
                    "No element of the collection was transformed to a non-null value."
                )
            )
    }
}

suspend inline fun <R, T> Iterable<T>.firstNotNullOfOrNullParallelly(
    crossinline extractor: SuspendExtractor<R?, T>
): R? {
    val elements = toList()
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) { extractor(element) })
        }
        for (promise in promises) {
            val value = promise.await()
            if (value != null) {
                return@coroutineScope value
            }
        }
        null
    }
}

suspend inline fun <R, T> Iterable<T>.tryFirstNotNullOfOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<R?> {
    val elements = toList()
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R?>>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) { extractor(element) })
        }
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> {
                    val value = ret.value
                    if (value != null) {
                        return@coroutineScope Ok(value)
                    }
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(null)
    }
}

suspend inline fun <R, T> Iterable<T>.exTryFirstNotNullOfOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): ExRet<R?> {
    val elements = toList()
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R?>>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) { extractor(element) })
        }
        val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error>()
        var result: R? = null
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> {
                    if (result == null) {
                        result = ret.value
                    }
                }

                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(result, errors)
    }
}

suspend inline fun <T> Iterable<T>.lastParallelly(
    crossinline predicate: SuspendPredicate<T>
): T {
    return lastOrNullParallelly(predicate)
        ?: throw NoSuchElementException("Collection contains no element matching the predicate.")
}

suspend inline fun <T> Iterable<T>.tryLastParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T> {
    return when (val result = tryLastOrNullParallelly(predicate)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))

        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

suspend inline fun <T> Iterable<T>.exTryLastParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<T> {
    return when (val result = exTryLastOrNullParallelly(predicate)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))

        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> result.value?.let { Warn(it, result.warnings) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))
    }
}

suspend inline fun <T> Iterable<T>.lastOrNullParallelly(
    crossinline predicate: SuspendPredicate<T>
): T? {
    val elements = toList()
    return coroutineScope {
        val promises = ArrayList<Deferred<Boolean>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) { predicate(element) })
        }
        for (index in promises.indices.reversed()) {
            if (promises[index].await()) {
                return@coroutineScope elements[index]
            }
        }
        null
    }
}

suspend inline fun <T> Iterable<T>.tryLastOrNullParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    val elements = toList()
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Boolean>>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) { predicate(element) })
        }
        for (index in promises.indices.reversed()) {
            when (val ret = promises[index].await()) {
                is Ok -> {
                    if (ret.value) {
                        return@coroutineScope Ok(elements[index])
                    }
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(null)
    }
}

suspend inline fun <T> Iterable<T>.exTryLastOrNullParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<T?> {
    val elements = toList()
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Boolean>>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) { predicate(element) })
        }
        val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error>()
        var matched: T? = null
        for (index in promises.indices.reversed()) {
            when (val ret = promises[index].await()) {
                is Ok -> {
                    if (matched == null && ret.value) {
                        matched = elements[index]
                    }
                }

                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(matched, errors)
    }
}

suspend inline fun <R, T> Iterable<T>.lastNotNullOfParallelly(
    crossinline extractor: SuspendExtractor<R?, T>
): R {
    return lastNotNullOfOrNullParallelly(extractor)
        ?: throw NoSuchElementException("No element of the collection was transformed to a non-null value.")
}

suspend inline fun <R, T> Iterable<T>.tryLastNotNullOfParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<R> {
    return when (val result = tryLastNotNullOfOrNullParallelly(extractor)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(
                Err(
                    ErrorCode.ApplicationException,
                    "No element of the collection was transformed to a non-null value."
                )
            )

        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

suspend inline fun <R, T> Iterable<T>.exTryLastNotNullOfParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): ExRet<R> {
    return when (val result = exTryLastNotNullOfOrNullParallelly(extractor)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(
                Err(
                    ErrorCode.ApplicationException,
                    "No element of the collection was transformed to a non-null value."
                )
            )

        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> result.value?.let { Warn(it, result.warnings) }
            ?: Failed(
                Err(
                    ErrorCode.ApplicationException,
                    "No element of the collection was transformed to a non-null value."
                )
            )
    }
}

suspend inline fun <R, T> Iterable<T>.lastNotNullOfOrNullParallelly(
    crossinline extractor: SuspendExtractor<R?, T>
): R? {
    val elements = toList()
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) { extractor(element) })
        }
        for (index in promises.indices.reversed()) {
            val value = promises[index].await()
            if (value != null) {
                return@coroutineScope value
            }
        }
        null
    }
}

suspend inline fun <R, T> Iterable<T>.tryLastNotNullOfOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<R?> {
    val elements = toList()
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R?>>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) { extractor(element) })
        }
        for (index in promises.indices.reversed()) {
            when (val ret = promises[index].await()) {
                is Ok -> {
                    val value = ret.value
                    if (value != null) {
                        return@coroutineScope Ok(value)
                    }
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(null)
    }
}

suspend inline fun <R, T> Iterable<T>.exTryLastNotNullOfOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): ExRet<R?> {
    val elements = toList()
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R?>>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) { extractor(element) })
        }
        val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error>()
        var result: R? = null
        for (index in promises.indices.reversed()) {
            when (val ret = promises[index].await()) {
                is Ok -> {
                    if (result == null) {
                        result = ret.value
                    }
                }

                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(result, errors)
    }
}

suspend inline fun <T> Iterable<T>.findParallelly(
    crossinline predicate: SuspendPredicate<T>
): T? {
    return firstOrNullParallelly(predicate)
}

suspend inline fun <T> Iterable<T>.tryFindParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    return tryFirstOrNullParallelly(predicate)
}

suspend inline fun <T> Iterable<T>.exTryFindParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<T?> {
    return exTryFirstOrNullParallelly(predicate)
}

suspend inline fun <T> Iterable<T>.findLastParallelly(
    crossinline predicate: SuspendPredicate<T>
): T? {
    return lastOrNullParallelly(predicate)
}

suspend inline fun <T> Iterable<T>.tryFindLastParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    return tryLastOrNullParallelly(predicate)
}

suspend inline fun <T> Iterable<T>.exTryFindLastParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<T?> {
    return exTryLastOrNullParallelly(predicate)
}

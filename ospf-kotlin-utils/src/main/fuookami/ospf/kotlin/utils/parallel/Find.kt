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
 * Parallel find operations (first, firstOrNull, last, lastOrNull) with concurrency control.
 *
 * 并发控制已实现：使用 Semaphore 限制同时活跃的协程数量。
 * Concurrency control implemented: Uses Semaphore to limit active coroutines.
 */

suspend inline fun <T> Iterable<T>.firstParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): T {
    return firstOrNullParallelly(concurrentAmount, predicate)
        ?: throw NoSuchElementException("Collection contains no element matching the predicate.")
}

suspend inline fun <T> Iterable<T>.tryFirstParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T> {
    return when (val result = tryFirstOrNullParallelly(concurrentAmount, predicate)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))

        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

suspend inline fun <T> Iterable<T>.exTryFirstParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<T> {
    return when (val result = exTryFirstOrNullParallelly(concurrentAmount, predicate)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))

        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> result.value?.let { Warn(it, result.warnings) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))
    }
}

suspend inline fun <T> Iterable<T>.firstOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): T? {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Boolean>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    predicate(element)
                } finally {
                    semaphore.release()
                }
            })
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
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Boolean>>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    predicate(element)
                } finally {
                    semaphore.release()
                }
            })
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
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<T?> {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Boolean>>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    predicate(element)
                } finally {
                    semaphore.release()
                }
            })
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
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendExtractor<R?, T>
): R {
    return firstNotNullOfOrNullParallelly(concurrentAmount, extractor)
        ?: throw NoSuchElementException("No element of the collection was transformed to a non-null value.")
}

suspend inline fun <R, T> Iterable<T>.tryFirstNotNullOfParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<R> {
    return when (val result = tryFirstNotNullOfOrNullParallelly(concurrentAmount, extractor)) {
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
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): ExRet<R> {
    return when (val result = exTryFirstNotNullOfOrNullParallelly(concurrentAmount, extractor)) {
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
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendExtractor<R?, T>
): R? {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    extractor(element)
                } finally {
                    semaphore.release()
                }
            })
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
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<R?> {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R?>>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    extractor(element)
                } finally {
                    semaphore.release()
                }
            })
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
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): ExRet<R?> {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R?>>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    extractor(element)
                } finally {
                    semaphore.release()
                }
            })
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
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): T {
    return lastOrNullParallelly(concurrentAmount, predicate)
        ?: throw NoSuchElementException("Collection contains no element matching the predicate.")
}

suspend inline fun <T> Iterable<T>.tryLastParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T> {
    return when (val result = tryLastOrNullParallelly(concurrentAmount, predicate)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))

        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

suspend inline fun <T> Iterable<T>.exTryLastParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<T> {
    return when (val result = exTryLastOrNullParallelly(concurrentAmount, predicate)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))

        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> result.value?.let { Warn(it, result.warnings) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))
    }
}

suspend inline fun <T> Iterable<T>.lastOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): T? {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Boolean>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    predicate(element)
                } finally {
                    semaphore.release()
                }
            })
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
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Boolean>>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    predicate(element)
                } finally {
                    semaphore.release()
                }
            })
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
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<T?> {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Boolean>>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    predicate(element)
                } finally {
                    semaphore.release()
                }
            })
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
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendExtractor<R?, T>
): R {
    return lastNotNullOfOrNullParallelly(concurrentAmount, extractor)
        ?: throw NoSuchElementException("No element of the collection was transformed to a non-null value.")
}

suspend inline fun <R, T> Iterable<T>.tryLastNotNullOfParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<R> {
    return when (val result = tryLastNotNullOfOrNullParallelly(concurrentAmount, extractor)) {
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
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): ExRet<R> {
    return when (val result = exTryLastNotNullOfOrNullParallelly(concurrentAmount, extractor)) {
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
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendExtractor<R?, T>
): R? {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    extractor(element)
                } finally {
                    semaphore.release()
                }
            })
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
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<R?> {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R?>>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    extractor(element)
                } finally {
                    semaphore.release()
                }
            })
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
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): ExRet<R?> {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R?>>>()
        for (element in elements) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    extractor(element)
                } finally {
                    semaphore.release()
                }
            })
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
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): T? {
    return firstOrNullParallelly(concurrentAmount, predicate)
}

suspend inline fun <T> Iterable<T>.tryFindParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    return tryFirstOrNullParallelly(concurrentAmount, predicate)
}

suspend inline fun <T> Iterable<T>.exTryFindParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<T?> {
    return exTryFirstOrNullParallelly(concurrentAmount, predicate)
}

suspend inline fun <T> Iterable<T>.findLastParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): T? {
    return lastOrNullParallelly(concurrentAmount, predicate)
}

suspend inline fun <T> Iterable<T>.tryFindLastParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    return tryLastOrNullParallelly(concurrentAmount, predicate)
}

suspend inline fun <T> Iterable<T>.exTryFindLastParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<T?> {
    return exTryLastOrNullParallelly(concurrentAmount, predicate)
}
package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.error.*

// firstParallelly: Find first element matching the predicate in parallel / 并行查找第一个匹配谓词的元素
suspend inline fun <T> Iterable<T>.firstParallelly(
    crossinline predicate: SuspendPredicate<T>
): T {
    return this.firstOrNullParallelly(predicate)
        ?: throw NoSuchElementException("Collection contains no element matching the predicate.")
}

// tryFirstParallelly: Try version of firstParallelly / firstParallelly 的 try 版本
suspend inline fun <T> Iterable<T>.tryFirstParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T> {
    return when (val result = this.tryFirstOrNullParallelly(predicate)) {
        is Ok -> {
            result.value
                ?.let { Ok(it) }
                ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))
        }

        is Failed -> {
            Failed(result.error)
        }
    }
}

// firstOrNullParallelly: Find first element matching the predicate in parallel, or null / 并行查找第一个匹配谓词的元素，或 null
suspend inline fun <T> Iterable<T>.firstOrNullParallelly(
    crossinline predicate: SuspendPredicate<T>
): T? {
    var result: T? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T?>>()
            for (element in this@firstOrNullParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    if (predicate(element)) {
                        element
                    } else {
                        null
                    }
                })
            }
            for (promise in promises) {
                result = promise.await()
                if (result != null) {
                    cancel()
                }
            }
            null
        }
    } catch (e: CancellationException) {
        result
    }
}

// tryFirstOrNullParallelly: Try version of firstOrNullParallelly / firstOrNullParallelly 的 try 版本
suspend inline fun <T> Iterable<T>.tryFirstOrNullParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    var result: T? = null
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<T?>>>()
            for (element in this@tryFirstOrNullParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    when (val ret = predicate(element)) {
                        is Ok -> {
                            if (ret.value) {
                                Ok(element)
                            } else {
                                Ok(null)
                            }
                        }

                        is Failed -> {
                            Failed(ret.error)
                        }
                    }
                })
                for (promise in promises) {
                    when (val ret = promise.await()) {
                        is Ok -> {
                            result = ret.value
                            if (result != null) {
                                cancel()
                            }
                        }

                        is Failed -> {
                            error = ret.error
                            cancel()
                        }
                    }
                }
            }
            Ok(null)
        }
    } catch (e: CancellationException) {
        result?.let { Ok(it) }
            ?: error?.let { Failed(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))
    }
}

// firstNotNullOfParallelly: Find first non-null extracted value in parallel / 并行查找第一个非空提取值
suspend inline fun <R, T> Iterable<T>.firstNotNullOfParallelly(
    crossinline extractor: SuspendExtractor<R?, T>
): R {
    return this.firstNotNullOfOrNullParallelly(extractor)
        ?: throw NoSuchElementException("No element of the collection was transformed to a non-null value.")
}

// tryFirstNotNullOfParallelly: Try version of firstNotNullOfParallelly / firstNotNullOfParallelly 的 try 版本
suspend inline fun <R, T> Iterable<T>.tryFirstNotNullOfParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<R> {
    return when (val result = this.tryFirstNotNullOfOrNullParallelly(extractor)) {
        is Ok -> {
            result.value
                ?.let { Ok(it) }
                ?: Failed(
                    Err(
                        ErrorCode.ApplicationException,
                        "No element of the collection was transformed to a non-null value."
                    )
                )
        }

        is Failed -> {
            Failed(result.error)
        }
    }
}

// firstNotNullOfOrNullParallelly: Find first non-null extracted value in parallel, or null / 并行查找第一个非空提取值，或 null
suspend inline fun <R, T> Iterable<T>.firstNotNullOfOrNullParallelly(
    crossinline extractor: SuspendExtractor<R?, T>
): R? {
    var result: R? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            for (element in this@firstNotNullOfOrNullParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    extractor(element)
                })
            }
            for (promise in promises) {
                result = promise.await()
                if (result != null) {
                    cancel()
                }
            }
            null
        }
    } catch (e: CancellationException) {
        result
    }
}

// tryFirstNotNullOfOrNullParallelly: Try version of firstNotNullOfOrNullParallelly / firstNotNullOfOrNullParallelly 的 try 版本
suspend inline fun <R, T> Iterable<T>.tryFirstNotNullOfOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<R?> {
    var result: R? = null
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<R?>>>()
            for (element in this@tryFirstNotNullOfOrNullParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    extractor(element)
                })
            }
            for (promise in promises) {
                when (val ret = promise.await()) {
                    is Ok -> {
                        result = ret.value
                        if (result != null) {
                            cancel()
                        }
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                    }
                }
            }
            Ok(null)
        }
    } catch (e: CancellationException) {
        result?.let { Ok(it) }
            ?: error?.let { Failed(it) }
            ?: Failed(
                Err(
                    ErrorCode.ApplicationException,
                    "No element of the collection was transformed to a non-null value."
                )
            )
    }
}

// lastParallelly: Find last element matching the predicate in parallel / 并行查找最后一个匹配谓词的元素
suspend inline fun <T> Iterable<T>.lastParallelly(
    crossinline predicate: SuspendPredicate<T>
): T {
    return this.lastOrNullParallelly(predicate)
        ?: throw NoSuchElementException("Collection contains no element matching the predicate.")
}

// tryLastParallelly: Try version of lastParallelly / lastParallelly 的 try 版本
suspend inline fun <T> Iterable<T>.tryLastParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T> {
    return when (val result = this.tryLastOrNullParallelly(predicate)) {
        is Ok -> {
            result.value
                ?.let { Ok(it) }
                ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))
        }

        is Failed -> {
            Failed(result.error)
        }
    }
}

// lastOrNullParallelly: Find last element matching the predicate in parallel, or null / 并行查找最后一个匹配谓词的元素，或 null
suspend inline fun <T> Iterable<T>.lastOrNullParallelly(
    crossinline predicate: SuspendPredicate<T>
): T? {
    var result: T? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T?>>()
            for (element in this@lastOrNullParallelly.reversed().iterator()) {
                promises.add(async(Dispatchers.Default) {
                    if (predicate(element)) {
                        element
                    } else {
                        null
                    }
                })
            }
            for (promise in promises) {
                result = promise.await()
                if (result != null) {
                    cancel()
                }
            }
            null
        }
    } catch (e: CancellationException) {
        result
    }
}

// tryLastOrNullParallelly: Try version of lastOrNullParallelly / lastOrNullParallelly 的 try 版本
suspend inline fun <T> Iterable<T>.tryLastOrNullParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    var result: T? = null
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<T?>>>()
            for (element in this@tryLastOrNullParallelly.reversed().iterator()) {
                promises.add(async(Dispatchers.Default) {
                    when (val ret = predicate(element)) {
                        is Ok -> {
                            if (ret.value) {
                                Ok(element)
                            } else {
                                Ok(null)
                            }
                        }

                        is Failed -> {
                            Failed(ret.error)
                        }
                    }
                })
            }
            for (promise in promises) {
                when (val ret = promise.await()) {
                    is Ok -> {
                        result = ret.value
                        if (result != null) {
                            cancel()
                        }
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                    }
                }
            }
            Ok(null)
        }
    } catch (e: CancellationException) {
        result?.let { Ok(it) }
            ?: error?.let { Failed(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))
    }
}

// lastNotNullOfParallelly: Find last non-null extracted value in parallel / 并行查找最后一个非空提取值
suspend inline fun <R, T> Iterable<T>.lastNotNullOfParallelly(
    crossinline extractor: SuspendExtractor<R?, T>
): R {
    return this.lastNotNullOfOrNullParallelly(extractor)
        ?: throw NoSuchElementException("No element of the collection was transformed to a non-null value.")
}

// tryLastNotNullOfParallelly: Try version of lastNotNullOfParallelly / lastNotNullOfParallelly 的 try 版本
suspend inline fun <R, T> Iterable<T>.tryLastNotNullOfParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<R> {
    return when (val result = this.tryLastNotNullOfOrNullParallelly(extractor)) {
        is Ok -> {
            result.value
                ?.let { Ok(it) }
                ?: Failed(
                    Err(
                        ErrorCode.ApplicationException,
                        "No element of the collection was transformed to a non-null value."
                    )
                )
        }

        is Failed -> {
            Failed(result.error)
        }
    }
}

// lastNotNullOfOrNullParallelly: Find last non-null extracted value in parallel, or null / 并行查找最后一个非空提取值，或 null
suspend inline fun <R, T> Iterable<T>.lastNotNullOfOrNullParallelly(
    crossinline extractor: SuspendExtractor<R?, T>
): R? {
    var result: R? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            for (element in this@lastNotNullOfOrNullParallelly.reversed().iterator()) {
                promises.add(async(Dispatchers.Default) {
                    extractor(element)
                })
            }
            for (promise in promises) {
                result = promise.await()
                if (result != null) {
                    cancel()
                }
            }
            null
        }
    } catch (e: CancellationException) {
        result
    }
}

// tryLastNotNullOfOrNullParallelly: Try version of lastNotNullOfOrNullParallelly / lastNotNullOfOrNullParallelly 的 try 版本
suspend inline fun <R, T> Iterable<T>.tryLastNotNullOfOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<R?> {
    var result: R? = null
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<R?>>>()
            for (element in this@tryLastNotNullOfOrNullParallelly.reversed().iterator()) {
                promises.add(async(Dispatchers.Default) {
                    extractor(element)
                })
            }
            for (promise in promises) {
                when (val ret = promise.await()) {
                    is Ok -> {
                        result = ret.value
                        if (result != null) {
                            cancel()
                        }
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                    }
                }
            }
            Ok(null)
        }
    } catch (e: CancellationException) {
        result?.let { Ok(it) }
            ?: error?.let { Failed(it) }
            ?: Failed(
                Err(
                    ErrorCode.ApplicationException,
                    "No element of the collection was transformed to a non-null value."
                )
            )
    }
}

// findParallelly: Alias for firstOrNullParallelly / firstOrNullParallelly 的别名
suspend inline fun <T> Iterable<T>.findParallelly(
    crossinline predicate: SuspendPredicate<T>
): T? {
    return this.firstOrNullParallelly(predicate)
}

// tryFindParallelly: Try version of findParallelly / findParallelly 的 try 版本
suspend inline fun <T> Iterable<T>.tryFindParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    return this.tryFirstOrNullParallelly(predicate)
}

// findLastParallelly: Alias for lastOrNullParallelly / lastOrNullParallelly 的别名
suspend inline fun <T> Iterable<T>.findLastParallelly(
    crossinline predicate: SuspendPredicate<T>
): T? {
    return this.lastOrNullParallelly(predicate)
}

// tryFindLastParallelly: Try version of findLastParallelly / findLastParallelly 的 try 版本
suspend inline fun <T> Iterable<T>.tryFindLastParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    return this.tryLastOrNullParallelly(predicate)
}
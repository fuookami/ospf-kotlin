package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <R, T> Iterable<T>.lastNotNullOfParallelly(
    crossinline extractor: Extractor<R?, T>
): R {
    return this.lastNotNullOfParallelly(UInt64(Runtime.getRuntime().availableProcessors()), extractor)
}

suspend inline fun <R, T> Iterable<T>.lastNotNullOfParallelly(
    segment: UInt64,
    crossinline extractor: Extractor<R?, T>
): R {
    var result: R? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            val iterator = this@lastNotNullOfParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.lastNotNullOfOrNull(extractor)
                })
            }

            for (promise in promises.reversed()) {
                result = promise.await()
                if (result != null) {
                    cancel()
                    return@coroutineScope result
                }
            }

            null
        } ?: throw NoSuchElementException("Collection contains no element matching the predicate.")
    } catch (e: CancellationException) {
        result ?: throw NoSuchElementException("Collection contains no element matching the predicate.")
    }
}

suspend inline fun <R, T> Iterable<T>.tryLastNotNullOfParallelly(
    crossinline extractor: TryExtractor<R?, T>
): Ret<R> {
    return this.tryLastNotNullOfParallelly(UInt64(Runtime.getRuntime().availableProcessors()), extractor)
}

suspend inline fun <R, T> Iterable<T>.tryLastNotNullOfParallelly(
    segment: UInt64,
    crossinline extractor: TryExtractor<R?, T>
): Ret<R> {
    var result: R? = null
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            val iterator = this@tryLastNotNullOfParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.lastNotNullOfOrNull {
                        when (val ret = extractor(it)) {
                            is Ok -> {
                                ret.value
                            }

                            is Failed -> {
                                error = ret.error
                                cancel()
                                null
                            }
                        }
                    }
                })
            }

            for (promise in promises.reversed()) {
                result = promise.await()
                if (result != null) {
                    cancel()
                    return@coroutineScope result
                }
            }

            null
        }?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))
    } catch (e: CancellationException) {
        result?.let { Ok(it) }
            ?: error?.let { Failed(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))
    }
}

suspend inline fun <R, T> Collection<T>.lastNotNullOfParallelly(
    crossinline extractor: Extractor<R?, T>
): R {
    return (this as Iterable<T>).lastNotNullOfParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <R, T> Collection<T>.lastNotNullOfParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<R?, T>
): R {
    return (this as Iterable<T>).lastNotNullOfParallelly(this.usize / concurrentAmount, extractor)
}

suspend inline fun <R, T> Collection<T>.tryLastNotNullOfParallelly(
    crossinline extractor: TryExtractor<R?, T>
): Ret<R> {
    return (this as Iterable<T>).tryLastNotNullOfParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <R, T> Collection<T>.tryLastNotNullOfParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryExtractor<R?, T>
): Ret<R> {
    return (this as Iterable<T>).tryLastNotNullOfParallelly(this.usize / concurrentAmount, extractor)
}

suspend inline fun <R, T> List<T>.lastNotNullOfParallelly(
    crossinline extractor: Extractor<R?, T>
): R {
    return this.lastNotNullOfParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <R, T> List<T>.lastNotNullOfParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<R?, T>
): R {
    var result: R? = null

    return try {
        coroutineScope {
            val iterator = this@lastNotNullOfParallelly.listIterator(this@lastNotNullOfParallelly.size)
            while (iterator.hasPrevious()) {
                val promises = ArrayList<Deferred<R?>>()
                for (j in UInt64.zero until concurrentAmount) {
                    val v = iterator.previous()
                    promises.add(async(Dispatchers.Default) {
                        extractor(v)
                    })

                    if (!iterator.hasPrevious()) {
                        break
                    }
                }
                for (promise in promises) {
                    result = promise.await()
                    if (result != null) {
                        cancel()
                        return@coroutineScope result
                    }
                }
            }

            null
        } ?: throw NoSuchElementException("Collection contains no element matching the predicate.")
    } catch (e: CancellationException) {
        result ?: throw NoSuchElementException("Collection contains no element matching the predicate.")
    }
}

suspend inline fun <R, T> List<T>.tryLastNotNullOfParallelly(
    crossinline extractor: TryExtractor<R?, T>
): Ret<R> {
    return this.tryLastNotNullOfParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <R, T> List<T>.tryLastNotNullOfParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryExtractor<R?, T>
): Ret<R> {
    var result: R? = null
    var error: Error? = null

    return try {
        coroutineScope {
            val iterator = this@tryLastNotNullOfParallelly.listIterator(this@tryLastNotNullOfParallelly.size)
            while (iterator.hasPrevious()) {
                val promises = ArrayList<Deferred<R?>>()
                for (j in UInt64.zero until concurrentAmount) {
                    val v = iterator.previous()
                    promises.add(async(Dispatchers.Default) {
                        when (val ret = extractor(v)) {
                            is Ok -> {
                                ret.value
                            }

                            is Failed -> {
                                error = ret.error
                                cancel()
                                null
                            }
                        }
                    })

                    if (!iterator.hasPrevious()) {
                        break
                    }
                }
                for (promise in promises) {
                    result = promise.await()
                    if (result != null) {
                        cancel()
                        return@coroutineScope result
                    }
                }
            }

            null
        }?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))
    } catch (e: CancellationException) {
        result?.let { Ok(it) }
            ?: error?.let { Failed(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))
    }
}

package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <R, T> Iterable<T>.lastNotNullOfOrNullParallelly(
    crossinline extractor: Extractor<R?, T>
): R? {
    return this.lastNotNullOfOrNullParallelly(UInt64(Runtime.getRuntime().availableProcessors()), extractor)
}

suspend inline fun <R, T> Iterable<T>.lastNotNullOfOrNullParallelly(
    segment: UInt64,
    crossinline extractor: Extractor<R?, T>
): R? {
    var result: R? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            val iterator = this@lastNotNullOfOrNullParallelly.iterator()
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
        }
    } catch (e: CancellationException) {
        result
    }
}

@JvmName("tryLastNotNullOfOrNullParallelly")
suspend inline fun <R, T> Iterable<T>.lastNotNullOfOrNullParallelly(
    crossinline extractor: TryExtractor<R?, T>
): Ret<R?> {
    return this.lastNotNullOfOrNullParallelly(UInt64(Runtime.getRuntime().availableProcessors()), extractor)
}

@JvmName("tryLastNotNullOfOrNullParallelly")
suspend inline fun <R, T> Iterable<T>.lastNotNullOfOrNullParallelly(
    segment: UInt64,
    crossinline extractor: TryExtractor<R?, T>
): Ret<R?> {
    var result: R? = null
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            val iterator = this@lastNotNullOfOrNullParallelly.iterator()
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
        }.let { Ok(it) }
    } catch (e: CancellationException) {
        result?.let { Ok(it) }
            ?: error?.let { Failed(it) }
            ?: Ok(null)
    }
}

suspend inline fun <R, T> Collection<T>.lastNotNullOfOrNullParallelly(
    crossinline extractor: Extractor<R?, T>
): R? {
    return (this as Iterable<T>).lastNotNullOfOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <R, T> Collection<T>.lastNotNullOfOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<R?, T>
): R? {
    return (this as Iterable<T>).lastNotNullOfOrNullParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

@JvmName("tryLastNotNullOfOrNullParallelly")
suspend inline fun <R, T> Collection<T>.lastNotNullOfOrNullParallelly(
    crossinline extractor: TryExtractor<R?, T>
): Ret<R?> {
    return (this as Iterable<T>).lastNotNullOfOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

@JvmName("tryLastNotNullOfOrNullParallelly")
suspend inline fun <R, T> Collection<T>.lastNotNullOfOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryExtractor<R?, T>
): Ret<R?> {
    return (this as Iterable<T>).lastNotNullOfOrNullParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <R, T> List<T>.lastNotNullOfOrNullParallelly(
    crossinline extractor: Extractor<R?, T>
): R? {
    return this.lastNotNullOfOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <R, T> List<T>.lastNotNullOfOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<R?, T>
): R? {
    var result: R? = null

    return try {
        coroutineScope {
            val iterator = this@lastNotNullOfOrNullParallelly.listIterator(this@lastNotNullOfOrNullParallelly.size)
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
        }

        null
    } catch (e: CancellationException) {
        result
    }
}

@JvmName("tryLastNotNullOfOrNullParallelly")
suspend inline fun <R, T> List<T>.lastNotNullOfOrNullParallelly(
    crossinline extractor: TryExtractor<R?, T>
): Ret<R?> {
    return this.lastNotNullOfOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

@JvmName("tryLastNotNullOfOrNullParallelly")
suspend inline fun <R, T> List<T>.lastNotNullOfOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryExtractor<R?, T>
): Ret<R?> {
    var result: R? = null
    var error: Error? = null

    return try {
        coroutineScope {
            val iterator = this@lastNotNullOfOrNullParallelly.listIterator(this@lastNotNullOfOrNullParallelly.size)
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
        }.let { Ok(it) }
    } catch (e: CancellationException) {
        result?.let { Ok(it) }
            ?: error?.let { Failed(it) }
            ?: Ok(null)
    }
}

package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.lastParallelly(
    crossinline predicate: Predicate<T>
): T {
    return this.lastParallelly(UInt64(Runtime.getRuntime().availableProcessors()), predicate)
}

suspend inline fun <T> Iterable<T>.lastParallelly(
    segment: UInt64,
    crossinline predicate: Predicate<T>
): T {
    var result: T? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T?>>()
            val iterator = this@lastParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.lastOrNull(predicate)
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

suspend inline fun <T> Iterable<T>.tryLastParallelly(
    crossinline predicate: TryPredicate<T>
): Ret<T> {
    return this.tryLastParallelly(UInt64(Runtime.getRuntime().availableProcessors()), predicate)
}

suspend inline fun <T> Iterable<T>.tryLastParallelly(
    segment: UInt64,
    crossinline predicate: TryPredicate<T>
): Ret<T> {
    var result: T? = null
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T?>>()
            val iterator = this@tryLastParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.lastOrNull {
                        when (val ret = predicate(it)) {
                            is Ok -> {
                                ret.value
                            }

                            is Failed -> {
                                error = ret.error
                                cancel()
                                false
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

suspend inline fun <T> Collection<T>.lastParallelly(
    crossinline predicate: Predicate<T>
): T {
    return (this as Iterable<T>).lastParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> Collection<T>.lastParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: Predicate<T>
): T {
    return (this as Iterable<T>).lastParallelly(UInt64(this.size) / concurrentAmount, predicate)
}

suspend inline fun <T> Collection<T>.tryLastParallelly(
    crossinline predicate: TryPredicate<T>
): Ret<T> {
    return (this as Iterable<T>).tryLastParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> Collection<T>.tryLastParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: TryPredicate<T>
): Ret<T> {
    return (this as Iterable<T>).tryLastParallelly(UInt64(this.size) / concurrentAmount, predicate)
}

suspend inline fun <T> List<T>.lastParallelly(
    crossinline predicate: Predicate<T>
): T {
    return this.lastParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> List<T>.lastParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: Predicate<T>
): T {
    var result: T? = null

    return try {
        coroutineScope {
            val iterator = this@lastParallelly.listIterator(this@lastParallelly.size)
            while (iterator.hasPrevious()) {
                val promises = ArrayList<Deferred<T?>>()
                for (j in UInt64.zero until concurrentAmount) {
                    val v = iterator.previous()
                    promises.add(async(Dispatchers.Default) {
                        if (predicate(v)) {
                            v
                        } else {
                            null
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
        } ?: throw NoSuchElementException("Collection contains no element matching the predicate.")
    } catch (e: CancellationException) {
        result!!
    }
}

suspend inline fun <T> List<T>.tryLastParallelly(
    crossinline predicate: TryPredicate<T>
): Ret<T> {
    return this.tryLastParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> List<T>.tryLastParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: TryPredicate<T>
): Ret<T> {
    var result: T? = null
    var error: Error? = null

    return try {
        coroutineScope {
            val iterator = this@tryLastParallelly.listIterator(this@tryLastParallelly.size)
            while (iterator.hasPrevious()) {
                val promises = ArrayList<Deferred<T?>>()
                for (j in UInt64.zero until concurrentAmount) {
                    val v = iterator.previous()
                    promises.add(async(Dispatchers.Default) {
                        when (val ret = predicate(v)) {
                            is Ok -> {
                                if (ret.value) {
                                    v
                                } else {
                                    null
                                }
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

package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.firstParallelly(
    crossinline predicate: Predicate<T>
): T {
    return this.firstParallelly(UInt64(Runtime.getRuntime().availableProcessors()), predicate)
}

suspend inline fun <T> Iterable<T>.firstParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: Predicate<T>
): T {
    var result: T? = null

    return try {
        coroutineScope {
            val iterator = this@firstParallelly.iterator()
            while (iterator.hasNext()) {
                val promises = ArrayList<Deferred<T?>>()
                for (j in UInt64.zero until concurrentAmount) {
                    val v = iterator.next()
                    promises.add(async(Dispatchers.Default) {
                        if (predicate(v)) {
                            v
                        } else {
                            null
                        }
                    })

                    if (!iterator.hasNext()) {
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

suspend inline fun <T> Iterable<T>.tryFirstParallelly(
    crossinline predicate: TryPredicate<T>
): Ret<T> {
    return this.tryFirstParallelly(UInt64(Runtime.getRuntime().availableProcessors()), predicate)
}

suspend inline fun <T> Iterable<T>.tryFirstParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: TryPredicate<T>
): Ret<T> {
    var result: T? = null
    var error: Error? = null

    return try {
        coroutineScope {
            val iterator = this@tryFirstParallelly.iterator()
            while (iterator.hasNext()) {
                val promises = ArrayList<Deferred<T?>>()
                for (j in UInt64.zero until concurrentAmount) {
                    val v = iterator.next()
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

                    if (!iterator.hasNext()) {
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

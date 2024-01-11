package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.firstOrNullParallelly(
    crossinline predicate: SuspendPredicate<T>
): T? {
    return this.firstOrNullParallelly(UInt64(Runtime.getRuntime().availableProcessors()), predicate)
}

suspend inline fun <T> Iterable<T>.firstOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: SuspendPredicate<T>
): T? {
    var result: T? = null

    return try {
        coroutineScope {
            val iterator = this@firstOrNullParallelly.iterator()
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
        }
    } catch (e: CancellationException) {
        result
    }
}

suspend inline fun <T> Iterable<T>.tryFirstOrNullParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    return this.tryFirstOrNullParallelly(UInt64(Runtime.getRuntime().availableProcessors()), predicate)
}

suspend inline fun <T> Iterable<T>.tryFirstOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    var result: T? = null
    var error: Error? = null

    return try {
        coroutineScope {
            val iterator = this@tryFirstOrNullParallelly.iterator()
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
        }.let { Ok(it) }
    } catch (e: CancellationException) {
        result?.let { Ok(it) }
            ?: error?.let { Failed(it) }
            ?: Ok(null)
    }
}

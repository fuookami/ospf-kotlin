package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.allParallelly(
    crossinline predicate: Predicate<T>
): Boolean {
    return this.allParallelly(UInt64.ten, predicate)
}

suspend inline fun <T> Iterable<T>.allParallelly(
    segment: UInt64,
    crossinline predicate: Predicate<T>
): Boolean {
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            val iterator = this@allParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.all(predicate)
                })
            }
            for (promise in promises) {
                if (!promise.await()) {
                    cancel()
                    return@coroutineScope false
                }
            }

            true
        }
    } catch (e: CancellationException) {
        false
    }
}

@JvmName("tryAllParallelly")
suspend inline fun <T> Iterable<T>.allParallelly(
    crossinline predicate: TryPredicate<T>
): Ret<Boolean> {
    return this.allParallelly(UInt64.ten, predicate)
}

@JvmName("tryAllParallelly")
suspend inline fun <T> Iterable<T>.allParallelly(
    segment: UInt64,
    crossinline predicate: TryPredicate<T>
): Ret<Boolean> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            val iterator = this@allParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.all {
                        when (val result = predicate(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                false
                            }
                        }
                    }
                })
            }
            for (promise in promises) {
                if (!promise.await()) {
                    cancel()
                    return@coroutineScope false
                }
            }

            true
        }.let { Ok(it) }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(false)
    }
}

suspend inline fun <T> Collection<T>.allParallelly(
    crossinline predicate: Predicate<T>
): Boolean {
    return (this as Iterable<T>).allParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> Collection<T>.allParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: Predicate<T>
): Boolean {
    return (this as Iterable<T>).allParallelly(UInt64(this.size) / concurrentAmount, predicate)
}

@JvmName("tryAllParallelly")
suspend inline fun <T> Collection<T>.allParallelly(
    crossinline predicate: TryPredicate<T>
): Ret<Boolean> {
    return (this as Iterable<T>).allParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

@JvmName("tryAllParallelly")
suspend inline fun <T> Collection<T>.allParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: TryPredicate<T>
): Ret<Boolean> {
    return (this as Iterable<T>).allParallelly(UInt64(this.size) / concurrentAmount, predicate)
}

suspend inline fun <T> List<T>.allParallelly(
    crossinline predicate: Predicate<T>
): Boolean {
    return this.allParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> List<T>.allParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: Predicate<T>
): Boolean {
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            val segmentAmount = this@allParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@allParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@allParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@allParallelly.subList(j, k).all(predicate)
                })
                i = k
            }
            for (promise in promises) {
                if (!promise.await()) {
                    cancel()
                    return@coroutineScope false
                }
            }

            true
        }
    } catch (e: CancellationException) {
        false
    }
}

@JvmName("tryAllParallelly")
suspend inline fun <T> List<T>.allParallelly(
    crossinline predicate: TryPredicate<T>
): Ret<Boolean> {
    return this.allParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

@JvmName("tryAllParallelly")
suspend inline fun <T> List<T>.allParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: TryPredicate<T>
): Ret<Boolean> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            val segmentAmount = this@allParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@allParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@allParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@allParallelly.subList(j, k).all {
                        when (val result = predicate(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                false
                            }
                        }
                    }
                })
                i = k
            }
            for (promise in promises) {
                if (!promise.await()) {
                    cancel()
                    return@coroutineScope false
                }
            }

            true
        }.let { Ok(it) }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(false)
    }
}

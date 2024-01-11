package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.allParallelly(
    crossinline predicate: SuspendPredicate<T>
): Boolean {
    return this.allParallelly(UInt64.ten, predicate)
}

suspend inline fun <T> Iterable<T>.allParallelly(
    segment: UInt64,
    crossinline predicate: SuspendPredicate<T>
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
                    thisSegment.all { predicate(it) }
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

suspend inline fun <T> Iterable<T>.tryAllParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Boolean> {
    return this.tryAllParallelly(UInt64.ten, predicate)
}

suspend inline fun <T> Iterable<T>.tryAllParallelly(
    segment: UInt64,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Boolean> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            val iterator = this@tryAllParallelly.iterator()
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
    crossinline predicate: SuspendPredicate<T>
): Boolean {
    return (this as Iterable<T>).allParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> Collection<T>.allParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: SuspendPredicate<T>
): Boolean {
    return (this as Iterable<T>).allParallelly(this.usize / concurrentAmount, predicate)
}

suspend inline fun <T> Collection<T>.tryAllParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Boolean> {
    return (this as Iterable<T>).tryAllParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> Collection<T>.tryAllParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Boolean> {
    return (this as Iterable<T>).tryAllParallelly(this.usize / concurrentAmount, predicate)
}

suspend inline fun <T> List<T>.allParallelly(
    crossinline predicate: SuspendPredicate<T>
): Boolean {
    return this.allParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> List<T>.allParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: SuspendPredicate<T>
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
                    this@allParallelly.subList(j, k).all { predicate(it) }
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

suspend inline fun <T> List<T>.tryAllParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Boolean> {
    return this.tryAllParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> List<T>.tryAllParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Boolean> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            val segmentAmount = this@tryAllParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryAllParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryAllParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryAllParallelly.subList(j, k).all {
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

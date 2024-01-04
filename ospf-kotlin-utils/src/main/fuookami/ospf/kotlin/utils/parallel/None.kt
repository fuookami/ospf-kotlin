package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.noneParallelly(
    crossinline predicate: Predicate<T>
): Boolean {
    return this.noneParallelly(UInt64.ten, predicate)
}

suspend inline fun <T> Iterable<T>.noneParallelly(
    segment: UInt64,
    crossinline predicate: Predicate<T>
): Boolean {
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            val iterator = this@noneParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.none(predicate)
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

suspend inline fun <T> Iterable<T>.tryNoneParallelly(
    crossinline predicate: TryPredicate<T>
): Ret<Boolean> {
    return this.tryNoneParallelly(UInt64.ten, predicate)
}

suspend inline fun <T> Iterable<T>.tryNoneParallelly(
    segment: UInt64,
    crossinline predicate: TryPredicate<T>
): Ret<Boolean> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            val iterator = this@tryNoneParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.none {
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

suspend inline fun <T> Collection<T>.noneParallelly(
    crossinline predicate: Predicate<T>
): Boolean {
    return (this as Iterable<T>).noneParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> Collection<T>.noneParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: Predicate<T>
): Boolean {
    return (this as Iterable<T>).noneParallelly(this.usize / concurrentAmount, predicate)
}

suspend inline fun <T> Collection<T>.tryNoneParallelly(
    crossinline predicate: TryPredicate<T>
): Ret<Boolean> {
    return (this as Iterable<T>).tryNoneParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> Collection<T>.tryNoneParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: TryPredicate<T>
): Ret<Boolean> {
    return (this as Iterable<T>).tryNoneParallelly(this.usize / concurrentAmount, predicate)
}

suspend inline fun <T> List<T>.noneParallelly(
    crossinline predicate: Predicate<T>
): Boolean {
    return this.noneParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> List<T>.noneParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: Predicate<T>
): Boolean {
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            val segmentAmount = this@noneParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@noneParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@noneParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@noneParallelly.subList(j, k).none(predicate)
                })
                i = k
            }
            for (promise in promises) {
                if (!promise.await()) {
                    cancel()
                }
            }
        }

        true
    } catch (e: CancellationException) {
        false
    }
}

suspend inline fun <T> List<T>.tryNoneParallelly(
    crossinline predicate: TryPredicate<T>
): Ret<Boolean> {
    return this.tryNoneParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> List<T>.tryNoneParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: TryPredicate<T>
): Ret<Boolean> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            val segmentAmount = this@tryNoneParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryNoneParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryNoneParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryNoneParallelly.subList(j, k).none {
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

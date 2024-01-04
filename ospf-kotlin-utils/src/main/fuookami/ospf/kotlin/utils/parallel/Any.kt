package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.anyParallelly(
    crossinline predicate: Predicate<T>
): Boolean {
    return this.anyParallelly(UInt64.ten, predicate)
}

suspend inline fun <T> Iterable<T>.anyParallelly(
    segment: UInt64,
    crossinline predicate: Predicate<T>
): Boolean {
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            val iterator = this@anyParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.any(predicate)
                })
            }
            for (promise in promises) {
                if (promise.await()) {
                    cancel()
                    return@coroutineScope true
                }
            }

            false
        }
    } catch (e: CancellationException) {
        true
    }
}

suspend inline fun <T> Iterable<T>.tryAnyParallelly(
    crossinline predicate: TryPredicate<T>
): Ret<Boolean> {
    return this.tryAnyParallelly(UInt64.ten, predicate)
}

suspend inline fun <T> Iterable<T>.tryAnyParallelly(
    segment: UInt64,
    crossinline predicate: TryPredicate<T>
): Ret<Boolean> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            val iterator = this@tryAnyParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.any {
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
                if (promise.await()) {
                    cancel()
                    return@coroutineScope true
                }
            }

            false
        }.let { Ok(it) }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(true)
    }
}

suspend inline fun <T> Collection<T>.anyParallelly(
    crossinline predicate: Predicate<T>
): Boolean {
    return (this as Iterable<T>).anyParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> Collection<T>.anyParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: Predicate<T>
): Boolean {
    return (this as Iterable<T>).anyParallelly(this.usize / concurrentAmount, predicate)
}

suspend inline fun <T> Collection<T>.tryAnyParallelly(
    crossinline predicate: TryPredicate<T>
): Ret<Boolean> {
    return (this as Iterable<T>).tryAnyParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> Collection<T>.tryAnyParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: TryPredicate<T>
): Ret<Boolean> {
    return (this as Iterable<T>).tryAnyParallelly(this.usize / concurrentAmount, predicate)
}

suspend inline fun <T> List<T>.anyParallelly(
    crossinline predicate: Predicate<T>
): Boolean {
    return this.anyParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> List<T>.anyParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: Predicate<T>
): Boolean {
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            val segmentAmount = this@anyParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@anyParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@anyParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@anyParallelly.subList(j, k).any(predicate)
                })
                i = k
            }
            for (promise in promises) {
                if (promise.await()) {
                    cancel()
                    return@coroutineScope true
                }
            }

            false
        }
    } catch (e: CancellationException) {
        true
    }
}

suspend inline fun <T> List<T>.tryAnyParallelly(
    crossinline predicate: TryPredicate<T>
): Ret<Boolean> {
    return this.tryAnyParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> List<T>.tryAnyParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: TryPredicate<T>
): Ret<Boolean> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            val segmentAmount = this@tryAnyParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryAnyParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryAnyParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryAnyParallelly.subList(j, k).any {
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
                if (promise.await()) {
                    cancel()
                    return@coroutineScope true
                }
            }

            false
        }.let { Ok(it) }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(true)
    }
}

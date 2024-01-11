package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.filterParallelly(
    crossinline predicate: SuspendPredicate<T>
): List<T> {
    return this.filterParallelly(UInt64.ten, predicate)
}

suspend inline fun <T> Iterable<T>.filterParallelly(
    segment: UInt64,
    crossinline predicate: SuspendPredicate<T>
): List<T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val iterator = this@filterParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.filter { predicate(it) }
            })
        }

        promises.flatMap { it.await() }
    }
}

suspend inline fun <T> Iterable<T>.tryFilterParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<List<T>> {
    return this.tryFilterParallelly(UInt64.ten, predicate)
}

suspend inline fun <T> Iterable<T>.tryFilterParallelly(
    segment: UInt64,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<List<T>> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<T>>>()
            val iterator = this@tryFilterParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.filter {
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

            Ok(promises.flatMap { it.await() })
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(emptyList())
    }
}

suspend inline fun <T> Collection<T>.filterParallelly(
    crossinline predicate: SuspendPredicate<T>
): List<T> {
    return (this as Iterable<T>).filterParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> Collection<T>.filterParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: SuspendPredicate<T>
): List<T> {
    return (this as Iterable<T>).filterParallelly(this.usize / concurrentAmount, predicate)
}

suspend inline fun <T> Collection<T>.tryFilterParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<List<T>> {
    return (this as Iterable<T>).tryFilterParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> Collection<T>.tryFilterParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<List<T>> {
    return (this as Iterable<T>).tryFilterParallelly(this.usize / concurrentAmount, predicate)
}

suspend inline fun <T> List<T>.filterParallelly(
    crossinline predicate: SuspendPredicate<T>
): List<T> {
    return this.filterParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> List<T>.filterParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: SuspendPredicate<T>
): List<T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val segmentAmount = this@filterParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@filterParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@filterParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@filterParallelly.subList(j, k).filter { predicate(it) }
            })
            i = k
        }

        promises.flatMap { it.await() }
    }
}

suspend inline fun <T> List<T>.tryFilterParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<List<T>> {
    return this.tryFilterParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> List<T>.tryFilterParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<List<T>> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<T>>>()
            val segmentAmount = this@tryFilterParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryFilterParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryFilterParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryFilterParallelly.subList(j, k).filter {
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

            Ok(promises.flatMap { it.await() })
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(emptyList())
    }
}

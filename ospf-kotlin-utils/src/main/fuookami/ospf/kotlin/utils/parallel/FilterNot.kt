package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.filterNotParallelly(
    crossinline predicate: Predicate<T>
): List<T> {
    return this.filterNotParallelly(UInt64.ten, predicate)
}

suspend inline fun <T> Iterable<T>.filterNotParallelly(
    segment: UInt64,
    crossinline predicate: Predicate<T>
): List<T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val iterator = this@filterNotParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.filterNot(predicate)
            })
        }

        promises.flatMap { it.await() }
    }
}

suspend inline fun <T> Iterable<T>.tryFilterNotParallelly(
    crossinline predicate: TryPredicate<T>
): Ret<List<T>> {
    return this.tryFilterNotParallelly(UInt64.ten, predicate)
}

suspend inline fun <T> Iterable<T>.tryFilterNotParallelly(
    segment: UInt64,
    crossinline predicate: TryPredicate<T>
): Ret<List<T>> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<T>>>()
            val iterator = this@tryFilterNotParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.filterNot {
                        when (val result = predicate(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                true
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

suspend inline fun <T> Collection<T>.filterNotParallelly(
    crossinline predicate: Predicate<T>
): List<T> {
    return this.filterNotParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> Collection<T>.filterNotParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: Predicate<T>
): List<T> {
    return (this as Iterable<T>).filterNotParallelly(this.usize / concurrentAmount, predicate)
}

suspend inline fun <T> Collection<T>.tryFilterNotParallelly(
    crossinline predicate: TryPredicate<T>
): Ret<List<T>> {
    return this.tryFilterNotParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> Collection<T>.tryFilterNotParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: TryPredicate<T>
): Ret<List<T>> {
    return (this as Iterable<T>).tryFilterNotParallelly(this.usize / concurrentAmount, predicate)
}

suspend inline fun <T> List<T>.filterNotParallelly(
    crossinline predicate: Predicate<T>
): List<T> {
    return this.filterNotParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> List<T>.filterNotParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: Predicate<T>
): List<T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val segmentAmount = this@filterNotParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@filterNotParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@filterNotParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@filterNotParallelly.subList(j, k).filterNot(predicate)
            })
            i = k
        }

        promises.flatMap { it.await() }
    }
}

suspend inline fun <T> List<T>.tryFilterNotParallelly(
    crossinline predicate: TryPredicate<T>
): Ret<List<T>> {
    return this.tryFilterNotParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> List<T>.tryFilterNotParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: TryPredicate<T>
): Ret<List<T>> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<T>>>()
            val segmentAmount = this@tryFilterNotParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryFilterNotParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryFilterNotParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryFilterNotParallelly.subList(j, k).filterNot {
                        when (val result = predicate(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                true
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

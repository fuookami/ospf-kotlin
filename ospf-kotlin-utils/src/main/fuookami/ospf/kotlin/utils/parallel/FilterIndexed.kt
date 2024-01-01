package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.filterIndexedParallelly(
    crossinline predicate: IndexedPredicate<T>
): List<T> {
    return this.filterIndexedParallelly(UInt64.ten, predicate)
}

suspend inline fun <T> Iterable<T>.filterIndexedParallelly(
    segment: UInt64,
    crossinline predicate: IndexedPredicate<T>
): List<T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val iterator = this@filterIndexedParallelly.iterator()
        var i = 0
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<Pair<Int, T>>()
            var j = UInt64.zero
            while (iterator.hasNext() && j != segment) {
                thisSegment.add(Pair(i, iterator.next()))
                ++i
                ++j
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.filter { predicate(it.first, it.second) }.map { it.second }
            })
        }

        promises.flatMap { it.await() }
    }
}

suspend inline fun <T> Iterable<T>.tryFilterIndexedParallelly(
    crossinline predicate: TryIndexedPredicate<T>
): Ret<List<T>> {
    return this.tryFilterIndexedParallelly(UInt64.ten, predicate)
}

suspend inline fun <T> Iterable<T>.tryFilterIndexedParallelly(
    segment: UInt64,
    crossinline predicate: TryIndexedPredicate<T>
): Ret<List<T>> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<T>>>()
            val iterator = this@tryFilterIndexedParallelly.iterator()
            var i = 0
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<Pair<Int, T>>()
                var j = UInt64.zero
                while (iterator.hasNext() && j != segment) {
                    thisSegment.add(Pair(i, iterator.next()))
                    ++i
                    ++j
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.filter {
                        when (val result = predicate(it.first, it.second)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                false
                            }
                        }
                    }.map { it.second }
                })
            }

            Ok(promises.flatMap { it.await() })
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(emptyList())
    }
}

suspend inline fun <T> Collection<T>.filterIndexedParallelly(
    crossinline predicate: IndexedPredicate<T>
): List<T> {
    return (this as Iterable<T>).filterIndexedParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> Collection<T>.filterIndexedParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: IndexedPredicate<T>
): List<T> {
    return (this as Iterable<T>).filterIndexedParallelly(UInt64(this.size) / concurrentAmount, predicate)
}

suspend inline fun <T> Collection<T>.tryFilterIndexedParallelly(
    crossinline predicate: TryIndexedPredicate<T>
): Ret<List<T>> {
    return (this as Iterable<T>).tryFilterIndexedParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> Collection<T>.tryFilterIndexedParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: TryIndexedPredicate<T>
): Ret<List<T>> {
    return (this as Iterable<T>).tryFilterIndexedParallelly(UInt64(this.size) / concurrentAmount, predicate)
}

suspend inline fun <T> List<T>.filterIndexedParallelly(
    crossinline predicate: IndexedPredicate<T>
): List<T> {
    return this.filterIndexedParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> List<T>.filterIndexedParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: IndexedPredicate<T>
): List<T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val segmentAmount = this@filterIndexedParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@filterIndexedParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@filterIndexedParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@filterIndexedParallelly.subList(j, k).filterIndexed { i, v -> predicate(i + j, v) }
            })
            i = k
        }

        promises.flatMap { it.await() }
    }
}

suspend inline fun <T> List<T>.tryFilterIndexedParallelly(
    crossinline predicate: TryIndexedPredicate<T>
): Ret<List<T>> {
    return this.tryFilterIndexedParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> List<T>.tryFilterIndexedParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: TryIndexedPredicate<T>
): Ret<List<T>> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<T>>>()
            val segmentAmount = this@tryFilterIndexedParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryFilterIndexedParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryFilterIndexedParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryFilterIndexedParallelly.subList(j, k).filterIndexed { i, v ->
                        when (val result = predicate(i + j, v)) {
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

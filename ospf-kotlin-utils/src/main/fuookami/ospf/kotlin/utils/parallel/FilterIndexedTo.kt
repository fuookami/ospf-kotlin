package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import org.ktorm.schema.typeRef

suspend inline fun <T, C : MutableCollection<in T>> Iterable<T>.filterIndexedToParallelly(
    destination: C,
    crossinline predicate: IndexedPredicate<T>
): C {
    return this.filterIndexedToParallelly(UInt64.ten, destination, predicate)
}

suspend inline fun <T, C : MutableCollection<in T>> Iterable<T>.filterIndexedToParallelly(
    segment: UInt64,
    destination: C,
    crossinline predicate: IndexedPredicate<T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val iterator = this@filterIndexedToParallelly.iterator()
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

        promises.flatMapTo(destination) { it.await() }
    }
}

suspend inline fun <T, C : MutableCollection<in T>> Iterable<T>.tryFilterIndexedToParallelly(
    destination: C,
    crossinline predicate: TryIndexedPredicate<T>
): Ret<C> {
    return this.tryFilterIndexedToParallelly(UInt64.ten, destination, predicate)
}

suspend inline fun <T, C : MutableCollection<in T>> Iterable<T>.tryFilterIndexedToParallelly(
    segment: UInt64,
    destination: C,
    crossinline predicate: TryIndexedPredicate<T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<T>>>()
            val iterator = this@tryFilterIndexedToParallelly.iterator()
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

            Ok(promises.flatMapTo(destination) { it.await() })
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(destination)
    }
}

suspend inline fun <T, C : MutableCollection<in T>> Collection<T>.filterIndexedToParallelly(
    destination: C,
    crossinline predicate: IndexedPredicate<T>
): C {
    return (this as Iterable<T>).filterIndexedToParallelly(
        defaultConcurrentAmount,
        destination,
        predicate
    )
}

suspend inline fun <T, C : MutableCollection<in T>> Collection<T>.filterIndexedToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline predicate: IndexedPredicate<T>
): C {
    return (this as Iterable<T>).filterIndexedToParallelly(this.usize / concurrentAmount, destination, predicate)
}

suspend inline fun <T, C : MutableCollection<in T>> Collection<T>.tryFilterIndexedToParallelly(
    destination: C,
    crossinline predicate: TryIndexedPredicate<T>
): Ret<C> {
    return (this as Iterable<T>).tryFilterIndexedToParallelly(
        defaultConcurrentAmount,
        destination,
        predicate
    )
}

suspend inline fun <T, C : MutableCollection<in T>> Collection<T>.tryFilterIndexedToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline predicate: TryIndexedPredicate<T>
): Ret<C> {
    return (this as Iterable<T>).tryFilterIndexedToParallelly(this.usize / concurrentAmount, destination, predicate)
}

suspend inline fun <T, C : MutableCollection<in T>> List<T>.filterIndexedToParallelly(
    destination: C,
    crossinline predicate: IndexedPredicate<T>
): C {
    return this.filterIndexedToParallelly(
        defaultConcurrentAmount,
        destination,
        predicate
    )
}

suspend inline fun <T, C : MutableCollection<in T>> List<T>.filterIndexedToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline predicate: IndexedPredicate<T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val segmentAmount = this@filterIndexedToParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@filterIndexedToParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@filterIndexedToParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@filterIndexedToParallelly.subList(j, k).filterIndexed { i, v -> predicate(i + j, v) }
            })
            i = k
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

suspend inline fun <T, C : MutableCollection<in T>> List<T>.tryFilterIndexedToParallelly(
    destination: C,
    crossinline predicate: TryIndexedPredicate<T>
): Ret<C> {
    return this.tryFilterIndexedToParallelly(
        defaultConcurrentAmount,
        destination,
        predicate
    )
}

suspend inline fun <T, C : MutableCollection<in T>> List<T>.tryFilterIndexedToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline predicate: TryIndexedPredicate<T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<T>>>()
            val segmentAmount = this@tryFilterIndexedToParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryFilterIndexedToParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryFilterIndexedToParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryFilterIndexedToParallelly.subList(j, k).filterIndexed { i, v ->
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

            Ok(promises.flatMapTo(destination) { it.await() })
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(destination)
    }
}

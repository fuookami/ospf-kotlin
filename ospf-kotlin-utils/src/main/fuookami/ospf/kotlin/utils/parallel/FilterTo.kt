package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, C : MutableCollection<in T>> Iterable<T>.filterToParallelly(
    destination: C,
    crossinline predicate: SuspendPredicate<T>
): C {
    return this.filterToParallelly(UInt64.ten, destination, predicate)
}

suspend inline fun <T, C : MutableCollection<in T>> Iterable<T>.filterToParallelly(
    segment: UInt64,
    destination: C,
    crossinline predicate: SuspendPredicate<T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val iterator = this@filterToParallelly.iterator()
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

        promises.flatMapTo(destination) { it.await() }
    }
}

suspend inline fun <T, C : MutableCollection<in T>> Iterable<T>.tryFilterToParallelly(
    destination: C,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<C> {
    return this.tryFilterToParallelly(UInt64.ten, destination, predicate)
}

suspend inline fun <T, C : MutableCollection<in T>> Iterable<T>.tryFilterToParallelly(
    segment: UInt64,
    destination: C,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<T>>>()
            val iterator = this@tryFilterToParallelly.iterator()
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

            Ok(promises.flatMapTo(destination) { it.await() })
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(destination)
    }
}

suspend inline fun <T, C : MutableCollection<in T>> Collection<T>.filterToParallelly(
    destination: C,
    crossinline predicate: SuspendPredicate<T>
): C {
    return (this as Iterable<T>).filterToParallelly(
        defaultConcurrentAmount,
        destination,
        predicate
    )
}

suspend inline fun <T, C : MutableCollection<in T>> Collection<T>.filterToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline predicate: SuspendPredicate<T>
): C {
    return (this as Iterable<T>).filterToParallelly(this.usize / concurrentAmount, destination, predicate)
}

suspend inline fun <T, C : MutableCollection<in T>> Collection<T>.tryFilterToParallelly(
    destination: C,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<C> {
    return (this as Iterable<T>).tryFilterToParallelly(
        defaultConcurrentAmount,
        destination,
        predicate
    )
}

suspend inline fun <T, C : MutableCollection<in T>> Collection<T>.tryFilterToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<C> {
    return (this as Iterable<T>).tryFilterToParallelly(this.usize / concurrentAmount, destination, predicate)
}

suspend inline fun <T, C : MutableCollection<in T>> List<T>.filterToParallelly(
    destination: C,
    crossinline predicate: SuspendPredicate<T>
): C {
    return this.filterToParallelly(
        defaultConcurrentAmount,
        destination,
        predicate
    )
}

suspend inline fun <T, C : MutableCollection<in T>> List<T>.filterToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline predicate: SuspendPredicate<T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val segmentAmount = this@filterToParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@filterToParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@filterToParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@filterToParallelly.subList(j, k).filter { predicate(it) }
            })
            i = k
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

suspend inline fun <T, C : MutableCollection<in T>> List<T>.tryFilterToParallelly(
    destination: C,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<C> {
    return this.tryFilterToParallelly(
        defaultConcurrentAmount,
        destination,
        predicate
    )
}

suspend inline fun <T, C : MutableCollection<in T>> List<T>.tryFilterToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<T>>>()
            val segmentAmount = this@tryFilterToParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryFilterToParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryFilterToParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryFilterToParallelly.subList(j, k).filter {
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

            Ok(promises.flatMapTo(destination) { it.await() })
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(destination)
    }
}

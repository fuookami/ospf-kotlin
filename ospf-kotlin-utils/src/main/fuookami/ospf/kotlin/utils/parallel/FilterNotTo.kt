package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, C : MutableCollection<in T>> Iterable<T>.filterNotToParallelly(
    destination: C,
    crossinline predicate: Predicate<T>
): C {
    return this.filterNotToParallelly(UInt64.ten, destination, predicate)
}

suspend inline fun <T, C : MutableCollection<in T>> Iterable<T>.filterNotToParallelly(
    segment: UInt64,
    destination: C,
    crossinline predicate: Predicate<T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val iterator = this@filterNotToParallelly.iterator()
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

        promises.flatMapTo(destination) { it.await() }
    }
}

@JvmName("tryFilterNotToParallelly")
suspend inline fun <T, C : MutableCollection<in T>> Iterable<T>.filterNotToParallelly(
    destination: C,
    crossinline predicate: TryPredicate<T>
): Ret<C> {
    return this.filterNotToParallelly(UInt64.ten, destination, predicate)
}

@JvmName("tryFilterNotToParallelly")
suspend inline fun <T, C : MutableCollection<in T>> Iterable<T>.filterNotToParallelly(
    segment: UInt64,
    destination: C,
    crossinline predicate: TryPredicate<T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<T>>>()
            val iterator = this@filterNotToParallelly.iterator()
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

            Ok(promises.flatMapTo(destination) { it.await() })
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(destination)
    }
}

suspend inline fun <T, C : MutableCollection<in T>> Collection<T>.filterNotToParallelly(
    destination: C,
    crossinline predicate: Predicate<T>
): C {
    return (this as Iterable<T>).filterNotToParallelly(
        defaultConcurrentAmount,
        destination,
        predicate
    )
}

suspend inline fun <T, C : MutableCollection<in T>> Collection<T>.filterNotToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline predicate: Predicate<T>
): C {
    return (this as Iterable<T>).filterNotToParallelly(UInt64(this.size) / concurrentAmount, destination, predicate)
}

@JvmName("tryFilterNotToParallelly")
suspend inline fun <T, C : MutableCollection<in T>> Collection<T>.filterNotToParallelly(
    destination: C,
    crossinline predicate: TryPredicate<T>
): Ret<C> {
    return (this as Iterable<T>).filterNotToParallelly(
        defaultConcurrentAmount,
        destination,
        predicate
    )
}

@JvmName("tryFilterNotToParallelly")
suspend inline fun <T, C : MutableCollection<in T>> Collection<T>.filterNotToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline predicate: TryPredicate<T>
): Ret<C> {
    return (this as Iterable<T>).filterNotToParallelly(UInt64(this.size) / concurrentAmount, destination, predicate)
}

suspend inline fun <T, C : MutableCollection<in T>> List<T>.filterNotToParallelly(
    destination: C,
    crossinline predicate: Predicate<T>
): C {
    return this.filterNotToParallelly(
        defaultConcurrentAmount,
        destination,
        predicate
    )
}

suspend inline fun <T, C : MutableCollection<in T>> List<T>.filterNotToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline predicate: Predicate<T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val segmentAmount = this@filterNotToParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@filterNotToParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@filterNotToParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@filterNotToParallelly.subList(j, k).filterNot(predicate)
            })
            i = k
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

@JvmName("tryFilterNotToParallelly")
suspend inline fun <T, C : MutableCollection<in T>> List<T>.filterNotToParallelly(
    destination: C,
    crossinline predicate: TryPredicate<T>
): Ret<C> {
    return this.filterNotToParallelly(
        defaultConcurrentAmount,
        destination,
        predicate
    )
}

@JvmName("tryFilterNotToParallelly")
suspend inline fun <T, C : MutableCollection<in T>> List<T>.filterNotToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline predicate: TryPredicate<T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<T>>>()
            val segmentAmount = this@filterNotToParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@filterNotToParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@filterNotToParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@filterNotToParallelly.subList(j, k).filterNot {
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

            Ok(promises.flatMapTo(destination) { it.await() })
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(destination)
    }
}

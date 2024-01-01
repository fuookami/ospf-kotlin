package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedToParallelly(
    destination: C,
    crossinline extractor: IndexedExtractor<Iterable<R>, T>
): C {
    return this.flatMapIndexedToParallelly(UInt64.ten, destination, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedToParallelly(
    segment: UInt64,
    destination: C,
    crossinline extractor: IndexedExtractor<Iterable<R>, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val iterator = this@flatMapIndexedToParallelly.iterator()
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
                thisSegment.flatMap { extractor(it.first, it.second) }
            })
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapIndexedParallelly(
    destination: C,
    crossinline extractor: TryIndexedExtractor<Iterable<R>, T>
): Ret<C> {
    return this.tryFlatMapIndexedParallelly(UInt64.ten, destination, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapIndexedParallelly(
    segment: UInt64,
    destination: C,
    crossinline extractor: TryIndexedExtractor<Iterable<R>, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<R>>>()
            val iterator = this@tryFlatMapIndexedParallelly.iterator()
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
                    thisSegment.flatMap {
                        when (val result = extractor(it.first, it.second)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                emptyList()
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

suspend inline fun <R, T, C : MutableCollection<in R>> Collection<T>.flatMapIndexedToParallelly(
    destination: C,
    crossinline extractor: IndexedExtractor<Iterable<R>, T>
): C {
    return (this as Iterable<T>).flatMapIndexedToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> Collection<T>.flatMapIndexedToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline extractor: IndexedExtractor<Iterable<R>, T>
): C {
    return (this as Iterable<T>).flatMapIndexedToParallelly(
        UInt64(this.size) / concurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> Collection<T>.tryFlatMapIndexedParallelly(
    destination: C,
    crossinline extractor: TryIndexedExtractor<Iterable<R>, T>
): Ret<C> {
    return (this as Iterable<T>).tryFlatMapIndexedParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> Collection<T>.tryFlatMapIndexedParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline extractor: TryIndexedExtractor<Iterable<R>, T>
): Ret<C> {
    return (this as Iterable<T>).tryFlatMapIndexedParallelly(
        UInt64(this.size) / concurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> List<T>.flatMapIndexedToParallelly(
    destination: C,
    crossinline extractor: IndexedExtractor<Iterable<R>, T>
): C {
    return this.flatMapIndexedToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> List<T>.flatMapIndexedToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline extractor: IndexedExtractor<Iterable<R>, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val segmentAmount = this@flatMapIndexedToParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@flatMapIndexedToParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@flatMapIndexedToParallelly.size - j
            )
            promises.add(async(Dispatchers.Default) {
                this@flatMapIndexedToParallelly.flatMapIndexed { i, v -> extractor(i + j, v) }
            })
            i = k
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> List<T>.tryFlatMapIndexedParallelly(
    destination: C,
    crossinline extractor: TryIndexedExtractor<Iterable<R>, T>
): Ret<C> {
    return this.tryFlatMapIndexedParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> List<T>.tryFlatMapIndexedParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline extractor: TryIndexedExtractor<Iterable<R>, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<R>>>()
            val segmentAmount = this@tryFlatMapIndexedParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryFlatMapIndexedParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryFlatMapIndexedParallelly.size - j
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryFlatMapIndexedParallelly.flatMapIndexed { i, v ->
                        when (val result = extractor(i + j, v)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                emptyList()
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

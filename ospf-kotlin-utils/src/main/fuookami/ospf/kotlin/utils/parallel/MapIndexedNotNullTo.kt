package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.mapIndexedNotNullToParallelly(
    destination: C,
    crossinline extractor: IndexedExtractor<R?, T>
): C {
    return this.mapIndexedNotNullToParallelly(UInt64.ten, destination, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.mapIndexedNotNullToParallelly(
    segment: UInt64,
    destination: C,
    crossinline extractor: IndexedExtractor<R?, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val iterator = this@mapIndexedNotNullToParallelly.iterator()
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
                thisSegment.mapNotNull { extractor(it.first, it.second) }
            })
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryMapIndexedNotNullToParallelly(
    destination: C,
    crossinline extractor: TryIndexedExtractor<R?, T>
): Ret<C> {
    return this.tryMapIndexedNotNullToParallelly(UInt64.ten, destination, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryMapIndexedNotNullToParallelly(
    segment: UInt64,
    destination: C,
    crossinline extractor: TryIndexedExtractor<R?, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<R>>>()
            val iterator = this@tryMapIndexedNotNullToParallelly.iterator()
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
                    thisSegment.mapNotNull {
                        when (val result = extractor(it.first, it.second)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                null
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

suspend inline fun <R, T, C : MutableCollection<in R>> Collection<T>.mapIndexedNotNullToParallelly(
    destination: C,
    crossinline extractor: IndexedExtractor<R?, T>
): C {
    return (this as Iterable<T>).mapIndexedNotNullToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> Collection<T>.mapIndexedNotNullToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline extractor: IndexedExtractor<R?, T>
): C {
    return (this as Iterable<T>).mapIndexedNotNullToParallelly(
        UInt64(this.size) / concurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> Collection<T>.tryMapIndexedNotNullToParallelly(
    destination: C,
    crossinline extractor: TryIndexedExtractor<R?, T>
): Ret<C> {
    return (this as Iterable<T>).tryMapIndexedNotNullToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> Collection<T>.tryMapIndexedNotNullToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline extractor: TryIndexedExtractor<R?, T>
): Ret<C> {
    return (this as Iterable<T>).tryMapIndexedNotNullToParallelly(
        UInt64(this.size) / concurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> List<T>.mapIndexedNotNullToParallelly(
    destination: C,
    crossinline extractor: IndexedExtractor<R?, T>
): C {
    return this.mapIndexedNotNullToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> List<T>.mapIndexedNotNullToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline extractor: IndexedExtractor<R?, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val segmentAmount = this@mapIndexedNotNullToParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@mapIndexedNotNullToParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@mapIndexedNotNullToParallelly.size - j
            )
            promises.add(async(Dispatchers.Default) {
                this@mapIndexedNotNullToParallelly.mapIndexedNotNull { i, v -> extractor(i + j, v) }
            })
            i = k
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> List<T>.tryMapIndexedNotNullToParallelly(
    destination: C,
    crossinline extractor: TryIndexedExtractor<R?, T>
): Ret<C> {
    return this.tryMapIndexedNotNullToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> List<T>.tryMapIndexedNotNullToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline extractor: TryIndexedExtractor<R?, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<R>>>()
            val segmentAmount = this@tryMapIndexedNotNullToParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryMapIndexedNotNullToParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryMapIndexedNotNullToParallelly.size - j
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryMapIndexedNotNullToParallelly.mapIndexedNotNull { i, v ->
                        when (val result = extractor(i + j, v)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                null
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

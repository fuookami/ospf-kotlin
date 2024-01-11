package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.mapIndexedToParallelly(
    destination: C,
    crossinline extractor: SuspendIndexedExtractor<R, T>
): C {
    return this.mapIndexedToParallelly(UInt64.ten, destination, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.mapIndexedToParallelly(
    segment: UInt64,
    destination: C,
    crossinline extractor: SuspendIndexedExtractor<R, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val iterator = this@mapIndexedToParallelly.iterator()
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
                thisSegment.map { extractor(it.first, it.second) }
            })
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryMapIndexedToParallelly(
    destination: C,
    crossinline extractor: SuspendTryIndexedExtractor<R, T>
): Ret<C> {
    return this.tryMapIndexedToParallelly(UInt64.ten, destination, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryMapIndexedToParallelly(
    segment: UInt64,
    destination: C,
    crossinline extractor: SuspendTryIndexedExtractor<R, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<R>>>()
            val iterator = this@tryMapIndexedToParallelly.iterator()
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

suspend inline fun <R, T, C : MutableCollection<in R>> Collection<T>.mapIndexedToParallelly(
    destination: C,
    crossinline extractor: SuspendIndexedExtractor<R, T>
): C {
    return (this as Iterable<T>).mapIndexedToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> Collection<T>.mapIndexedToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline extractor: SuspendIndexedExtractor<R, T>
): C {
    return (this as Iterable<T>).mapIndexedToParallelly(this.usize / concurrentAmount, destination, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Collection<T>.tryMapIndexedToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline extractor: SuspendTryIndexedExtractor<R, T>
): Ret<C> {
    return (this as Iterable<T>).tryMapIndexedToParallelly(this.usize / concurrentAmount, destination, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Collection<T>.tryMapIndexedToParallelly(
    destination: C,
    crossinline extractor: SuspendTryIndexedExtractor<R, T>
): Ret<C> {
    return (this as Iterable<T>).tryMapIndexedToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> List<T>.mapIndexedToParallelly(
    destination: C,
    crossinline extractor: SuspendIndexedExtractor<R, T>
): C {
    return this.mapIndexedToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> List<T>.mapIndexedToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline extractor: SuspendIndexedExtractor<R, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val segmentAmount = this@mapIndexedToParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@mapIndexedToParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@mapIndexedToParallelly.size - j
            )
            promises.add(async(Dispatchers.Default) {
                this@mapIndexedToParallelly.mapIndexed { i, v -> extractor(i + j, v) }
            })
            i = k
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> List<T>.tryMapIndexedToParallelly(
    destination: C,
    crossinline extractor: SuspendTryIndexedExtractor<R, T>
): Ret<C> {
    return this.tryMapIndexedToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> List<T>.tryMapIndexedToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline extractor: SuspendTryIndexedExtractor<R, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<R>>>()
            val segmentAmount = this@tryMapIndexedToParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryMapIndexedToParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryMapIndexedToParallelly.size - j
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryMapIndexedToParallelly.mapIndexedNotNull { i, v ->
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

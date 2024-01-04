package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapToParallelly(
    destination: C,
    crossinline extractor: Extractor<Iterable<R>, T>
): C {
    return this.flatMapToParallelly(UInt64.ten, destination, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapToParallelly(
    segment: UInt64,
    destination: C,
    crossinline extractor: Extractor<Iterable<R>, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val iterator = this@flatMapToParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.flatMap { extractor(it) }
            })
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapToParallelly(
    destination: C,
    crossinline extractor: TryExtractor<Iterable<R>, T>
): Ret<C> {
    return this.tryFlatMapToParallelly(UInt64.ten, destination, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapToParallelly(
    segment: UInt64,
    destination: C,
    crossinline extractor: TryExtractor<Iterable<R>, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<R>>>()
            val iterator = this@tryFlatMapToParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.flatMap {
                        when (val result = extractor(it)) {
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

suspend inline fun <R, T, C : MutableCollection<in R>> Collection<T>.flatMapToParallelly(
    destination: C,
    crossinline extractor: Extractor<Iterable<R>, T>
): C {
    return (this as Iterable<T>).flatMapToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> Collection<T>.flatMapToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline extractor: Extractor<Iterable<R>, T>
): C {
    return (this as Iterable<T>).flatMapToParallelly(this.usize / concurrentAmount, destination, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Collection<T>.tryFlatMapToParallelly(
    destination: C,
    crossinline extractor: TryExtractor<Iterable<R>, T>
): Ret<C> {
    return (this as Iterable<T>).tryFlatMapToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> Collection<T>.tryFlatMapToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline extractor: TryExtractor<Iterable<R>, T>
): Ret<C> {
    return (this as Iterable<T>).tryFlatMapToParallelly(this.usize / concurrentAmount, destination, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> List<T>.flatMapToParallelly(
    destination: C,
    crossinline extractor: Extractor<Iterable<R>, T>
): C {
    return this.flatMapToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> List<T>.flatMapToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline extractor: Extractor<Iterable<R>, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val segmentAmount = this@flatMapToParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@flatMapToParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@flatMapToParallelly.size - j
            )
            promises.add(async(Dispatchers.Default) {
                this@flatMapToParallelly.flatMap { extractor(it) }
            })
            i = k
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> List<T>.tryFlatMapToParallelly(
    destination: C,
    crossinline extractor: TryExtractor<Iterable<R>, T>
): Ret<C> {
    return this.tryFlatMapToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> List<T>.tryFlatMapToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline extractor: TryExtractor<Iterable<R>, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<R>>>()
            val segmentAmount = this@tryFlatMapToParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryFlatMapToParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryFlatMapToParallelly.size - j
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryFlatMapToParallelly.flatMap {
                        when (val result = extractor(it)) {
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

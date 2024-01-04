package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.mapToParallelly(
    destination: C,
    crossinline extractor: Extractor<R, T>
): C {
    return this.mapToParallelly(UInt64.ten, destination, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.mapToParallelly(
    segment: UInt64,
    destination: C,
    crossinline extractor: Extractor<R, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val iterator = this@mapToParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map(extractor)
            })
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryMapNotNullParallelly(
    destination: C,
    crossinline extractor: TryExtractor<R, T>
): Ret<C> {
    return this.tryMapNotNullParallelly(UInt64.ten, destination, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryMapNotNullParallelly(
    segment: UInt64,
    destination: C,
    crossinline extractor: TryExtractor<R, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<R>>>()
            val iterator = this@tryMapNotNullParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
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

suspend inline fun <R, T, C : MutableCollection<in R>> Collection<T>.mapToParallelly(
    destination: C,
    crossinline extractor: Extractor<R, T>
): C {
    return (this as Iterable<T>).mapToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> Collection<T>.mapToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline extractor: Extractor<R, T>
): C {
    return (this as Iterable<T>).mapToParallelly(this.usize / concurrentAmount, destination, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Collection<T>.tryMapNotNullParallelly(
    destination: C,
    crossinline extractor: TryExtractor<R, T>
): Ret<C> {
    return (this as Iterable<T>).tryMapNotNullParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> Collection<T>.tryMapNotNullParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline extractor: TryExtractor<R, T>
): Ret<C> {
    return (this as Iterable<T>).tryMapNotNullParallelly(this.usize / concurrentAmount, destination, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> List<T>.mapToParallelly(
    destination: C,
    crossinline extractor: Extractor<R, T>
): C {
    return this.mapToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> List<T>.mapToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline extractor: Extractor<R, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val segmentAmount = this@mapToParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@mapToParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@mapToParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@mapToParallelly.subList(j, k).map(extractor)
            })
            i = k
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> List<T>.tryMapNotNullParallelly(
    destination: C,
    crossinline extractor: TryExtractor<R, T>
): Ret<C> {
    return this.tryMapNotNullParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <R, T, C : MutableCollection<in R>> List<T>.tryMapNotNullParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline extractor: TryExtractor<R, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<R>>>()
            val segmentAmount = this@tryMapNotNullParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryMapNotNullParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryMapNotNullParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryMapNotNullParallelly.subList(j, k).mapNotNull {
                        when (val result = extractor(it)) {
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

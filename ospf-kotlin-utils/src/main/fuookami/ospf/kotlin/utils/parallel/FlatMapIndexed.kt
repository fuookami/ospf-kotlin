package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <R, T> Iterable<T>.flatMapIndexedParallelly(
    crossinline extractor: IndexedExtractor<Iterable<R>, T>
): List<R> {
    return this.flatMapIndexedParallelly(UInt64.ten, extractor)
}

suspend inline fun <R, T> Iterable<T>.flatMapIndexedParallelly(
    segment: UInt64,
    crossinline extractor: IndexedExtractor<Iterable<R>, T>
): List<R> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val iterator = this@flatMapIndexedParallelly.iterator()
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

        promises.flatMap { it.await() }
    }
}

@JvmName("tryFlatMapIndexedParallelly")
suspend inline fun <R, T> Iterable<T>.tryFlatMapIndexedParallelly(
    crossinline extractor: TryIndexedExtractor<Iterable<R>, T>
): Ret<List<R>> {
    return this.tryFlatMapIndexedParallelly(UInt64.ten, extractor)
}

@JvmName("tryFlatMapIndexedParallelly")
suspend inline fun <R, T> Iterable<T>.tryFlatMapIndexedParallelly(
    segment: UInt64,
    crossinline extractor: TryIndexedExtractor<Iterable<R>, T>
): Ret<List<R>> {
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

            Ok(promises.flatMap { it.await() })
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(emptyList())
    }
}

suspend inline fun <R, T> Collection<T>.flatMapIndexedParallelly(
    crossinline extractor: IndexedExtractor<Iterable<R>, T>
): List<R> {
    return (this as Iterable<T>).flatMapIndexedParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <R, T> Collection<T>.flatMapIndexedParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: IndexedExtractor<Iterable<R>, T>
): List<R> {
    return (this as Iterable<T>).flatMapIndexedParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <R, T> Collection<T>.tryFlatMapIndexedParallelly(
    crossinline extractor: TryIndexedExtractor<Iterable<R>, T>
): Ret<List<R>> {
    return (this as Iterable<T>).tryFlatMapIndexedParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <R, T> Collection<T>.tryFlatMapIndexedParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryIndexedExtractor<Iterable<R>, T>
): Ret<List<R>> {
    return (this as Iterable<T>).tryFlatMapIndexedParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <R, T> List<T>.flatMapIndexedParallelly(crossinline extractor: IndexedExtractor<Iterable<R>, T>): List<R> {
    return this.flatMapIndexedParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <R, T> List<T>.flatMapIndexedParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: IndexedExtractor<Iterable<R>, T>
): List<R> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val segmentAmount = this@flatMapIndexedParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@flatMapIndexedParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@flatMapIndexedParallelly.size - j
            )
            promises.add(async(Dispatchers.Default) {
                this@flatMapIndexedParallelly.flatMapIndexed { i, v -> extractor(i + j, v) }
            })
            i = k
        }

        promises.flatMap { it.await() }
    }
}

suspend inline fun <R, T> List<T>.tryFlatMapIndexedParallelly(
    crossinline extractor: TryIndexedExtractor<Iterable<R>, T>
): Ret<List<R>> {
    return this.tryFlatMapIndexedParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <R, T> List<T>.tryFlatMapIndexedParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryIndexedExtractor<Iterable<R>, T>
): Ret<List<R>> {
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

            Ok(promises.flatMap { it.await() })
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(emptyList())
    }
}

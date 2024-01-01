package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxByOrNullParallelly(
    crossinline extractor: Extractor<R, T>
): T? {
    return this.maxByOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxByOrNullParallelly(
    segment: UInt64,
    crossinline extractor: Extractor<R, T>
): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, R>?>>()
        val iterator = this@maxByOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { Pair(it, extractor(it)) }.maxByOrNull { it.second }
            })
        }

        promises.mapNotNull { it.await() }.maxByOrNull { it.second }?.first
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMaxByOrNullParallelly(
    crossinline extractor: TryExtractor<R, T>
): Ret<T?> {
    return this.tryMaxByOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMaxByOrNullParallelly(
    segment: UInt64,
    crossinline extractor: TryExtractor<R, T>
): Ret<T?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<T, R>?>>()
            val iterator = this@tryMaxByOrNullParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment
                        .map {
                            Pair(it, when (val result = extractor(it)) {
                                is Ok -> {
                                    result.value
                                }

                                is Failed -> {
                                    error = result.error
                                    cancel()
                                    return@async null
                                }
                            })
                        }
                        .maxByOrNull { it.second }
                })
            }

            promises.mapNotNull { it.await() }.maxByOrNull { it.second }?.first
        }?.let { Ok(it) } ?: Ok(null)
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(null)
    }
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.maxByOrNullParallelly(
    crossinline extractor: Extractor<R, T>
): T? {
    return (this as Iterable<T>).maxByOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.maxByOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<R, T>
): T? {
    return (this as Iterable<T>).maxByOrNullParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.tryMaxByOrNullParallelly(
    crossinline extractor: TryExtractor<R, T>
): Ret<T?> {
    return (this as Iterable<T>).tryMaxByOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.tryMaxByOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryExtractor<R, T>
): Ret<T?> {
    return (this as Iterable<T>).tryMaxByOrNullParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <T, R : Comparable<R>> List<T>.maxByOrNullParallelly(
    crossinline extractor: Extractor<R, T>
): T? {
    return this.maxByOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> List<T>.maxByOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<R, T>
): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, R>?>>()
        val segmentAmount = this@maxByOrNullParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@maxByOrNullParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@maxByOrNullParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@maxByOrNullParallelly
                    .subList(j, k)
                    .map { Pair(it, extractor(it)) }
                    .maxByOrNull { it.second }
            })
            i = k
        }

        promises.mapNotNull { it.await() }.maxByOrNull { it.second }?.first
    }
}

suspend inline fun <T, R : Comparable<R>> List<T>.tryMaxByOrNullParallelly(
    crossinline extractor: TryExtractor<R, T>
): Ret<T?> {
    return this.tryMaxByOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> List<T>.tryMaxByOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryExtractor<R, T>
): Ret<T?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<T, R>?>>()
            val segmentAmount = this@tryMaxByOrNullParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryMaxByOrNullParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryMaxByOrNullParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryMaxByOrNullParallelly.subList(j, k)
                        .map {
                            Pair(it, when (val result = extractor(it)) {
                                is Ok -> {
                                    result.value
                                }

                                is Failed -> {
                                    error = result.error
                                    cancel()
                                    return@async null
                                }
                            })
                        }
                        .maxByOrNull { it.second }
                })
                i = k
            }

            promises.mapNotNull { it.await() }.maxByOrNull { it.second }?.first
        }?.let { Ok(it) }
            ?: Ok(null)
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(null)
    }
}

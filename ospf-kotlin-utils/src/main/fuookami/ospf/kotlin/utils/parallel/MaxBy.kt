package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxByParallelly(
    crossinline extractor: Extractor<R, T>
): T {
    return this.maxByParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxByParallelly(
    segment: UInt64,
    crossinline extractor: Extractor<R, T>
): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, R>?>>()
        val iterator = this@maxByParallelly.iterator()
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
    } ?: throw NoSuchElementException()
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMaxByParallelly(
    crossinline extractor: TryExtractor<R, T>
): Ret<T> {
    return this.tryMaxByParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMaxByParallelly(
    segment: UInt64,
    crossinline extractor: TryExtractor<R, T>
): Ret<T> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<T, R>?>>()
            val iterator = this@tryMaxByParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.map {
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
                    }.maxByOrNull { it.second }
                })
            }

            promises.mapNotNull { it.await() }.maxByOrNull { it.second }?.first
        }?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    }
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.maxByParallelly(
    crossinline extractor: Extractor<R, T>
): T {
    return (this as Iterable<T>).maxByParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.maxByParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<R, T>
): T {
    return (this as Iterable<T>).maxByParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.tryMaxByParallelly(
    crossinline extractor: TryExtractor<R, T>
): Ret<T> {
    return (this as Iterable<T>).tryMaxByParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.tryMaxByParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryExtractor<R, T>
): Ret<T> {
    return (this as Iterable<T>).tryMaxByParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <T, R : Comparable<R>> List<T>.maxByParallelly(
    crossinline extractor: Extractor<R, T>
): T {
    return this.maxByParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> List<T>.maxByParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<R, T>
): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, R>?>>()
        val segmentAmount = this@maxByParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@maxByParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@maxByParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@maxByParallelly
                    .subList(j, k)
                    .map { Pair(it, extractor(it)) }
                    .maxByOrNull { it.second }
            })
            i = k
        }

        promises.mapNotNull { it.await() }.maxByOrNull { it.second }?.first
    } ?: throw NoSuchElementException()
}

suspend inline fun <T, R : Comparable<R>> List<T>.tryMaxByParallelly(
    crossinline extractor: TryExtractor<R, T>
): Ret<T> {
    return this.tryMaxByParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> List<T>.tryMaxByParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryExtractor<R, T>
): Ret<T> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<T, R>?>>()
            val segmentAmount = this@tryMaxByParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryMaxByParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryMaxByParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryMaxByParallelly
                        .subList(j, k)
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
                        }.maxByOrNull { it.second }
                })
                i = k
            }

            promises.mapNotNull { it.await() }.maxByOrNull { it.second }?.first
        }?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    }
}

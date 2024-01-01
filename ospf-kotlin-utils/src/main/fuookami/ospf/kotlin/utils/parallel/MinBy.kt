package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minByParallelly(
    crossinline extractor: Extractor<R, T>
): T {
    return this.minByParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minByParallelly(
    segment: UInt64,
    crossinline extractor: Extractor<R, T>
): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, R>?>>()
        val iterator = this@minByParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { Pair(it, extractor(it)) }.minByOrNull { it.second }
            })
        }

        promises.mapNotNull { it.await() }.minByOrNull { it.second }?.first
    } ?: throw NoSuchElementException()
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinByParallelly(
    crossinline extractor: TryExtractor<R, T>
): Ret<T> {
    return this.tryMinByParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinByParallelly(
    segment: UInt64,
    crossinline extractor: TryExtractor<R, T>
): Ret<T> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<T, R>?>>()
            val iterator = this@tryMinByParallelly.iterator()
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
                        .minByOrNull { it.second }
                })
            }

            promises.mapNotNull { it.await() }.minByOrNull { it.second }?.first
        }?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    }
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.minByParallelly(
    crossinline extractor: Extractor<R, T>
): T {
    return (this as Iterable<T>).minByParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.minByParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<R, T>
): T {
    return (this as Iterable<T>).minByParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.tryMinByParallelly(
    crossinline extractor: TryExtractor<R, T>
): Ret<T> {
    return (this as Iterable<T>).tryMinByParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.tryMinByParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryExtractor<R, T>
): Ret<T> {
    return (this as Iterable<T>).tryMinByParallelly(UInt64(this.size) / concurrentAmount, extractor)
}


suspend inline fun <T, R : Comparable<R>> List<T>.minByParallelly(
    crossinline extractor: Extractor<R, T>
): T {
    return this.minByParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> List<T>.minByParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<R, T>
): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, R>?>>()
        val segmentAmount = this@minByParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@minByParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@minByParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@minByParallelly.subList(j, k).map { Pair(it, extractor(it)) }.minByOrNull { it.second }
            })
            i = k
        }

        promises.mapNotNull { it.await() }.minByOrNull { it.second }?.first
    } ?: throw NoSuchElementException()
}

suspend inline fun <T, R : Comparable<R>> List<T>.tryMinByParallelly(
    crossinline extractor: TryExtractor<R, T>
): Ret<T> {
    return this.tryMinByParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> List<T>.tryMinByParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryExtractor<R, T>
): Ret<T> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<T, R>?>>()
            val segmentAmount = this@tryMinByParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryMinByParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryMinByParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryMinByParallelly.subList(j, k)
                        .map { Pair(it, when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                return@async null
                            }
                        }) }
                        .minByOrNull { it.second }
                })
                i = k
            }

            promises.mapNotNull { it.await() }.minByOrNull { it.second }?.first
        }?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    }
}

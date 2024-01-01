package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minOfOrNullParallelly(
    crossinline extractor: Extractor<R, T>
): R? {
    return this.minOfOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minOfOrNullParallelly(
    segment: UInt64,
    crossinline extractor: Extractor<R, T>
): R? {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val iterator = this@minOfOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minOfOrNull(extractor)
            })
        }

        promises.mapNotNull { it.await() }.minOrNull()
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinOfOrNullParallelly(
    crossinline extractor: TryExtractor<R, T>
): Ret<R?> {
    return this.tryMinOfOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinOfOrNullParallelly(
    segment: UInt64,
    crossinline extractor: TryExtractor<R, T>
): Ret<R?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            val iterator = this@tryMinOfOrNullParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.minOfOrNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                return@async null
                            }
                        }
                    }
                })
            }

            promises.mapNotNull { it.await() }.minOrNull()
        }?.let { Ok(it) }
            ?: Ok(null)
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(null)
    }
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.minOfOrNullParallelly(
    crossinline extractor: Extractor<R, T>
): R? {
    return (this as Iterable<T>).minOfOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.minOfOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<R, T>
): R? {
    return (this as Iterable<T>).minOfOrNullParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.tryMinOfOrNullParallelly(
    crossinline extractor: TryExtractor<R, T>
): Ret<R?> {
    return (this as Iterable<T>).tryMinOfOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.tryMinOfOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryExtractor<R, T>
): Ret<R?> {
    return (this as Iterable<T>).tryMinOfOrNullParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <T, R : Comparable<R>> List<T>.minOfOrNullParallelly(
    crossinline extractor: Extractor<R, T>
): R? {
    return this.minOfOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> List<T>.minOfOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<R, T>
): R? {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val segmentAmount = this@minOfOrNullParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@minOfOrNullParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@minOfOrNullParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@minOfOrNullParallelly.subList(j, k).minOfOrNull(extractor)
            })
            i = k
        }

        promises.mapNotNull { it.await() }.minOrNull()
    }
}

suspend inline fun <T, R : Comparable<R>> List<T>.tryMinOfOrNullParallelly(
    crossinline extractor: TryExtractor<R, T>
): Ret<R?> {
    return this.tryMinOfOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> List<T>.tryMinOfOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryExtractor<R, T>
): Ret<R?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            val segmentAmount = this@tryMinOfOrNullParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryMinOfOrNullParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryMinOfOrNullParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryMinOfOrNullParallelly
                        .subList(j, k)
                        .minOfOrNull {
                            when (val result = extractor(it)) {
                                is Ok -> {
                                    result.value
                                }

                                is Failed -> {
                                    error = result.error
                                    cancel()
                                    return@async null
                                }
                            }
                        }
                })
                i = k
            }

            promises.mapNotNull { it.await() }.minOrNull()
        }?.let { Ok(it) }
            ?: Ok(null)
    } catch (e: CancellationException) {
        error?.let{ Failed(it) }
            ?: Ok(null)
    }
}

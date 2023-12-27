package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minOfParallelly(
    crossinline extractor: Extractor<R, T>
): R {
    return this.minOfParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minOfParallelly(
    segment: UInt64,
    crossinline extractor: Extractor<R, T>
): R {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val iterator = this@minOfParallelly.iterator()
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
    } ?: throw NoSuchElementException()
}

@JvmName("tryMinOfParallelly")
suspend inline fun <T, R : Comparable<R>> Iterable<T>.minOfParallelly(
    crossinline extractor: TryExtractor<R, T>
): Ret<R> {
    return this.minOfParallelly(UInt64.ten, extractor)
}

@JvmName("tryMinOfParallelly")
suspend inline fun <T, R : Comparable<R>> Iterable<T>.minOfParallelly(
    segment: UInt64,
    crossinline extractor: TryExtractor<R, T>
): Ret<R> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            val iterator = this@minOfParallelly.iterator()
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
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    }
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.minOfParallelly(
    crossinline extractor: Extractor<R, T>
): R {
    return (this as Iterable<T>).minOfParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.minOfParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<R, T>
): R {
    return (this as Iterable<T>).minOfParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

@JvmName("tryMinOfParallelly")
suspend inline fun <T, R : Comparable<R>> Collection<T>.minOfParallelly(
    crossinline extractor: TryExtractor<R, T>
): Ret<R> {
    return (this as Iterable<T>).minOfParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

@JvmName("tryMinOfParallelly")
suspend inline fun <T, R : Comparable<R>> Collection<T>.minOfParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryExtractor<R, T>
): Ret<R> {
    return (this as Iterable<T>).minOfParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <T, R : Comparable<R>> List<T>.minOfParallelly(
    crossinline extractor: Extractor<R, T>
): R {
    return this.minOfParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> List<T>.minOfParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<R, T>
): R {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val segmentAmount = this@minOfParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@minOfParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@minOfParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@minOfParallelly
                    .subList(j, k)
                    .minOfOrNull(extractor)
            })
            i = k
        }

        promises.mapNotNull { it.await() }.minOrNull()
    } ?: throw NoSuchElementException()
}

@JvmName("tryMinOfParallelly")
suspend inline fun <T, R : Comparable<R>> List<T>.minOfParallelly(
    crossinline extractor: TryExtractor<R, T>
): Ret<R> {
    return this.minOfParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

@JvmName("tryMinOfParallelly")
suspend inline fun <T, R : Comparable<R>> List<T>.minOfParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryExtractor<R, T>
): Ret<R> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            val segmentAmount = this@minOfParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@minOfParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@minOfParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@minOfParallelly
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
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    }
}

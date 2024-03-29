package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxOfParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return this.maxOfParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxOfParallelly(
    segment: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val iterator = this@maxOfParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.maxOfOrNull { extractor(it) }
            })
        }

        promises.mapNotNull { it.await() }.maxOrNull()
    } ?: throw NoSuchElementException()
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMaxOfParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R> {
    return this.tryMaxOfParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMaxOfParallelly(
    segment: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            val iterator = this@tryMaxOfParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.maxOfOrNull {
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

            promises.mapNotNull { it.await() }.maxOrNull()
        }?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    }
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.maxOfParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return (this as Iterable<T>).maxOfParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.maxOfParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return (this as Iterable<T>).maxOfParallelly(this.usize / concurrentAmount, extractor)
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.tryMaxOfParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R> {
    return (this as Iterable<T>).tryMaxOfParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.tryMaxOfParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R> {
    return (this as Iterable<T>).tryMaxOfParallelly(this.usize / concurrentAmount, extractor)
}

suspend inline fun <T, R : Comparable<R>> List<T>.maxOfParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return this.maxOfParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> List<T>.maxOfParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val segmentAmount = this@maxOfParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@maxOfParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@maxOfParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@maxOfParallelly.subList(j, k).maxOfOrNull { extractor(it) }
            })
            i = k
        }

        promises.mapNotNull { it.await() }.maxOrNull()
    } ?: throw NoSuchElementException()
}

suspend inline fun <T, R : Comparable<R>> List<T>.tryMaxOfParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R> {
    return this.tryMaxOfParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> List<T>.tryMaxOfParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            val segmentAmount = this@tryMaxOfParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryMaxOfParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryMaxOfParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryMaxOfParallelly.subList(j, k)
                        .maxOfOrNull {
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

            promises.mapNotNull { it.await() }.maxOrNull()
        }?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    }
}

package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxOfOrNullParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return this.maxOfOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxOfOrNullParallelly(
    segment: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val iterator = this@maxOfOrNullParallelly.iterator()
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
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMaxOfOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R?> {
    return this.tryMaxOfOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMaxOfOrNullParallelly(
    segment: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            val iterator = this@tryMaxOfOrNullParallelly.iterator()
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
            ?: Ok(null)
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(null)
    }
}


suspend inline fun <T, R : Comparable<R>> Collection<T>.maxOfOrNullParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return (this as Iterable<T>).maxOfOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.maxOfOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return (this as Iterable<T>).maxOfOrNullParallelly(this.usize / concurrentAmount, extractor)
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.tryMaxOfOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R?> {
    return (this as Iterable<T>).tryMaxOfOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.tryMaxOfOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R?> {
    return (this as Iterable<T>).tryMaxOfOrNullParallelly(this.usize / concurrentAmount, extractor)
}

suspend inline fun <T, R : Comparable<R>> List<T>.maxOfOrNullParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return this.maxOfOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> List<T>.maxOfOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val segmentAmount = this@maxOfOrNullParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@maxOfOrNullParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@maxOfOrNullParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@maxOfOrNullParallelly.subList(j, k).maxOfOrNull { extractor(it) }
            })
            i = k
        }

        promises.mapNotNull { it.await() }.maxOrNull()
    }
}

suspend inline fun <T, R : Comparable<R>> List<T>.tryMaxOfOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R?> {
    return this.tryMaxOfOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> List<T>.tryMaxOfOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            val segmentAmount = this@tryMaxOfOrNullParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryMaxOfOrNullParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryMaxOfOrNullParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryMaxOfOrNullParallelly.subList(j, k).maxOfOrNull {
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
            ?: Ok(null)
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(null)
    }
}

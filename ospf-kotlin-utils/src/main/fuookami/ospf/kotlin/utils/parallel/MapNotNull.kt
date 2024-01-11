package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <R, T> Iterable<T>.mapNotNullParallelly(
    crossinline extractor: SuspendExtractor<R?, T>
): List<R> {
    return this.mapNotNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <R, T> Iterable<T>.mapNotNullParallelly(
    segment: UInt64,
    crossinline extractor: SuspendExtractor<R?, T>
): List<R> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val iterator = this@mapNotNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.mapNotNull { extractor(it) }
            })
        }

        promises.flatMap { it.await() }
    }
}

suspend inline fun <R, T> Iterable<T>.tryMapNotNullParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<List<R>> {
    return this.tryMapNotNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <R, T> Iterable<T>.tryMapNotNullParallelly(
    segment: UInt64,
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<List<R>> {
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

            Ok(promises.flatMap { it.await() })
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(emptyList())
    }
}

suspend inline fun <R, T> Collection<T>.mapNotNullParallelly(
    crossinline extractor: SuspendExtractor<R?, T>
): List<R> {
    return (this as Iterable<T>).mapNotNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <R, T> Collection<T>.mapNotNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendExtractor<R?, T>
): List<R> {
    return (this as Iterable<T>).mapNotNullParallelly(this.usize / concurrentAmount, extractor)
}

suspend inline fun <R, T> Collection<T>.tryMapNotNullParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<List<R>> {
    return (this as Iterable<T>).tryMapNotNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <R, T> Collection<T>.tryMapNotNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<List<R>> {
    return (this as Iterable<T>).tryMapNotNullParallelly(this.usize / concurrentAmount, extractor)
}

suspend inline fun <R, T> List<T>.mapNotNullParallelly(
    crossinline extractor: SuspendExtractor<R?, T>
): List<R> {
    return this.mapNotNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <R, T> List<T>.mapNotNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendExtractor<R?, T>
): List<R> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val segmentAmount = this@mapNotNullParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@mapNotNullParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@mapNotNullParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@mapNotNullParallelly.subList(j, k).mapNotNull { extractor(it) }
            })
            i = k
        }

        promises.flatMap { it.await() }
    }
}

suspend inline fun <R, T> List<T>.tryMapNotNullParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<List<R>> {
    return this.tryMapNotNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <R, T> List<T>.tryMapNotNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<List<R>> {
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

            Ok(promises.flatMap { it.await() })
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(emptyList())
    }
}

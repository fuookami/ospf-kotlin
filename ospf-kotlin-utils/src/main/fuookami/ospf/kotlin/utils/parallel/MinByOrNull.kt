package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minByOrNullParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): T? {
    return this.minByOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minByOrNullParallelly(
    segment: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, R>?>>()
        val iterator = this@minByOrNullParallelly.iterator()
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
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinByOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<T?> {
    return this.tryMinByOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinByOrNullParallelly(
    segment: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<T?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<T, R>?>>()
            val iterator = this@tryMinByOrNullParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment
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
            }

            promises.mapNotNull { it.await() }.minByOrNull { it.second }?.first
        }?.let { Ok(it) }
            ?: Ok(null)
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(null)
    }
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.minByOrNullParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): T? {
    return (this as Iterable<T>).minByOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.minByOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): T? {
    return (this as Iterable<T>).minByOrNullParallelly(this.usize / concurrentAmount, extractor)
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.tryMinByOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<T?> {
    return (this as Iterable<T>).tryMinByOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.tryMinByOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<T?> {
    return (this as Iterable<T>).tryMinByOrNullParallelly(this.usize / concurrentAmount, extractor)
}


suspend inline fun <T, R : Comparable<R>> List<T>.minByOrNullParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): T? {
    return this.minByOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> List<T>.minByOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, R>?>>()
        val segmentAmount = this@minByOrNullParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@minByOrNullParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@minByOrNullParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@minByOrNullParallelly.subList(j, k).map { Pair(it, extractor(it)) }.minByOrNull { it.second }
            })
            i = k
        }

        promises.mapNotNull { it.await() }.minByOrNull { it.second }?.first
    }
}

@JvmName("tryMinByOrNullParallelly")
suspend inline fun <T, R : Comparable<R>> List<T>.minByOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<T?> {
    return this.minByOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

@JvmName("tryMinByOrNullParallelly")
suspend inline fun <T, R : Comparable<R>> List<T>.minByOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<T?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<T, R>?>>()
            val segmentAmount = this@minByOrNullParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@minByOrNullParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@minByOrNullParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@minByOrNullParallelly
                        .subList(j, k)
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
            ?: Ok(null)
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(null)
    }
}

package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxOfOrNullParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R>? {
    return this.minMaxOfOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxOfOrNullParallelly(
    segment: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R>? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<R, R>?>>()
        val iterator = this@minMaxOfOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minMaxOrNull()
            })
        }

        val segmentResults = promises.mapNotNull { it.await() }
        val minPromise = async(Dispatchers.Default) {
            segmentResults.minOfOrNull { it.first }
        }
        val maxPromise = async(Dispatchers.Default) {
            segmentResults.maxOfOrNull { it.second }
        }

        val min = when (val min = minPromise.await()) {
            null -> {
                return@coroutineScope null
            }

            else -> {
                min
            }
        }

        val max = when (val max = maxPromise.await()) {
            null -> {
                return@coroutineScope null
            }

            else -> {
                max
            }
        }

        Pair(min, max)
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinMaxOfOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>?> {
    return this.tryMinMaxOfOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinMaxOfOrNullParallelly(
    segment: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<R, R>?>>()
            val iterator = this@tryMinMaxOfOrNullParallelly.iterator()
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
                        .minMaxOrNull()
                })
            }

            val segmentResults = promises.mapNotNull { it.await() }
            val minPromise = async(Dispatchers.Default) {
                segmentResults.minOfOrNull { it.first }
            }
            val maxPromise = async(Dispatchers.Default) {
                segmentResults.maxOfOrNull { it.second }
            }

            val min = when (val min = minPromise.await()) {
                null -> {
                    return@coroutineScope null
                }

                else -> {
                    min
                }
            }

            val max = when (val max = maxPromise.await()) {
                null -> {
                    return@coroutineScope null
                }

                else -> {
                    max
                }
            }

            Pair(min, max)
        }?.let { Ok(it) }
            ?: Ok(null)
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(null)
    }
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.minMaxOfOrNullParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R>? {
    return (this as Iterable<T>).minMaxOfOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.minMaxOfOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R>? {
    return (this as Iterable<T>).minMaxOfOrNullParallelly(this.usize / concurrentAmount, extractor)
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.tryMinMaxOfOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>?> {
    return (this as Iterable<T>).tryMinMaxOfOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.tryMinMaxOfOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>?> {
    return (this as Iterable<T>).tryMinMaxOfOrNullParallelly(this.usize / concurrentAmount, extractor)
}

suspend inline fun <T, R : Comparable<R>> List<T>.minMaxOfOrNullParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R>? {
    return this.minMaxOfOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> List<T>.minMaxOfOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R>? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<R, R>?>>()
        val segmentAmount = this@minMaxOfOrNullParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@minMaxOfOrNullParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@minMaxOfOrNullParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@minMaxOfOrNullParallelly.subList(j, k).map { extractor(it) }.minMaxOrNull()
            })
            i = k
        }

        val segmentResults = promises.mapNotNull { it.await() }
        val minPromise = async(Dispatchers.Default) {
            segmentResults.minOfOrNull { it.first }
        }
        val maxPromise = async(Dispatchers.Default) {
            segmentResults.maxOfOrNull { it.second }
        }

        val min = when (val min = minPromise.await()) {
            null -> {
                return@coroutineScope null
            }

            else -> {
                min
            }
        }

        val max = when (val max = maxPromise.await()) {
            null -> {
                return@coroutineScope null
            }

            else -> {
                max
            }
        }

        Pair(min, max)
    }
}

suspend inline fun <T, R : Comparable<R>> List<T>.tryMinMaxOfOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>?> {
    return this.tryMinMaxOfOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> List<T>.tryMinMaxOfOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<R, R>?>>()
            val segmentAmount = this@tryMinMaxOfOrNullParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryMinMaxOfOrNullParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryMinMaxOfOrNullParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryMinMaxOfOrNullParallelly
                        .subList(j, k)
                        .map {
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
                        .minMaxOrNull()
                })
                i = k
            }

            val segmentResults = promises.mapNotNull { it.await() }
            val minPromise = async(Dispatchers.Default) {
                segmentResults.minOfOrNull { it.first }
            }
            val maxPromise = async(Dispatchers.Default) {
                segmentResults.maxOfOrNull { it.second }
            }

            val min = when (val min = minPromise.await()) {
                null -> {
                    return@coroutineScope null
                }

                else -> {
                    min
                }
            }

            val max = when (val max = maxPromise.await()) {
                null -> {
                    return@coroutineScope null
                }

                else -> {
                    max
                }
            }

            Pair(min, max)
        }?.let { Ok(it) }
            ?: Ok(null)
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(null)
    }
}

package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxByOrNullParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): Pair<T, T>? {
    return this.minMaxByOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxByOrNullParallelly(
    segment: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<T, T>? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<Pair<T, R>, Pair<T, R>>?>>()
        val iterator = this@minMaxByOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment
                    .map { Pair(it, extractor(it)) }
                    .minMaxByOrNull { it.second }
            })
        }

        val segmentResults = promises.mapNotNull { it.await() }
        val minPromise = async(Dispatchers.Default) {
            segmentResults.map { it.first }.minByOrNull { it.second }
        }
        val maxPromise = async(Dispatchers.Default) {
            segmentResults.map { it.second }.maxByOrNull { it.second }
        }

        val min = when (val min = minPromise.await()) {
            null -> {
                return@coroutineScope null
            }

            else -> {
                min.first
            }
        }

        val max = when (val max = maxPromise.await()) {
            null -> {
                return@coroutineScope null
            }

            else -> {
                max.first
            }
        }

        Pair(min, max)
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinMayByOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<T, T>?> {
    return this.tryMinMayByOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinMayByOrNullParallelly(
    segment: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<T, T>?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<Pair<T, R>, Pair<T, R>>?>>()
            val iterator = this@tryMinMayByOrNullParallelly.iterator()
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
                        .minMaxByOrNull { it.second }
                })
            }

            val segmentResults = promises.mapNotNull { it.await() }
            val minPromise = async(Dispatchers.Default) {
                segmentResults.map { it.first }.minByOrNull { it.second }
            }
            val maxPromise = async(Dispatchers.Default) {
                segmentResults.map { it.second }.maxByOrNull { it.second }
            }

            val min = when (val min = minPromise.await()) {
                null -> {
                    return@coroutineScope null
                }

                else -> {
                    min.first
                }
            }

            val max = when (val max = maxPromise.await()) {
                null -> {
                    return@coroutineScope null
                }

                else -> {
                    max.first
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

suspend inline fun <T, R : Comparable<R>> Collection<T>.minMaxByOrNullParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): Pair<T, T>? {
    return (this as Iterable<T>).minMaxByOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.minMaxByOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<T, T>? {
    return (this as Iterable<T>).minMaxByOrNullParallelly(this.usize / concurrentAmount, extractor)
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.tryMinMayByOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<T, T>?> {
    return (this as Iterable<T>).tryMinMayByOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.tryMinMayByOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<T, T>?> {
    return (this as Iterable<T>).tryMinMayByOrNullParallelly(this.usize / concurrentAmount, extractor)
}

suspend inline fun <T, R : Comparable<R>> List<T>.minMaxByOrNullParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): Pair<T, T>? {
    return this.minMaxByOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> List<T>.minMaxByOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<T, T>? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<Pair<T, R>, Pair<T, R>>?>>()
        val segmentAmount = this@minMaxByOrNullParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@minMaxByOrNullParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@minMaxByOrNullParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@minMaxByOrNullParallelly.subList(j, k).map { Pair(it, extractor(it)) }.minMaxByOrNull { it.second }
            })
            i = k
        }

        val segmentResults = promises.mapNotNull { it.await() }
        val minPromise = async(Dispatchers.Default) {
            segmentResults.map { it.first }.minByOrNull { it.second }
        }
        val maxPromise = async(Dispatchers.Default) {
            segmentResults.map { it.second }.maxByOrNull { it.second }
        }

        val min = when (val min = minPromise.await()) {
            null -> {
                return@coroutineScope null
            }

            else -> {
                min.first
            }
        }

        val max = when (val max = maxPromise.await()) {
            null -> {
                return@coroutineScope null
            }

            else -> {
                max.first
            }
        }

        Pair(min, max)
    }
}

suspend inline fun <T, R : Comparable<R>> List<T>.tryMinMayByOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<T, T>?> {
    return this.tryMinMayByOrNullParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> List<T>.tryMinMayByOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<T, T>?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<Pair<T, R>, Pair<T, R>>?>>()
            val segmentAmount = this@tryMinMayByOrNullParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryMinMayByOrNullParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryMinMayByOrNullParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryMinMayByOrNullParallelly
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
                        }
                        .minMaxByOrNull { it.second }
                })
                i = k
            }

            val segmentResults = promises.mapNotNull { it.await() }
            val minPromise = async(Dispatchers.Default) {
                segmentResults.map { it.first }.minByOrNull { it.second }
            }
            val maxPromise = async(Dispatchers.Default) {
                segmentResults.map { it.second }.maxByOrNull { it.second }
            }

            val min = when (val min = minPromise.await()) {
                null -> {
                    return@coroutineScope null
                }

                else -> {
                    min.first
                }
            }

            val max = when (val max = maxPromise.await()) {
                null -> {
                    return@coroutineScope null
                }

                else -> {
                    max.first
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

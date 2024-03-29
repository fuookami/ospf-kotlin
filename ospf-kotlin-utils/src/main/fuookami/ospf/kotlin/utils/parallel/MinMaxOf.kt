package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxOfParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R> {
    return this.minMaxOfParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxOfParallelly(
    segment: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<R, R>?>>()
        val iterator = this@minMaxOfParallelly.iterator()
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
    } ?: throw NoSuchElementException()
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinMaxOfParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>> {
    return this.tryMinMaxOfParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinMaxOfParallelly(
    segment: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<R, R>?>>()
            val iterator = this@tryMinMaxOfParallelly.iterator()
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
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    }
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.minMaxOfParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R> {
    return (this as Iterable<T>).minMaxOfParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.minMaxOfParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R> {
    return (this as Iterable<T>).minMaxOfParallelly(this.usize / concurrentAmount, extractor)
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.tryMinMaxOfParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>> {
    return (this as Iterable<T>).tryMinMaxOfParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.tryMinMaxOfParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>> {
    return (this as Iterable<T>).tryMinMaxOfParallelly(this.usize / concurrentAmount, extractor)
}

suspend inline fun <T, R : Comparable<R>> List<T>.minMaxOfParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R> {
    return this.minMaxOfParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> List<T>.minMaxOfParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<R, R>?>>()
        val segmentAmount = this@minMaxOfParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@minMaxOfParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@minMaxOfParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@minMaxOfParallelly.subList(j, k).map { extractor(it) }.minMaxOrNull()
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
    } ?: throw NoSuchElementException()
}

suspend inline fun <T, R : Comparable<R>> List<T>.tryMinMaxOfParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>> {
    return this.tryMinMaxOfParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> List<T>.tryMinMaxOfParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<R, R>?>>()
            val segmentAmount = this@tryMinMaxOfParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryMinMaxOfParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryMinMaxOfParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryMinMaxOfParallelly
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
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    }
}

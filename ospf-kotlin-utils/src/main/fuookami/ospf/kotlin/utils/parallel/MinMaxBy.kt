package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxByParallelly(
    crossinline extractor: Extractor<R, T>
): Pair<T, T> {
    return this.minMaxByParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxByParallelly(
    segment: UInt64,
    crossinline extractor: Extractor<R, T>
): Pair<T, T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<Pair<T, R>, Pair<T, R>>?>>()
        val iterator = this@minMaxByParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { Pair(it, extractor(it)) }.minMaxByOrNull { it.second }
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
    } ?: throw NoSuchElementException()
}

@JvmName("tryMinMaxByParallelly")
suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxByParallelly(
    crossinline extractor: TryExtractor<R, T>
): Ret<Pair<T, T>> {
    return this.minMaxByParallelly(UInt64.ten, extractor)
}

@JvmName("tryMinMaxByParallelly")
suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxByParallelly(
    segment: UInt64,
    crossinline extractor: TryExtractor<R, T>
): Ret<Pair<T, T>> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<Pair<T, R>, Pair<T, R>>?>>()
            val iterator = this@minMaxByParallelly.iterator()
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
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    }
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.minMaxByParallelly(
    crossinline extractor: Extractor<R, T>
): Pair<T, T> {
    return (this as Iterable<T>).minMaxByParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.minMaxByParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<R, T>
): Pair<T, T> {
    return (this as Iterable<T>).minMaxByParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

@JvmName("tryMinMaxByParallelly")
suspend inline fun <T, R : Comparable<R>> Collection<T>.minMaxByParallelly(
    crossinline extractor: TryExtractor<R, T>
): Ret<Pair<T, T>> {
    return (this as Iterable<T>).minMaxByParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

@JvmName("tryMinMaxByParallelly")
suspend inline fun <T, R : Comparable<R>> Collection<T>.minMaxByParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryExtractor<R, T>
): Ret<Pair<T, T>> {
    return (this as Iterable<T>).minMaxByParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <T, R : Comparable<R>> List<T>.minMaxByParallelly(
    crossinline extractor: Extractor<R, T>
): Pair<T, T> {
    return this.minMaxByParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> List<T>.minMaxByParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<R, T>
): Pair<T, T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<Pair<T, R>, Pair<T, R>>?>>()
        val segmentAmount = this@minMaxByParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@minMaxByParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@minMaxByParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@minMaxByParallelly.subList(j, k).map { Pair(it, extractor(it)) }.minMaxByOrNull { it.second }
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
    } ?: throw NoSuchElementException()
}

@JvmName("tryMinMaxByParallelly")
suspend inline fun <T, R : Comparable<R>> List<T>.minMaxByParallelly(
    crossinline extractor: TryExtractor<R, T>
): Ret<Pair<T, T>> {
    return this.minMaxByParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

@JvmName("tryMinMaxByParallelly")
suspend inline fun <T, R : Comparable<R>> List<T>.minMaxByParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryExtractor<R, T>
): Ret<Pair<T, T>> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<Pair<T, R>, Pair<T, R>>?>>()
            val segmentAmount = this@minMaxByParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@minMaxByParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@minMaxByParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@minMaxByParallelly
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
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "no such element"))
    }
}

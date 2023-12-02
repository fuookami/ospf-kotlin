package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <K, V, T> Iterable<T>.associateParallelly(crossinline extractor: Extractor<Pair<K, V>, T>): Map<K, V> {
    return this.associateParallelly(UInt64.ten, extractor)
}

suspend inline fun <K, V, T> Iterable<T>.associateParallelly(segment: UInt64, crossinline extractor: Extractor<Pair<K, V>, T>): Map<K, V> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<Pair<K, V>>>>()
        val iterator = this@associateParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map(extractor)
            })
        }

        promises.flatMap { it.await() }.toMap()
    }
}

suspend inline fun <K, V, T> Iterable<T>.associateParallelly(crossinline extractor: TryExtractor<Pair<K, V>, T>): Result<Map<K, V>, Error> {
    return this.associateParallelly(UInt64.ten, extractor)
}

suspend inline fun <K, V, T> Iterable<T>.associateParallelly(segment: UInt64, crossinline extractor: TryExtractor<Pair<K, V>, T>): Result<Map<K, V>, Error> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<Pair<K, V>>>>()
            val iterator = this@associateParallelly.iterator()
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

            Ok(promises.flatMap { it.await() }.toMap())
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(emptyMap())
    }
}

suspend inline fun <K, V, T> Collection<T>.associateParallelly(crossinline extractor: Extractor<Pair<K, V>, T>): Map<K, V> {
    return (this as Iterable<T>).associateParallelly(
        UInt64(
            maxOf(
                minOf(
                    Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                    Runtime.getRuntime().availableProcessors()
                ),
                1
            )
        ),
        extractor
    )
}

suspend inline fun <K, V, T> Collection<T>.associateParallelly(concurrentAmount: UInt64, crossinline extractor: Extractor<Pair<K, V>, T>): Map<K, V> {
    return (this as Iterable<T>).associateParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <K, V, T> Collection<T>.associateParallelly(crossinline extractor: TryExtractor<Pair<K, V>, T>): Result<Map<K, V>, Error> {
    return (this as Iterable<T>).associateParallelly(
        UInt64(
            maxOf(
                minOf(
                    Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                    Runtime.getRuntime().availableProcessors()
                ),
                1
            )
        ),
        extractor
    )
}

suspend inline fun <K, V, T> Collection<T>.associateParallelly(concurrentAmount: UInt64, crossinline extractor: TryExtractor<Pair<K, V>, T>): Result<Map<K, V>, Error> {
    return (this as Iterable<T>).associateParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <K, V, T> List<T>.associateParallelly(crossinline extractor: Extractor<Pair<K, V>, T>): Map<K, V> {
    return this.associateParallelly(
        UInt64(
            maxOf(
                minOf(
                    Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                    Runtime.getRuntime().availableProcessors()
                ),
                1
            )
        ),
        extractor
    )
}

suspend inline fun <K, V, T> List<T>.associateParallelly(concurrentAmount: UInt64, crossinline extractor: Extractor<Pair<K, V>, T>): Map<K, V> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<Pair<K, V>>>>()
        val segmentAmount = this@associateParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@associateParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@associateParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@associateParallelly.subList(j, k).map(extractor)
            })
            i = k
        }

        promises.flatMap { it.await() }.toMap()
    }
}

suspend inline fun <K, V, T> List<T>.associateParallelly(crossinline extractor: TryExtractor<Pair<K, V>, T>): Result<Map<K, V>, Error> {
    return this.associateParallelly(
        UInt64(
            maxOf(
                minOf(
                    Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                    Runtime.getRuntime().availableProcessors()
                ),
                1
            )
        ),
        extractor
    )
}

suspend inline fun <K, V, T> List<T>.associateParallelly(concurrentAmount: UInt64, crossinline extractor: TryExtractor<Pair<K, V>, T>): Result<Map<K, V>, Error> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<Pair<K, V>>>>()
            val segmentAmount = this@associateParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@associateParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@associateParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@associateParallelly.subList(j, k).mapNotNull {
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

            Ok(promises.flatMap { it.await() }.toMap())
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(emptyMap())
    }
}

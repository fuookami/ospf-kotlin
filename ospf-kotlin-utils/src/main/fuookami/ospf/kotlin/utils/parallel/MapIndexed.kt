package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <R, T> Iterable<T>.mapIndexedParallelly(crossinline extractor: IndexedExtractor<R, T>): List<R> {
    return this.mapIndexedParallelly(UInt64.ten, extractor)
}

suspend inline fun <R, T> Iterable<T>.mapIndexedParallelly(
    segment: UInt64,
    crossinline extractor: IndexedExtractor<R, T>
): List<R> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val iterator = this@mapIndexedParallelly.iterator()
        var i = 0
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<Pair<Int, T>>()
            var j = UInt64.zero
            while (iterator.hasNext() && j != segment) {
                thisSegment.add(Pair(i, iterator.next()))
                ++i
                ++j
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it.first, it.second) }
            })
        }

        promises.flatMap { it.await() }
    }
}

suspend inline fun <R, T> Iterable<T>.mapIndexedParallelly(crossinline extractor: TryIndexedExtractor<R, T>): Ret<List<R>> {
    return this.mapIndexedParallelly(UInt64.ten, extractor)
}

suspend inline fun <R, T> Iterable<T>.mapIndexedParallelly(
    segment: UInt64,
    crossinline extractor: TryIndexedExtractor<R, T>
): Ret<List<R>> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<R>>>()
            val iterator = this@mapIndexedParallelly.iterator()
            var i = 0
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<Pair<Int, T>>()
                var j = UInt64.zero
                while (iterator.hasNext() && j != segment) {
                    thisSegment.add(Pair(i, iterator.next()))
                    ++i
                    ++j
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it.first, it.second)) {
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

suspend inline fun <R, T> Collection<T>.mapIndexedParallelly(crossinline extractor: IndexedExtractor<R, T>): List<R> {
    return (this as Iterable<T>).mapIndexedParallelly(
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

suspend inline fun <R, T> Collection<T>.mapIndexedParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: IndexedExtractor<R, T>
): List<R> {
    return (this as Iterable<T>).mapIndexedParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <R, T> Collection<T>.mapIndexedParallelly(crossinline extractor: TryIndexedExtractor<R, T>): Ret<List<R>> {
    return (this as Iterable<T>).mapIndexedParallelly(
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

suspend inline fun <R, T> Collection<T>.mapIndexedParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryIndexedExtractor<R, T>
): Ret<List<R>> {
    return (this as Iterable<T>).mapIndexedParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <R, T> List<T>.mapIndexedParallelly(crossinline extractor: IndexedExtractor<R, T>): List<R> {
    return this.mapIndexedParallelly(
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

suspend inline fun <R, T> List<T>.mapIndexedParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: IndexedExtractor<R, T>
): List<R> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val segmentAmount = this@mapIndexedParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@mapIndexedParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@mapIndexedParallelly.size - j
            )
            promises.add(async(Dispatchers.Default) {
                this@mapIndexedParallelly.mapIndexed { i, v -> extractor(i + j, v) }
            })
            i = k
        }

        promises.flatMap { it.await() }
    }
}

suspend inline fun <R, T> List<T>.mapIndexedParallelly(crossinline extractor: TryIndexedExtractor<R, T>): Ret<List<R>> {
    return this.mapIndexedParallelly(
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

suspend inline fun <R, T> List<T>.mapIndexedParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryIndexedExtractor<R, T>
): Ret<List<R>> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<R>>>()
            val segmentAmount = this@mapIndexedParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@mapIndexedParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@mapIndexedParallelly.size - j
                )
                promises.add(async(Dispatchers.Default) {
                    this@mapIndexedParallelly.mapIndexedNotNull { i, v ->
                        when (val result = extractor(i + j, v)) {
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

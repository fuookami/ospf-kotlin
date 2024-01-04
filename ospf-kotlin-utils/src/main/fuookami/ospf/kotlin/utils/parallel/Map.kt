package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <R, T> Iterable<T>.mapParallelly(
    crossinline extractor: Extractor<R, T>
): List<R> {
    return this.mapParallelly(UInt64.ten, extractor)
}

suspend inline fun <R, T> Iterable<T>.mapParallelly(
    segment: UInt64,
    crossinline extractor: Extractor<R, T>
): List<R> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val iterator = this@mapParallelly.iterator()
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

        promises.flatMap { it.await() }
    }
}

suspend inline fun <R, T> Iterable<T>.tryMapParallelly(
    crossinline extractor: TryExtractor<R, T>
): Ret<List<R>> {
    return this.tryMapParallelly(UInt64.ten, extractor)
}

suspend inline fun <R, T> Iterable<T>.tryMapParallelly(
    segment: UInt64,
    crossinline extractor: TryExtractor<R, T>
): Ret<List<R>> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<R>>>()
            val iterator = this@tryMapParallelly.iterator()
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

suspend inline fun <R, T> Collection<T>.mapParallelly(
    crossinline extractor: Extractor<R, T>
): List<R> {
    return this.mapParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <R, T> Collection<T>.mapParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<R, T>
): List<R> {
    return (this as Iterable<T>).mapParallelly(this.usize / concurrentAmount, extractor)
}

suspend inline fun <R, T> Collection<T>.tryMapParallelly(
    crossinline extractor: TryExtractor<R, T>
): Ret<List<R>> {
    return this.tryMapParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <R, T> Collection<T>.tryMapParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryExtractor<R, T>
): Ret<List<R>> {
    return (this as Iterable<T>).tryMapParallelly(this.usize / concurrentAmount, extractor)
}

suspend inline fun <R, T> List<T>.mapParallelly(
    crossinline extractor: Extractor<R, T>
): List<R> {
    return this.mapParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <R, T> List<T>.mapParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<R, T>
): List<R> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val segmentAmount = this@mapParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@mapParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@mapParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@mapParallelly.subList(j, k).map(extractor)
            })
            i = k
        }

        promises.flatMap { it.await() }
    }
}

suspend inline fun <R, T> List<T>.tryMapParallelly(
    crossinline extractor: TryExtractor<R, T>
): Ret<List<R>> {
    return this.tryMapParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <R, T> List<T>.tryMapParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryExtractor<R, T>
): Ret<List<R>> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<R>>>()
            val segmentAmount = this@tryMapParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryMapParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryMapParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryMapParallelly.subList(j, k).mapNotNull {
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

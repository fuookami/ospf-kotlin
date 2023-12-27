package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <R, T> Iterable<T>.flatMapParallelly(
    crossinline extractor: Extractor<Iterable<R>, T>
): List<R> {
    return this.flatMapParallelly(UInt64.ten, extractor)
}

suspend inline fun <R, T> Iterable<T>.flatMapParallelly(
    segment: UInt64,
    crossinline extractor: Extractor<Iterable<R>, T>
): List<R> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val iterator = this@flatMapParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.flatMap(extractor)
            })
        }

        promises.flatMap { it.await() }
    }
}

@JvmName("tryFlatMapParallelly")
suspend inline fun <R, T> Iterable<T>.flatMapParallelly(
    crossinline extractor: TryExtractor<Iterable<R>, T>
): Ret<List<R>> {
    return this.flatMapParallelly(UInt64.ten, extractor)
}

@JvmName("tryFlatMapParallelly")
suspend inline fun <R, T> Iterable<T>.flatMapParallelly(
    segment: UInt64,
    crossinline extractor: TryExtractor<Iterable<R>, T>
): Ret<List<R>> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<R>>>()
            val iterator = this@flatMapParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.flatMap {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                emptyList()
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

suspend inline fun <R, T> Collection<T>.flatMapParallelly(
    crossinline extractor: Extractor<Iterable<R>, T>
): List<R> {
    return (this as Iterable<T>).flatMapParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <R, T> Collection<T>.flatMapParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<Iterable<R>, T>
): List<R> {
    return (this as Iterable<T>).flatMapParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

@JvmName("tryFlatMapParallelly")
suspend inline fun <R, T> Collection<T>.flatMapParallelly(
    crossinline extractor: TryExtractor<Iterable<R>, T>
): Ret<List<R>> {
    return (this as Iterable<T>).flatMapParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

@JvmName("tryFlatMapParallelly")
suspend inline fun <R, T> Collection<T>.flatMapParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryExtractor<Iterable<R>, T>
): Ret<List<R>> {
    return (this as Iterable<T>).flatMapParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <R, T> List<T>.flatMapParallelly(
    crossinline extractor: Extractor<Iterable<R>, T>
): List<R> {
    return this.flatMapParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

suspend inline fun <R, T> List<T>.flatMapParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<Iterable<R>, T>
): List<R> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val segmentAmount = this@flatMapParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@flatMapParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@flatMapParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@flatMapParallelly.subList(j, k).flatMap(extractor)
            })
            i = k
        }

        promises.flatMap { it.await() }
    }
}

@JvmName("tryFlatMapParallelly")
suspend inline fun <R, T> List<T>.flatMapParallelly(
    crossinline extractor: TryExtractor<Iterable<R>, T>
): Ret<List<R>> {
    return this.flatMapParallelly(
        defaultConcurrentAmount,
        extractor
    )
}

@JvmName("tryFlatMapParallelly")
suspend inline fun <R, T> List<T>.flatMapParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: TryExtractor<Iterable<R>, T>
): Ret<List<R>> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<R>>>()
            val segmentAmount = this@flatMapParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@flatMapParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@flatMapParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@flatMapParallelly.subList(j, k).flatMap {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                emptyList()
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

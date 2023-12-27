package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, V, M : MutableMap<in T, in V>> Iterable<T>.associateWithToParallelly(
    destination: M,
    crossinline extractor: Extractor<V, T>
): M {
    return this.associateWithToParallelly(UInt64.ten, destination, extractor)
}

suspend inline fun <T, V, M : MutableMap<in T, in V>> Iterable<T>.associateWithToParallelly(
    segment: UInt64,
    destination: M,
    crossinline extractor: Extractor<V, T>
): M {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<Pair<T, V>>>>()
        val iterator = this@associateWithToParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { Pair(it, extractor(it)) }
            })
        }

        promises.flatMap { it.await() }.toMap(destination)
    }
}

@JvmName("tryAssociateWithToParallelly")
suspend inline fun <T, V, M : MutableMap<in T, in V>> Iterable<T>.associateWithToParallelly(
    destination: M,
    crossinline extractor: TryExtractor<V, T>
): Ret<M> {
    return this.associateWithToParallelly(UInt64.ten, destination, extractor)
}

@JvmName("tryAssociateWithToParallelly")
suspend inline fun <T, V, M : MutableMap<in T, in V>> Iterable<T>.associateWithToParallelly(
    segment: UInt64,
    destination: M,
    crossinline extractor: TryExtractor<V, T>
): Ret<M> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<Pair<T, V>>>>()
            val iterator = this@associateWithToParallelly.iterator()
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
                                Pair(it, result.value)
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

            Ok(promises.flatMap { it.await() }.toMap(destination))
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(destination)
    }
}

suspend inline fun <T, V, M : MutableMap<in T, in V>> Collection<T>.associateWithToParallelly(
    destination: M,
    crossinline extractor: Extractor<V, T>
): M {
    return (this as Iterable<T>).associateWithToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <T, V, M : MutableMap<in T, in V>> Collection<T>.associateWithToParallelly(
    concurrentAmount: UInt64,
    destination: M,
    crossinline extractor: Extractor<V, T>
): M {
    return (this as Iterable<T>).associateWithToParallelly(UInt64(this.size) / concurrentAmount, destination, extractor)
}

@JvmName("tryAssociateWithToParallelly")
suspend inline fun <T, V, M : MutableMap<in T, in V>> Collection<T>.associateWithToParallelly(
    destination: M,
    crossinline extractor: TryExtractor<V, T>
): Ret<M> {
    return (this as Iterable<T>).associateWithToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

@JvmName("tryAssociateWithToParallelly")
suspend inline fun <T, V, M : MutableMap<in T, in V>> Collection<T>.associateWithToParallelly(
    concurrentAmount: UInt64,
    destination: M,
    crossinline extractor: TryExtractor<V, T>
): Ret<M> {
    return (this as Iterable<T>).associateWithToParallelly(UInt64(this.size) / concurrentAmount, destination, extractor)
}

suspend inline fun <T, V, M : MutableMap<in T, in V>> List<T>.associateWithToParallelly(
    destination: M,
    crossinline extractor: Extractor<V, T>
): M {
    return this.associateWithToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <T, V, M : MutableMap<in T, in V>> List<T>.associateWithToParallelly(
    concurrentAmount: UInt64,
    destination: M,
    crossinline extractor: Extractor<V, T>
): M {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<Pair<T, V>>>>()
        val segmentAmount = this@associateWithToParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@associateWithToParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@associateWithToParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@associateWithToParallelly.subList(j, k).map { Pair(it, extractor(it)) }
            })
            i = k
        }

        promises.flatMap { it.await() }.toMap(destination)
    }
}

@JvmName("tryAssociateWithToParallelly")
suspend inline fun <T, V, M : MutableMap<in T, in V>> List<T>.associateWithToParallelly(
    destination: M,
    crossinline extractor: TryExtractor<V, T>
): Ret<M> {
    return this.associateWithToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

@JvmName("tryAssociateWithToParallelly")
suspend inline fun <T, V, M : MutableMap<in T, in V>> List<T>.associateWithToParallelly(
    concurrentAmount: UInt64,
    destination: M,
    crossinline extractor: TryExtractor<V, T>
): Ret<M> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<Pair<T, V>>>>()
            val segmentAmount = this@associateWithToParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@associateWithToParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@associateWithToParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@associateWithToParallelly.subList(j, k).mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                Pair(it, result.value)
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

            Ok(promises.flatMap { it.await() }.toMap(destination))
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(destination)
    }
}

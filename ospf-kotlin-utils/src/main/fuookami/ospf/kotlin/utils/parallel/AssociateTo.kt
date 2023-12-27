package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <K, V, T, M : MutableMap<in K, in V>> Iterable<T>.associateToParallelly(
    destination: M,
    crossinline extractor: Extractor<Pair<K, V>, T>
): M {
    return this.associateToParallelly(UInt64.ten, destination, extractor)
}

suspend inline fun <K, V, T, M : MutableMap<in K, in V>> Iterable<T>.associateToParallelly(
    segment: UInt64,
    destination: M,
    crossinline extractor: Extractor<Pair<K, V>, T>
): M {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<Pair<K, V>>>>()
        val iterator = this@associateToParallelly.iterator()
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

        promises.flatMap { it.await() }.toMap(destination)
    }
}

@JvmName("tryAssociateToParallelly")
suspend inline fun <K, V, T, M : MutableMap<in K, in V>> Iterable<T>.associateToParallelly(
    destination: M,
    crossinline extractor: TryExtractor<Pair<K, V>, T>
): Ret<M> {
    return this.associateToParallelly(UInt64.ten, destination, extractor)
}

@JvmName("tryAssociateToParallelly")
suspend inline fun <K, V, T, M : MutableMap<in K, in V>> Iterable<T>.associateToParallelly(
    segment: UInt64,
    destination: M,
    crossinline extractor: TryExtractor<Pair<K, V>, T>
): Ret<M> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<Pair<K, V>>>>()
            val iterator = this@associateToParallelly.iterator()
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

            Ok(promises.flatMap { it.await() }.toMap(destination))
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(destination)
    }
}

suspend inline fun <K, V, T, M : MutableMap<in K, in V>> Collection<T>.associateToParallelly(
    destination: M,
    crossinline extractor: Extractor<Pair<K, V>, T>
): M {
    return (this as Iterable<T>).associateToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <K, V, T, M : MutableMap<in K, in V>> Collection<T>.associateToParallelly(
    concurrentAmount: UInt64,
    destination: M,
    crossinline extractor: Extractor<Pair<K, V>, T>
): M {
    return (this as Iterable<T>).associateToParallelly(UInt64(this.size) / concurrentAmount, destination, extractor)
}

@JvmName("tryAssociateToParallelly")
suspend inline fun <K, V, T, M : MutableMap<in K, in V>> Collection<T>.associateToParallelly(
    destination: M,
    crossinline extractor: TryExtractor<Pair<K, V>, T>
): Ret<M> {
    return (this as Iterable<T>).associateToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

@JvmName("tryAssociateToParallelly")
suspend inline fun <K, V, T, M : MutableMap<in K, in V>> Collection<T>.associateToParallelly(
    concurrentAmount: UInt64,
    destination: M,
    crossinline extractor: TryExtractor<Pair<K, V>, T>
): Ret<M> {
    return (this as Iterable<T>).associateToParallelly(UInt64(this.size) / concurrentAmount, destination, extractor)
}

suspend inline fun <K, V, T, M : MutableMap<in K, in V>> List<T>.associateToParallelly(
    destination: M,
    crossinline extractor: Extractor<Pair<K, V>, T>
): M {
    return this.associateToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <K, V, T, M : MutableMap<in K, in V>> List<T>.associateToParallelly(
    concurrentAmount: UInt64,
    destination: M,
    crossinline extractor: Extractor<Pair<K, V>, T>
): M {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<Pair<K, V>>>>()
        val segmentAmount = this@associateToParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@associateToParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@associateToParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@associateToParallelly.subList(j, k).map(extractor)
            })
            i = k
        }

        promises.flatMap { it.await() }.toMap(destination)
    }
}

@JvmName("tryAssociateToParallelly")
suspend inline fun <K, V, T, M : MutableMap<in K, in V>> List<T>.associateToParallelly(
    destination: M,
    crossinline extractor: TryExtractor<Pair<K, V>, T>
): Ret<M> {
    return this.associateToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

@JvmName("tryAssociateToParallelly")
suspend inline fun <K, V, T, M : MutableMap<in K, in V>> List<T>.associateToParallelly(
    concurrentAmount: UInt64,
    destination: M,
    crossinline extractor: TryExtractor<Pair<K, V>, T>
): Ret<M> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<Pair<K, V>>>>()
            val segmentAmount = this@associateToParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@associateToParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@associateToParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@associateToParallelly.subList(j, k).mapNotNull {
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

            Ok(promises.flatMap { it.await() }.toMap(destination))
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(destination)
    }
}

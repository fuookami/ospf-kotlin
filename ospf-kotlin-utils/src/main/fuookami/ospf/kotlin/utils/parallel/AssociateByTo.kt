package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <K, T, M : MutableMap<in K, in T>> Iterable<T>.associateByToParallelly(
    destination: M,
    crossinline extractor: SuspendExtractor<K, T>
): M {
    return this.associateByToParallelly(UInt64.ten, destination, extractor)
}

suspend inline fun <K, T, M : MutableMap<in K, in T>> Iterable<T>.associateByToParallelly(
    segment: UInt64,
    destination: M,
    crossinline extractor: SuspendExtractor<K, T>
): M {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<Pair<K, T>>>>()
        val iterator = this@associateByToParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { Pair(extractor(it), it) }
            })
        }

        promises.flatMap { it.await() }.toMap(destination)
    }
}

suspend inline fun <K, T, M : MutableMap<in K, in T>> Iterable<T>.tryAssociateByToParallelly(
    destination: M,
    crossinline extractor: SuspendTryExtractor<K, T>
): Ret<M> {
    return this.tryAssociateByToParallelly(UInt64.ten, destination, extractor)
}

suspend inline fun <K, T, M : MutableMap<in K, in T>> Iterable<T>.tryAssociateByToParallelly(
    segment: UInt64,
    destination: M,
    crossinline extractor: SuspendTryExtractor<K, T>
): Ret<M> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<Pair<K, T>>>>()
            val iterator = this@tryAssociateByToParallelly.iterator()
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
                                Pair(result.value, it)
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

suspend inline fun <K, T, M : MutableMap<in K, in T>> Collection<T>.associateByToParallelly(
    destination: M,
    crossinline extractor: SuspendExtractor<K, T>
): M {
    return (this as Iterable<T>).associateByToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <K, T, M : MutableMap<in K, in T>> Collection<T>.associateByToParallelly(
    concurrentAmount: UInt64,
    destination: M,
    crossinline extractor: SuspendExtractor<K, T>
): M {
    return (this as Iterable<T>).associateByToParallelly(this.usize / concurrentAmount, destination, extractor)
}

suspend inline fun <K, T, M : MutableMap<in K, in T>> Collection<T>.tryAssociateByToParallelly(
    destination: M,
    crossinline extractor: SuspendTryExtractor<K, T>
): Ret<M> {
    return (this as Iterable<T>).tryAssociateByToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <K, T, M : MutableMap<in K, in T>> Collection<T>.tryAssociateByToParallelly(
    concurrentAmount: UInt64,
    destination: M,
    crossinline extractor: SuspendTryExtractor<K, T>
): Ret<M> {
    return (this as Iterable<T>).tryAssociateByToParallelly(this.usize / concurrentAmount, destination, extractor)
}

suspend inline fun <K, T, M : MutableMap<in K, in T>> List<T>.associateByToParallelly(
    destination: M,
    crossinline extractor: SuspendExtractor<K, T>
): M {
    return this.associateByToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <K, T, M : MutableMap<in K, in T>> List<T>.associateByToParallelly(
    concurrentAmount: UInt64,
    destination: M,
    crossinline extractor: SuspendExtractor<K, T>
): M {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<Pair<K, T>>>>()
        val segmentAmount = this@associateByToParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@associateByToParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@associateByToParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@associateByToParallelly.subList(j, k).map { Pair(extractor(it), it) }
            })
            i = k
        }

        promises.flatMap { it.await() }.toMap(destination)
    }
}

suspend inline fun <K, T, M : MutableMap<in K, in T>> List<T>.tryAssociateByToParallelly(
    destination: M,
    crossinline extractor: SuspendTryExtractor<K, T>
): Ret<M> {
    return this.tryAssociateByToParallelly(
        defaultConcurrentAmount,
        destination,
        extractor
    )
}

suspend inline fun <K, T, M : MutableMap<in K, in T>> List<T>.tryAssociateByToParallelly(
    concurrentAmount: UInt64,
    destination: M,
    crossinline extractor: SuspendTryExtractor<K, T>
): Ret<M> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<List<Pair<K, T>>>>()
            val segmentAmount = this@tryAssociateByToParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryAssociateByToParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryAssociateByToParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryAssociateByToParallelly.subList(j, k).mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                Pair(result.value, it)
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

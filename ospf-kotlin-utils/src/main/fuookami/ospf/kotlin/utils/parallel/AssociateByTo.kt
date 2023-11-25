package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <K, T, M : MutableMap<in K, in T>> Iterable<T>.associateByToParallelly(destination: M, crossinline extractor: Extractor<K, T>): M {
    return this.associateByToParallelly(UInt64.ten, destination, extractor)
}

suspend inline fun <K, T, M : MutableMap<in K, in T>> Iterable<T>.associateByToParallelly(segment: UInt64, destination: M, crossinline extractor: Extractor<K, T>): M {
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

suspend inline fun <K, T, M : MutableMap<in K, in T>> Collection<T>.associateByToParallelly(destination: M, crossinline extractor: Extractor<K, T>): M {
    return this.associateByToParallelly(
        UInt64(
            maxOf(
                minOf(
                    Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                    Runtime.getRuntime().availableProcessors()
                ),
                1
            )
        ),
        destination,
        extractor
    )
}

suspend inline fun <K, T, M : MutableMap<in K, in T>> Collection<T>.associateByToParallelly(concurrentAmount: UInt64, destination: M, crossinline extractor: Extractor<K, T>): M {
    return (this as Iterable<T>).associateByToParallelly(UInt64(this.size) / concurrentAmount, destination, extractor)
}

suspend inline fun <K, T, M : MutableMap<in K, in T>> List<T>.associateByToParallelly(destination: M, crossinline extractor: Extractor<K, T>): M {
    return this.associateByToParallelly(
        UInt64(
            maxOf(
                minOf(
                    Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                    Runtime.getRuntime().availableProcessors()
                ),
                1
            )
        ),
        destination,
        extractor
    )
}

suspend inline fun <K, T, M : MutableMap<in K, in T>> List<T>.associateByToParallelly(concurrentAmount: UInt64, destination: M, crossinline extractor: Extractor<K, T>): M {
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

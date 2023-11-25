package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, V> Iterable<T>.associateWithParallelly(crossinline extractor: Extractor<V, T>): Map<T, V> {
    return this.associateWithParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, V> Iterable<T>.associateWithParallelly(segment: UInt64, crossinline extractor: Extractor<V, T>): Map<T, V> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<Pair<T, V>>>>()
        val iterator = this@associateWithParallelly.iterator()
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

        promises.flatMap { it.await() }.toMap()
    }
}

suspend inline fun <T, V> Collection<T>.associateWithParallelly(crossinline extractor: Extractor<V, T>): Map<T, V> {
    return this.associateWithParallelly(
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

suspend inline fun <T, V> Collection<T>.associateWithParallelly(concurrentAmount: UInt64, crossinline extractor: Extractor<V, T>): Map<T, V> {
    return (this as Iterable<T>).associateWithParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <T, V> List<T>.associateWithParallelly(crossinline extractor: Extractor<V, T>): Map<T, V> {
    return this.associateWithParallelly(
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

suspend inline fun <T, V> List<T>.associateWithParallelly(concurrentAmount: UInt64, crossinline extractor: Extractor<V, T>): Map<T, V> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<Pair<T, V>>>>()
        val segmentAmount = this@associateWithParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@associateWithParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@associateWithParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@associateWithParallelly.subList(j, k).map { Pair(it, extractor(it)) }
            })
            i = k
        }

        promises.flatMap { it.await() }.toMap()
    }
}

package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, R: Comparable<R>> Iterable<T>.minOfOrNullParallelly(crossinline extractor: Extractor<R, T>): R? {
    return this.minOfOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R: Comparable<R>> Iterable<T>.minOfOrNullParallelly(segment: UInt64, crossinline extractor: Extractor<R, T>): R? {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val iterator = this@minOfOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<R>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(extractor(iterator.next()))
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.maxOrNull()
            })
        }

        promises.mapNotNull { it.await() }.maxOrNull()
    }
}

suspend inline fun <T, R: Comparable<R>> Collection<T>.minOfOrNullParallelly(crossinline extractor: Extractor<R, T>): R? {
    return (this as Iterable<T>).minOfOrNullParallelly(
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

suspend inline fun <T, R: Comparable<R>> Collection<T>.minOfOrNullParallelly(concurrentAmount: UInt64, crossinline extractor: Extractor<R, T>): R? {
    return (this as Iterable<T>).minOfOrNullParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <T, R: Comparable<R>> List<T>.minOfOrNullParallelly(crossinline extractor: Extractor<R, T>): R? {
    return this.minOfOrNullParallelly(
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

suspend inline fun <T, R: Comparable<R>> List<T>.minOfOrNullParallelly(concurrentAmount: UInt64, crossinline extractor: Extractor<R, T>): R? {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val segmentAmount = this@minOfOrNullParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@minOfOrNullParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@minOfOrNullParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@minOfOrNullParallelly.subList(j, k).maxOfOrNull { extractor(it) }
            })
            i = k
        }

        promises.mapNotNull { it.await() }.maxOrNull()
    }
}

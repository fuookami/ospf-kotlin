package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, R: Comparable<R>> Iterable<T>.maxByOrNullParallelly(crossinline extractor: Extractor<R, T>): T? {
    return this.maxByOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R: Comparable<R>> Iterable<T>.maxByOrNullParallelly(segment: UInt64, crossinline extractor: Extractor<R, T>): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, R>?>>()
        val iterator = this@maxByOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<Pair<T, R>>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                val v = iterator.next()
                thisSegment.add(Pair(v, extractor(v)))
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.maxByOrNull { it.second }
            })
        }

        promises.mapNotNull { it.await() }.maxByOrNull { it.second }?.first
    }
}

suspend inline fun <T, R: Comparable<R>> Collection<T>.maxByOrNullParallelly(crossinline extractor: Extractor<R, T>): T? {
    return (this as Iterable<T>).maxByOrNullParallelly(
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

suspend inline fun <T, R: Comparable<R>> Collection<T>.maxByOrNullParallelly(concurrentAmount: UInt64, crossinline extractor: Extractor<R, T>): T? {
    return (this as Iterable<T>).maxByOrNullParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <T, R: Comparable<R>> List<T>.maxByOrNullParallelly(crossinline extractor: Extractor<R, T>): T? {
    return this.maxByOrNullParallelly(
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

suspend inline fun <T, R: Comparable<R>> List<T>.maxByOrNullParallelly(concurrentAmount: UInt64, crossinline extractor: Extractor<R, T>): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, R>?>>()
        val segmentAmount = this@maxByOrNullParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@maxByOrNullParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@maxByOrNullParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@maxByOrNullParallelly.subList(j, k).map { Pair(it, extractor(it)) }.maxByOrNull { it.second }
            })
            i = k
        }

        promises.mapNotNull { it.await() }.maxByOrNull { it.second }?.first
    }
}

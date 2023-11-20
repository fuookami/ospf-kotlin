package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, R: Comparable<R>> Iterable<T>.maxByParallelly(crossinline extractor: Extractor<R, T>): T {
    return this.maxByParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R: Comparable<R>> Iterable<T>.maxByParallelly(segment: UInt64, crossinline extractor: Extractor<R, T>): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, R>?>>()
        val iterator = this@maxByParallelly.iterator()
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
    } ?: throw NoSuchElementException()
}

suspend inline fun <T, R: Comparable<R>> Collection<T>.maxByParallelly(crossinline extractor: Extractor<R, T>): T {
    return (this as Iterable<T>).maxByParallelly(
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

suspend inline fun <T, R: Comparable<R>> Collection<T>.maxByParallelly(concurrentAmount: UInt64, crossinline extractor: Extractor<R, T>): T {
    return (this as Iterable<T>).maxByParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <T, R: Comparable<R>> List<T>.maxByParallelly(crossinline extractor: Extractor<R, T>): T {
    return this.maxByParallelly(
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

suspend inline fun <T, R: Comparable<R>> List<T>.maxByParallelly(concurrentAmount: UInt64, crossinline extractor: Extractor<R, T>): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, R>?>>()
        val segmentAmount = this@maxByParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@maxByParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@maxByParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@maxByParallelly.subList(j, k).map { Pair(it, extractor(it)) }.maxByOrNull { it.second }
            })
            i = k
        }

        promises.mapNotNull { it.await() }.maxByOrNull { it.second }?.first
    } ?: throw NoSuchElementException()
}

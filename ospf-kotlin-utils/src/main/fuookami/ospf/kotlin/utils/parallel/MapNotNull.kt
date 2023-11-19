package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <R, T> Iterable<T>.mapNotNullParallelly(crossinline extractor: Extractor<R?, T>): List<R> {
    return this.mapNotNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <R, T> Iterable<T>.mapNotNullParallelly(segment: UInt64, crossinline extractor: Extractor<R?, T>): List<R> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val iterator = this@mapNotNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.mapNotNull(extractor)
            })
        }
        promises.flatMap { it.await() }
    }
}

suspend inline fun <R, T> Collection<T>.mapNotNullParallelly(crossinline extractor: Extractor<R?, T>): List<R> {
    return this.mapNotNullParallelly(
        UInt64(
            minOf(
                Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                Runtime.getRuntime().availableProcessors()
            )
        ),
        extractor
    )
}

suspend inline fun <R, T> Collection<T>.mapNotNullParallelly(concurrentAmount: UInt64, crossinline extractor: Extractor<R?, T>): List<R> {
    return (this as Iterable<T>).mapNotNullParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <R, T> List<T>.mapNotNullParallelly(crossinline extractor: Extractor<R?, T>): List<R> {
    return this.mapNotNullParallelly(
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

suspend inline fun <R, T> List<T>.mapNotNullParallelly(concurrentAmount: UInt64, crossinline extractor: Extractor<R?, T>): List<R> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val segmentAmount = this@mapNotNullParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@mapNotNullParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@mapNotNullParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@mapNotNullParallelly.subList(j, k).mapNotNull(extractor)
            })
            i = k
        }
        promises.flatMap { it.await() }
    }
}

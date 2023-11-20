package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <R, T> Iterable<T>.flatMapParallelly(crossinline extractor: Extractor<Iterable<R>, T>): List<R> {
    return this.flatMapParallelly(UInt64.ten, extractor)
}

suspend inline fun <R, T> Iterable<T>.flatMapParallelly(segment: UInt64, crossinline extractor: Extractor<Iterable<R>, T>): List<R> {
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

suspend inline fun <R, T> Collection<T>.flatMapParallelly(crossinline extractor: Extractor<Iterable<R>, T>): List<R> {
    return this.flatMapParallelly(
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

suspend inline fun <R, T> Collection<T>.flatMapParallelly(concurrentAmount: UInt64, crossinline extractor: Extractor<Iterable<R>, T>): List<R> {
    return (this as Iterable<T>).flatMapParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <R, T> List<T>.flatMapParallelly(crossinline extractor: Extractor<Iterable<R>, T>): List<R> {
    return this.flatMapParallelly(
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

suspend inline fun <R, T> List<T>.flatMapParallelly(concurrentAmount: UInt64, crossinline extractor: Extractor<Iterable<R>, T>): List<R> {
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

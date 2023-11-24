package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <R, T> Iterable<T>.flatMapIndexedParallelly(crossinline extractor: IndexedExtractor<Iterable<R>, T>): List<R> {
    return this.flatMapIndexedParallelly(UInt64.ten, extractor)
}

suspend inline fun <R, T> Iterable<T>.flatMapIndexedParallelly(segment: UInt64, crossinline extractor: IndexedExtractor<Iterable<R>, T>): List<R> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val iterator = this@flatMapIndexedParallelly.iterator()
        var i = 0
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<Pair<Int, T>>()
            var j = UInt64.zero
            while (iterator.hasNext() && j != segment) {
                thisSegment.add(Pair(i, iterator.next()))
                ++i
                ++j
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.flatMap { extractor(it.first, it.second) }
            })
        }

        promises.flatMap { it.await() }
    }
}

suspend inline fun <R, T> Collection<T>.flatMapIndexedParallelly(crossinline extractor: IndexedExtractor<Iterable<R>, T>): List<R> {
    return (this as Iterable<T>).flatMapIndexedParallelly(
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

suspend inline fun <R, T> Collection<T>.flatMapIndexedParallelly(concurrentAmount: UInt64, crossinline extractor: IndexedExtractor<Iterable<R>, T>): List<R> {
    return (this as Iterable<T>).flatMapIndexedParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <R, T> List<T>.flatMapIndexedParallelly(crossinline extractor: IndexedExtractor<Iterable<R>, T>): List<R> {
    return this.flatMapIndexedParallelly(
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

suspend inline fun <R, T> List<T>.flatMapIndexedParallelly(concurrentAmount: UInt64, crossinline extractor: IndexedExtractor<Iterable<R>, T>): List<R> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val segmentAmount = this@flatMapIndexedParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@flatMapIndexedParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@flatMapIndexedParallelly.size - j
            )
            promises.add(async(Dispatchers.Default) {
                this@flatMapIndexedParallelly.flatMapIndexed { i, v -> extractor(i + j, v) }
            })
            i = k
        }

        promises.flatMap { it.await() }
    }
}

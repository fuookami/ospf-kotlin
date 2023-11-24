package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <R, T, C: MutableCollection<in R>> Iterable<T>.flatMapIndexedToParallelly(destination: C, crossinline extractor: IndexedExtractor<Iterable<R>, T>): C {
    return this.flatMapIndexedToParallelly(UInt64.ten, destination, extractor)
}

suspend inline fun <R, T, C: MutableCollection<in R>> Iterable<T>.flatMapIndexedToParallelly(segment: UInt64, destination: C, crossinline extractor: IndexedExtractor<Iterable<R>, T>): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val iterator = this@flatMapIndexedToParallelly.iterator()
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

        promises.flatMapTo(destination) { it.await() }
    }
}

suspend inline fun <R, T, C: MutableCollection<in R>> Collection<T>.flatMapIndexedToParallelly(destination: C, crossinline extractor: IndexedExtractor<Iterable<R>, T>): C {
    return (this as Iterable<T>).flatMapIndexedToParallelly(
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

suspend inline fun <R, T, C: MutableCollection<in R>> Collection<T>.flatMapIndexedToParallelly(concurrentAmount: UInt64, destination: C, crossinline extractor: IndexedExtractor<Iterable<R>, T>): C {
    return (this as Iterable<T>).flatMapIndexedToParallelly(UInt64(this.size) / concurrentAmount, destination, extractor)
}

suspend inline fun <R, T, C: MutableCollection<in R>> List<T>.flatMapIndexedToParallelly(destination: C, crossinline extractor: IndexedExtractor<Iterable<R>, T>): C {
    return this.flatMapIndexedToParallelly(
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

suspend inline fun <R, T, C: MutableCollection<in R>> List<T>.flatMapIndexedToParallelly(concurrentAmount: UInt64, destination: C, crossinline extractor: IndexedExtractor<Iterable<R>, T>): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val segmentAmount = this@flatMapIndexedToParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@flatMapIndexedToParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@flatMapIndexedToParallelly.size - j
            )
            promises.add(async(Dispatchers.Default) {
                this@flatMapIndexedToParallelly.flatMapIndexed { i, v -> extractor(i + j, v) }
            })
            i = k
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

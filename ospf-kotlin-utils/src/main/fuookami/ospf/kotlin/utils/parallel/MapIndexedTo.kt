package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.mapIndexedToParallelly(
    destination: C,
    crossinline extractor: IndexedExtractor<R, T>
): C {
    return this.mapIndexedToParallelly(UInt64.ten, destination, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.mapIndexedToParallelly(
    segment: UInt64,
    destination: C,
    crossinline extractor: IndexedExtractor<R, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val iterator = this@mapIndexedToParallelly.iterator()
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
                thisSegment.map { extractor(it.first, it.second) }
            })
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Collection<T>.mapIndexedToParallelly(
    destination: C,
    crossinline extractor: IndexedExtractor<R, T>
): C {
    return (this as Iterable<T>).mapIndexedToParallelly(
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

suspend inline fun <R, T, C : MutableCollection<in R>> Collection<T>.mapIndexedToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline extractor: IndexedExtractor<R, T>
): C {
    return (this as Iterable<T>).mapIndexedToParallelly(UInt64(this.size) / concurrentAmount, destination, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> List<T>.mapIndexedToParallelly(
    destination: C,
    crossinline extractor: IndexedExtractor<R, T>
): C {
    return this.mapIndexedToParallelly(
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

suspend inline fun <R, T, C : MutableCollection<in R>> List<T>.mapIndexedToParallelly(
    concurrentAmount: UInt64,
    destination: C,
    crossinline extractor: IndexedExtractor<R, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val segmentAmount = this@mapIndexedToParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@mapIndexedToParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@mapIndexedToParallelly.size - j
            )
            promises.add(async(Dispatchers.Default) {
                this@mapIndexedToParallelly.mapIndexed { i, v -> extractor(i + j, v) }
            })
            i = k
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <R, T, C: MutableCollection<in R>> Iterable<T>.mapToParallelly(destination: C, crossinline extractor: Extractor<R, T>): C {
    return this.mapToParallelly(UInt64.ten, destination, extractor)
}

suspend inline fun <R, T, C: MutableCollection<in R>> Iterable<T>.mapToParallelly(segment: UInt64, destination: C, crossinline extractor: Extractor<R, T>): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val iterator = this@mapToParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map(extractor)
            })
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

suspend inline fun <R, T, C: MutableCollection<in R>> Collection<T>.mapToParallelly(destination: C, crossinline extractor: Extractor<R, T>): C {
    return this.mapToParallelly(
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

suspend inline fun <R, T, C: MutableCollection<in R>> Collection<T>.mapToParallelly(concurrentAmount: UInt64, destination: C, crossinline extractor: Extractor<R, T>): C {
    return (this as Iterable<T>).mapToParallelly(UInt64(this.size) / concurrentAmount, destination, extractor)
}

suspend inline fun <R, T, C: MutableCollection<in R>> List<T>.mapToParallelly(destination: C, crossinline extractor: Extractor<R, T>): C {
    return this.mapToParallelly(
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

suspend inline fun <R, T, C: MutableCollection<in R>> List<T>.mapToParallelly(concurrentAmount: UInt64, destination: C, crossinline extractor: Extractor<R, T>): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<R>>>()
        val segmentAmount = this@mapToParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@mapToParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@mapToParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@mapToParallelly.subList(j, k).map(extractor)
            })
            i = k
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

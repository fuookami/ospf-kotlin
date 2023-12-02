package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxOfParallelly(crossinline extractor: Extractor<R, T>): R {
    return this.maxOfParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxOfParallelly(
    segment: UInt64,
    crossinline extractor: Extractor<R, T>
): R {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val iterator = this@maxOfParallelly.iterator()
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
    } ?: throw NoSuchElementException()
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.maxOfParallelly(crossinline extractor: Extractor<R, T>): R {
    return (this as Iterable<T>).maxOfParallelly(
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

suspend inline fun <T, R : Comparable<R>> Collection<T>.maxOfParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<R, T>
): R {
    return (this as Iterable<T>).maxOfParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <T, R : Comparable<R>> List<T>.maxOfParallelly(crossinline extractor: Extractor<R, T>): R {
    return this.maxOfParallelly(
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

suspend inline fun <T, R : Comparable<R>> List<T>.maxOfParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<R, T>
): R {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val segmentAmount = this@maxOfParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@maxOfParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@maxOfParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@maxOfParallelly.subList(j, k).maxOfOrNull { extractor(it) }
            })
            i = k
        }

        promises.mapNotNull { it.await() }.maxOrNull()
    } ?: throw NoSuchElementException()
}

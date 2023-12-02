package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*

suspend inline fun <T : Comparable<T>> Iterable<T>.minParallelly(): T {
    return this.minParallelly(UInt64.ten)
}

suspend inline fun <T : Comparable<T>> Iterable<T>.minParallelly(segment: UInt64): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        val iterator = this@minParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minOrNull()
            })
        }

        promises.mapNotNull { it.await() }.minOrNull()
    } ?: throw NoSuchElementException()
}

suspend inline fun <T : Comparable<T>> Collection<T>.minParallelly(): T {
    return (this as Iterable<T>).minParallelly(
        UInt64(
            maxOf(
                minOf(
                    Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                    Runtime.getRuntime().availableProcessors()
                ),
                1
            )
        )
    )
}

suspend inline fun <T : Comparable<T>> Collection<T>.minParallelly(concurrentAmount: UInt64): T {
    return (this as Iterable<T>).minParallelly(UInt64(this.size) / concurrentAmount)
}

suspend inline fun <T : Comparable<T>> List<T>.minParallelly(): T {
    return this.minParallelly(
        UInt64(
            maxOf(
                minOf(
                    Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                    Runtime.getRuntime().availableProcessors()
                ),
                1
            )
        )
    )
}

suspend inline fun <T : Comparable<T>> List<T>.minParallelly(concurrentAmount: UInt64): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        val segmentAmount = this@minParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@minParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@minParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@minParallelly.subList(j, k).minOrNull()
            })
            i = k
        }

        promises.mapNotNull { it.await() }.minOrNull()
    } ?: throw NoSuchElementException()
}

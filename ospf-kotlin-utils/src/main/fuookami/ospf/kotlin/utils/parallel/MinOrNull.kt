package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*

suspend inline fun <T : Comparable<T>> Iterable<T>.minOrNullParallelly(): T? {
    return this.minOrNullParallelly(UInt64.ten)
}

suspend inline fun <T : Comparable<T>> Iterable<T>.minOrNullParallelly(segment: UInt64): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        val iterator = this@minOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.maxOrNull()
            })
        }

        promises.mapNotNull { it.await() }.maxOrNull()
    }
}

suspend inline fun <T : Comparable<T>> Collection<T>.minOrNullParallelly(): T? {
    return (this as Iterable<T>).minOrNullParallelly(
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

suspend inline fun <T : Comparable<T>> Collection<T>.minOrNullParallelly(concurrentAmount: UInt64): T? {
    return (this as Iterable<T>).minOrNullParallelly(UInt64(this.size) / concurrentAmount)
}

suspend inline fun <T : Comparable<T>> List<T>.minOrNullParallelly(): T? {
    return this.minOrNullParallelly(
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

suspend inline fun <T : Comparable<T>> List<T>.minOrNullParallelly(concurrentAmount: UInt64): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        val segmentAmount = this@minOrNullParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@minOrNullParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@minOrNullParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@minOrNullParallelly.subList(j, k).maxOrNull()
            })
            i = k
        }

        promises.mapNotNull { it.await() }.maxOrNull()
    }
}

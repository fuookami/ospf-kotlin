package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*

suspend inline fun <T : Comparable<T>> Iterable<T>.maxOrNullParallelly(): T? {
    return this.maxOrNullParallelly(UInt64.ten)
}

suspend inline fun <T : Comparable<T>> Iterable<T>.maxOrNullParallelly(
    segment: UInt64
): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        val iterator = this@maxOrNullParallelly.iterator()
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

suspend inline fun <T : Comparable<T>> Collection<T>.maxOrNullParallelly(): T? {
    return (this as Iterable<T>).maxOrNullParallelly(
        defaultConcurrentAmount
    )
}

suspend inline fun <T : Comparable<T>> Collection<T>.maxOrNullParallelly(
    concurrentAmount: UInt64
): T? {
    return (this as Iterable<T>).maxOrNullParallelly(this.usize / concurrentAmount)
}

suspend inline fun <T : Comparable<T>> List<T>.maxOrNullParallelly(): T? {
    return this.maxOrNullParallelly(
        defaultConcurrentAmount
    )
}

suspend inline fun <T : Comparable<T>> List<T>.maxOrNullParallelly(
    concurrentAmount: UInt64
): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        val segmentAmount = this@maxOrNullParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@maxOrNullParallelly.size) {
            val j = i
            val k = i + maxOf(
                segmentAmount,
                this@maxOrNullParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@maxOrNullParallelly.subList(j, k).maxOrNull()
            })
            i = k
        }

        promises.mapNotNull { it.await() }.maxOrNull()
    }
}

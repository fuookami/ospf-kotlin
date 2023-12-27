package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*

suspend inline fun <T : Comparable<T>> Iterable<T>.maxParallelly(): T {
    return this.maxParallelly(UInt64.ten)
}

suspend inline fun <T : Comparable<T>> Iterable<T>.maxParallelly(
    segment: UInt64
): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        val iterator = this@maxParallelly.iterator()
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
    } ?: throw NoSuchElementException()
}

suspend inline fun <T : Comparable<T>> Collection<T>.maxParallelly(): T {
    return (this as Iterable<T>).maxParallelly(
        defaultConcurrentAmount
    )
}

suspend inline fun <T : Comparable<T>> Collection<T>.maxParallelly(
    concurrentAmount: UInt64
): T {
    return (this as Iterable<T>).maxParallelly(UInt64(this.size) / concurrentAmount)
}

suspend inline fun <T : Comparable<T>> List<T>.maxParallelly(): T {
    return this.maxParallelly(
        defaultConcurrentAmount
    )
}

suspend inline fun <T : Comparable<T>> List<T>.maxParallelly(
    concurrentAmount: UInt64
): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        val segmentAmount = this@maxParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@maxParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@maxParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@maxParallelly.subList(j, k).maxOrNull()
            })
            i = k
        }

        promises.mapNotNull { it.await() }.maxOrNull()
    } ?: throw NoSuchElementException()
}

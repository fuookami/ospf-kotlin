package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T?>.filterNotNullParallelly(): List<T> {
    return this.filterNotNullParallelly(UInt64.ten)
}

suspend inline fun <T> Iterable<T?>.filterNotNullParallelly(
    segment: UInt64
): List<T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val iterator = this@filterNotNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T?>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.filterNotNull()
            })
        }

        promises.flatMap { it.await() }
    }
}

suspend inline fun <T> Collection<T?>.filterNotNullParallelly(): List<T> {
    return this.filterNotNullParallelly(
        defaultConcurrentAmount
    )
}

suspend inline fun <T> Collection<T?>.filterNotNullParallelly(
    concurrentAmount: UInt64
): List<T> {
    return (this as Iterable<T?>).filterNotNullParallelly(this.usize / concurrentAmount)
}

suspend inline fun <T> List<T?>.filterNotNullParallelly(): List<T> {
    return this.filterNotNullParallelly(
        defaultConcurrentAmount
    )
}

suspend inline fun <T> List<T?>.filterNotNullParallelly(
    concurrentAmount: UInt64
): List<T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val segmentAmount = this@filterNotNullParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@filterNotNullParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@filterNotNullParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@filterNotNullParallelly.subList(j, k).filterNotNull()
            })
            i = k
        }

        promises.flatMap { it.await() }
    }
}

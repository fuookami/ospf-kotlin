package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*

suspend inline fun <reified T> Iterable<*>.filterIsInstanceParallelly(): List<T> {
    return this.filterIsInstanceParallelly<T>(UInt64.ten)
}

suspend inline fun <reified T> Iterable<*>.filterIsInstanceParallelly(
    segment: UInt64
): List<T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val iterator = this@filterIsInstanceParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<Any?>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.filterIsInstance<T>()
            })
        }

        promises.flatMap { it.await() }
    }
}

suspend inline fun <reified T> Collection<*>.filterIsInstanceParallelly(): List<T> {
    return this.filterIsInstanceParallelly<T>(
        defaultConcurrentAmount
    )
}

suspend inline fun <reified T> Collection<*>.filterIsInstanceParallelly(
    concurrentAmount: UInt64
): List<T> {
    return (this as Iterable<*>).filterIsInstanceParallelly<T>(UInt64(this.size) / concurrentAmount)
}

suspend inline fun <reified T> List<*>.filterIsInstanceParallelly(): List<T> {
    return this.filterIsInstanceParallelly<T>(
        defaultConcurrentAmount
    )
}

suspend inline fun <reified T> List<*>.filterIsInstanceParallelly(
    concurrentAmount: UInt64
): List<T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val segmentAmount = this@filterIsInstanceParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@filterIsInstanceParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@filterIsInstanceParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@filterIsInstanceParallelly.subList(j, k).filterIsInstance<T>()
            })
            i = k
        }

        promises.flatMap { it.await() }
    }
}

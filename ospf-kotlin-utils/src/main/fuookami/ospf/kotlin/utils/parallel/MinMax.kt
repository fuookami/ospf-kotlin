package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T : Comparable<T>> Iterable<T>.minMaxParallelly(): Pair<T, T> {
    return this.minMaxParallelly(UInt64.ten)
}

suspend inline fun <T : Comparable<T>> Iterable<T>.minMaxParallelly(
    segment: UInt64
): Pair<T, T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, T>?>>()
        val iterator = this@minMaxParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minMaxOrNull()
            })
        }

        val segmentResults = promises.mapNotNull { it.await() }
        val minPromise = async(Dispatchers.Default) {
            segmentResults.minOfOrNull { it.first }
        }
        val maxPromise = async(Dispatchers.Default) {
            segmentResults.maxOfOrNull { it.second }
        }

        val min = when (val min = minPromise.await()) {
            null -> {
                return@coroutineScope null
            }

            else -> {
                min
            }
        }

        val max = when (val max = maxPromise.await()) {
            null -> {
                return@coroutineScope null
            }

            else -> {
                max
            }
        }

        Pair(min, max)
    } ?: throw NoSuchElementException()
}

suspend inline fun <T : Comparable<T>> Collection<T>.minMaxParallelly(): Pair<T, T> {
    return (this as Iterable<T>).minMaxParallelly(
        defaultConcurrentAmount
    )
}

suspend inline fun <T : Comparable<T>> Collection<T>.minMaxParallelly(
    concurrentAmount: UInt64
): Pair<T, T> {
    return (this as Iterable<T>).minMaxParallelly(UInt64(this.size) / concurrentAmount)
}

suspend inline fun <T : Comparable<T>> List<T>.minMaxParallelly(): Pair<T, T> {
    return this.minMaxParallelly(
        defaultConcurrentAmount
    )
}

suspend inline fun <T : Comparable<T>> List<T>.minMaxParallelly(
    concurrentAmount: UInt64
): Pair<T, T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, T>?>>()
        val segmentAmount = this@minMaxParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@minMaxParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@minMaxParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@minMaxParallelly.subList(j, k).minMaxOrNull()
            })
            i = k
        }

        val segmentResults = promises.mapNotNull { it.await() }
        val minPromise = async(Dispatchers.Default) {
            segmentResults.minOfOrNull { it.first }
        }
        val maxPromise = async(Dispatchers.Default) {
            segmentResults.maxOfOrNull { it.second }
        }

        val min = when (val min = minPromise.await()) {
            null -> {
                return@coroutineScope null
            }

            else -> {
                min
            }
        }

        val max = when (val max = maxPromise.await()) {
            null -> {
                return@coroutineScope null
            }

            else -> {
                max
            }
        }

        Pair(min, max)
    } ?: throw NoSuchElementException()
}

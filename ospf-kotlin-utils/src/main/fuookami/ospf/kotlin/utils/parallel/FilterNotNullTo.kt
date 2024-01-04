package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*

suspend inline fun <T, C : MutableCollection<in T>> Iterable<T>.filterNotNullToParallelly(
    destination: C
): C {
    return this.filterNotNullToParallelly(UInt64.ten, destination)
}

suspend inline fun <T, C : MutableCollection<in T>> Iterable<T>.filterNotNullToParallelly(
    segment: UInt64,
    destination: C
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val iterator = this@filterNotNullToParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.filterNotNull()
            })
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

suspend inline fun <T, C : MutableCollection<in T>> Collection<T>.filterNotNullToParallelly(
    destination: C
): C {
    return (this as Iterable<T>).filterNotNullToParallelly(
        defaultConcurrentAmount,
        destination
    )
}

suspend inline fun <T, C : MutableCollection<in T>> Collection<T>.filterNotNullToParallelly(
    concurrentAmount: UInt64,
    destination: C
): C {
    return (this as Iterable<T>).filterNotNullToParallelly(this.usize / concurrentAmount, destination)
}

suspend inline fun <T, C : MutableCollection<in T>> List<T>.filterNotNullToParallelly(
    destination: C
): C {
    return this.filterNotNullToParallelly(
        defaultConcurrentAmount,
        destination
    )
}

suspend inline fun <T, C : MutableCollection<in T>> List<T>.filterNotNullToParallelly(
    concurrentAmount: UInt64,
    destination: C
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val segmentAmount = this@filterNotNullToParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@filterNotNullToParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@filterNotNullToParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@filterNotNullToParallelly.subList(j, k).filterNotNull()
            })
            i = k
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

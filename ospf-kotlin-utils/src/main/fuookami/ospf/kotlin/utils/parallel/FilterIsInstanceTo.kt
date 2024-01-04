package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <reified T, C : MutableCollection<in T>> Iterable<*>.filterIsInstanceToParallelly(
    destination: C
): C {
    return this.filterIsInstanceToParallelly(UInt64.ten, destination)
}

suspend inline fun <reified T, C : MutableCollection<in T>> Iterable<*>.filterIsInstanceToParallelly(
    segment: UInt64,
    destination: C
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val iterator = this@filterIsInstanceToParallelly.iterator()
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

        promises.flatMapTo(destination) { it.await() }
    }
}

suspend inline fun <reified T, C : MutableCollection<in T>> Collection<*>.filterIsInstanceToParallelly(
    destination: C
): C {
    return (this as Iterable<*>).filterIsInstanceToParallelly(
        defaultConcurrentAmount,
        destination
    )
}

suspend inline fun <reified T, C : MutableCollection<in T>> Collection<*>.filterIsInstanceToParallelly(
    concurrentAmount: UInt64,
    destination: C
): C {
    return (this as Iterable<*>).filterIsInstanceToParallelly(this.usize / concurrentAmount, destination)
}

suspend inline fun <reified T, C : MutableCollection<in T>> List<*>.filterIsInstanceToParallelly(
    destination: C
): C {
    return this.filterIsInstanceToParallelly(
        defaultConcurrentAmount,
        destination
    )
}

suspend inline fun <reified T, C : MutableCollection<in T>> List<*>.filterIsInstanceToParallelly(
    concurrentAmount: UInt64,
    destination: C
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val segmentAmount = this@filterIsInstanceToParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@filterIsInstanceToParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@filterIsInstanceToParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@filterIsInstanceToParallelly.subList(j, k).filterIsInstance<T>()
            })
            i = k
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

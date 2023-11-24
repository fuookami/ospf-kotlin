package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, C: MutableCollection<in T>> Iterable<T>.filterNotToParallelly(destination: C, crossinline predicate: Predicate<T>): C {
    return this.filterNotToParallelly(UInt64.ten, destination, predicate)
}

suspend inline fun <T, C: MutableCollection<in T>> Iterable<T>.filterNotToParallelly(segment: UInt64, destination: C, crossinline predicate: Predicate<T>): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val iterator = this@filterNotToParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.filterNot(predicate)
            })
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

suspend inline fun <T, C: MutableCollection<in T>> Collection<T>.filterNotToParallelly(destination: C, crossinline predicate: Predicate<T>): C {
    return (this as Iterable<T>).filterNotToParallelly(
        UInt64(
            maxOf(
                minOf(
                    Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                    Runtime.getRuntime().availableProcessors()
                ),
                1
            )
        ),
        destination,
        predicate
    )
}

suspend inline fun <T, C: MutableCollection<in T>> Collection<T>.filterNotToParallelly(concurrentAmount: UInt64, destination: C, crossinline predicate: Predicate<T>): C {
    return (this as Iterable<T>).filterNotToParallelly(UInt64(this.size) / concurrentAmount, destination, predicate)
}

suspend inline fun <T, C: MutableCollection<in T>> List<T>.filterNotToParallelly(destination: C, crossinline predicate: Predicate<T>): C {
    return this.filterNotToParallelly(
        UInt64(
            maxOf(
                minOf(
                    Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                    Runtime.getRuntime().availableProcessors()
                ),
                1
            )
        ),
        destination,
        predicate
    )
}

suspend inline fun <T, C: MutableCollection<in T>> List<T>.filterNotToParallelly(concurrentAmount: UInt64, destination: C, crossinline predicate: Predicate<T>): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val segmentAmount = this@filterNotToParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@filterNotToParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@filterNotToParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@filterNotToParallelly.subList(j, k).filterNot(predicate)
            })
            i = k
        }

        promises.flatMapTo(destination) { it.await() }
    }
}

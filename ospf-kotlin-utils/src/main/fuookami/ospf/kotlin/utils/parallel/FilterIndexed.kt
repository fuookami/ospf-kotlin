package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.filterIndexedParallelly(crossinline predicate: IndexedPredicate<T>): List<T> {
    return this.filterIndexedParallelly(UInt64.ten, predicate)
}

suspend inline fun <T> Iterable<T>.filterIndexedParallelly(segment: UInt64, crossinline predicate: IndexedPredicate<T>): List<T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val iterator = this@filterIndexedParallelly.iterator()
        var i = 0
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<Pair<Int, T>>()
            var j = UInt64.zero
            while (iterator.hasNext() && j != segment) {
                thisSegment.add(Pair(i, iterator.next()))
                ++i
                ++j
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.filter { predicate(it.first, it.second) }.map { it.second }
            })
        }
        promises.flatMap { it.await() }
    }
}

suspend inline fun <T> Collection<T>.filterIndexedParallelly(crossinline predicate: IndexedPredicate<T>): List<T> {
    return this.filterIndexedParallelly(
        UInt64(
            minOf(
                Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                Runtime.getRuntime().availableProcessors()
            )
        ),
        predicate
    )
}

suspend inline fun <T> Collection<T>.filterIndexedParallelly(concurrentAmount: UInt64, crossinline predicate: IndexedPredicate<T>): List<T> {
    return (this as Iterable<T>).filterIndexedParallelly(UInt64(this.size) / concurrentAmount, predicate)
}

suspend inline fun <T> List<T>.filterIndexedParallelly(crossinline predicate: IndexedPredicate<T>): List<T> {
    return this.filterIndexedParallelly(
        UInt64(
            maxOf(
                minOf(
                    Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                    Runtime.getRuntime().availableProcessors()
                ),
                1
            )
        ),
        predicate
    )
}

suspend inline fun <T> List<T>.filterIndexedParallelly(concurrentAmount: UInt64, crossinline predicate: IndexedPredicate<T>): List<T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<List<T>>>()
        val segmentAmount = this@filterIndexedParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@filterIndexedParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@filterIndexedParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@filterIndexedParallelly.subList(j, k).filterIndexed { i, v -> predicate(i + j, v) }
            })
            i = k
        }
        promises.flatMap { it.await() }
    }
}

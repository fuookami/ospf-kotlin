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
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.filterIndexed(predicate)
            })
        }
        promises.flatMap { it.await() }
    }
}

suspend inline fun <T> Collection<T>.filterIndexedParallelly(crossinline predicate: IndexedPredicate<T>): List<T> {
    return this.filterIndexedParallelly(UInt64(minOf(
        Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
        Runtime.getRuntime().availableProcessors()
    )), predicate)
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
                this@filterIndexedParallelly.subList(j, k).filterIndexed(predicate)
            })
            i = k
        }
        promises.flatMap { it.await() }
    }
}

package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.allParallelly(crossinline predicate: Predicate<T>): Boolean {
    return this.allParallelly(UInt64.ten, predicate)
}

suspend inline fun <T> Iterable<T>.allParallelly(segment: UInt64, crossinline predicate: Predicate<T>): Boolean {
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            val iterator = this@allParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.all(predicate)
                })
            }
            for (promise in promises) {
                if (!promise.await()) {
                    cancel()
                }
            }
            true
        }
    } catch (e: CancellationException) {
        false
    }
}

suspend inline fun <T> Collection<T>.allParallelly(crossinline predicate: Predicate<T>): Boolean {
    return this.allParallelly(UInt64(minOf(
        Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
        Runtime.getRuntime().availableProcessors()
    )), predicate)
}

suspend inline fun <T> Collection<T>.allParallelly(concurrentAmount: UInt64, crossinline predicate: Predicate<T>): Boolean {
    return (this as Iterable<T>).allParallelly(UInt64(this.size) / concurrentAmount, predicate)
}

suspend inline fun <T> List<T>.allParallelly(crossinline predicate: Predicate<T>): Boolean {
    return this.allParallelly(UInt64(minOf(
        Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
        Runtime.getRuntime().availableProcessors()
    )), predicate)
}

suspend inline fun <T> List<T>.allParallelly(concurrentAmount: UInt64, crossinline predicate: Predicate<T>): Boolean {
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            val segmentAmount = this@allParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@allParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@allParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@allParallelly.subList(j, k).all(predicate)
                })
                i = k
            }
            for (promise in promises) {
                if (!promise.await()) {
                    cancel()
                }
            }
            true
        }
    } catch (e: CancellationException) {
        false
    }
}

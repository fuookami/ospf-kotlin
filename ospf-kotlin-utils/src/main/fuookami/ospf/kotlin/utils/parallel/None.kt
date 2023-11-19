package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.noneParallelly(crossinline predicate: Predicate<T>): Boolean {
    return this.noneParallelly(UInt64.ten, predicate)
}

suspend inline fun <T> Iterable<T>.noneParallelly(segment: UInt64, crossinline predicate: Predicate<T>): Boolean {
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            val iterator = this@noneParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.none(predicate)
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

suspend inline fun <T> Collection<T>.noneParallelly(crossinline predicate: Predicate<T>): Boolean {
    return this.noneParallelly(
        UInt64(
            minOf(
                Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                Runtime.getRuntime().availableProcessors()
            )
        ),
        predicate
    )
}

suspend inline fun <T> Collection<T>.noneParallelly(concurrentAmount: UInt64, crossinline predicate: Predicate<T>): Boolean {
    return (this as Iterable<T>).noneParallelly(UInt64(this.size) / concurrentAmount, predicate)
}

suspend inline fun <T> List<T>.noneParallelly(crossinline predicate: Predicate<T>): Boolean {
    return this.noneParallelly(
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

suspend inline fun <T> List<T>.noneParallelly(concurrentAmount: UInt64, crossinline predicate: Predicate<T>): Boolean {
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            val segmentAmount = this@noneParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@noneParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@noneParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@noneParallelly.subList(j, k).none(predicate)
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

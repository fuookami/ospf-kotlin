package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.anyParallelly(crossinline predicate: Predicate<T>): Boolean {
    return this.anyParallelly(UInt64.ten, predicate)
}

suspend inline fun <T> Iterable<T>.anyParallelly(segment: UInt64, crossinline predicate: Predicate<T>): Boolean {
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            val iterator = this@anyParallelly.iterator()
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
                if (promise.await()) {
                    cancel()
                }
            }
            false
        }
    } catch (e: CancellationException) {
        true
    }
}

suspend inline fun <T> Collection<T>.anyParallelly(crossinline predicate: Predicate<T>): Boolean {
    return this.anyParallelly(
        UInt64(
            minOf(
                Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                Runtime.getRuntime().availableProcessors()
            )
        ),
        predicate
    )
}

suspend inline fun <T> Collection<T>.anyParallelly(parallel: UInt64, crossinline predicate: Predicate<T>): Boolean {
    return (this as Iterable<T>).anyParallelly(UInt64(this.size) / parallel, predicate)
}

suspend inline fun <T> List<T>.anyParallelly(crossinline predicate: Predicate<T>): Boolean {
    return this.anyParallelly(
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

suspend inline fun <T> List<T>.anyParallelly(concurrentAmount: UInt64, crossinline predicate: Predicate<T>): Boolean {
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            val segmentAmount = this@anyParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@anyParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@anyParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@anyParallelly.subList(j, k).any(predicate)
                })
                i = k
            }
            for (promise in promises) {
                if (promise.await()) {
                    cancel()
                }
            }
            false
        }
    } catch (e: CancellationException) {
        true
    }
}

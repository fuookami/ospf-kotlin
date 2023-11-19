package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.lastParallelly(crossinline predicate: Predicate<T>): T {
    return this.lastParallelly(UInt64(Runtime.getRuntime().availableProcessors()), predicate)
}

suspend inline fun <T> Iterable<T>.lastParallelly(segment: UInt64, crossinline predicate: Predicate<T>): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        val iterator = this@lastParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.lastOrNull(predicate)
            })
        }

        for (promise in promises.reversed()) {
            val result = promise.await()
            if (result != null) {
                return@coroutineScope result
            }
        }
        throw NoSuchElementException("Collection contains no element matching the predicate.")
    }
}

suspend inline fun <T> Collection<T>.lastParallelly(crossinline predicate: Predicate<T>): T {
    return (this as Iterable<T>).lastParallelly(
        UInt64(
            minOf(
                Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                Runtime.getRuntime().availableProcessors()
            )
        ),
        predicate
    )
}

suspend inline fun <T> Collection<T>.lastParallelly(concurrentAmount: UInt64, crossinline predicate: Predicate<T>): T {
    return (this as Iterable<T>).lastParallelly(UInt64(this.size) / concurrentAmount, predicate)
}

suspend inline fun <T> List<T>.lastParallelly(crossinline predicate: Predicate<T>): T {
    return this.lastParallelly(
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

suspend inline fun <T> List<T>.lastParallelly(concurrentAmount: UInt64, crossinline predicate: Predicate<T>): T {
    var result: T? = null

    return try {
        coroutineScope {
            val iterator = this@lastParallelly.listIterator(this@lastParallelly.size)
            while (iterator.hasPrevious()) {
                val promises = ArrayList<Deferred<T?>>()
                for (j in UInt64.zero until concurrentAmount) {
                    val v = iterator.previous()
                    promises.add(async(Dispatchers.Default) {
                        if (predicate(v)) {
                            v
                        } else {
                            null
                        }
                    })

                    if (!iterator.hasPrevious()) {
                        break
                    }
                }
                for (promise in promises) {
                    result = promise.await()
                    if (result != null) {
                        cancel()
                    }
                }
            }
        }
        throw NoSuchElementException("Collection contains no element matching the predicate.")
    } catch (e: CancellationException) {
        result!!
    }
}

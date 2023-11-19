package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.lastOrNullParallelly(crossinline predicate: Predicate<T>): T? {
    return this.lastOrNullParallelly(UInt64(Runtime.getRuntime().availableProcessors()), predicate)
}

suspend inline fun <T> Iterable<T>.lastOrNullParallelly(segment: UInt64, crossinline predicate: Predicate<T>): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        val iterator = this@lastOrNullParallelly.iterator()
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
        null
    }
}

suspend inline fun <T> Collection<T>.lastOrNullParallelly(crossinline predicate: Predicate<T>): T? {
    return (this as Iterable<T>).lastOrNullParallelly(
        UInt64(
            minOf(
                Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                Runtime.getRuntime().availableProcessors()
            )
        ),
        predicate
    )
}

suspend inline fun <T> Collection<T>.lastOrNullParallelly(concurrentAmount: UInt64, crossinline predicate: Predicate<T>): T? {
    return (this as Iterable<T>).lastOrNullParallelly(UInt64(this.size) / concurrentAmount, predicate)
}

suspend inline fun <T> List<T>.lastOrNullParallelly(crossinline predicate: Predicate<T>): T? {
    return this.lastOrNullParallelly(
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

suspend inline fun <T> List<T>.lastOrNullParallelly(concurrentAmount: UInt64, crossinline predicate: Predicate<T>): T? {
    var result: T? = null

    return try {
        coroutineScope {
            val iterator = this@lastOrNullParallelly.listIterator(this@lastOrNullParallelly.size)
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
        null
    } catch (e: CancellationException) {
        result
    }
}

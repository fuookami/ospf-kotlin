package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <R, T> Iterable<T>.lastNotNullOfParallelly(crossinline extractor: Extractor<R?, T>): R {
    return this.lastNotNullOfParallelly(UInt64(Runtime.getRuntime().availableProcessors()), extractor)
}

suspend inline fun <R, T> Iterable<T>.lastNotNullOfParallelly(segment: UInt64, crossinline extractor: Extractor<R?, T>): R {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val iterator = this@lastNotNullOfParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.lastNotNullOf(extractor)
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

suspend inline fun <R, T> Collection<T>.lastNotNullOfParallelly(crossinline extractor: Extractor<R?, T>): R {
    return (this as Iterable<T>).lastNotNullOfParallelly(
        UInt64(
            maxOf(
                minOf(
                    Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                    Runtime.getRuntime().availableProcessors()
                ),
                1
            )
        ),
        extractor
    )
}

suspend inline fun <R, T> Collection<T>.lastNotNullOfParallelly(concurrentAmount: UInt64, crossinline extractor: Extractor<R?, T>): R {
    return (this as Iterable<T>).lastNotNullOfParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <R, T> List<T>.lastNotNullOfParallelly(crossinline extractor: Extractor<R?, T>): R {
    return this.lastNotNullOfParallelly(
        UInt64(
            maxOf(
                minOf(
                    Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                    Runtime.getRuntime().availableProcessors()
                ),
                1
            )
        ),
        extractor
    )
}

suspend inline fun <R, T> List<T>.lastNotNullOfParallelly(concurrentAmount: UInt64, crossinline extractor: Extractor<R?, T>): R {
    var result: R? = null

    return try {
        coroutineScope {
            val iterator = this@lastNotNullOfParallelly.listIterator(this@lastNotNullOfParallelly.size)
            while (iterator.hasPrevious()) {
                val promises = ArrayList<Deferred<R?>>()
                for (j in UInt64.zero until concurrentAmount) {
                    val v = iterator.previous()
                    promises.add(async(Dispatchers.Default) {
                        extractor(v)
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

package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <R, T> Iterable<T>.firstNotNullOfParallelly(crossinline extractor: Extractor<R?, T>): R {
    return this.firstNotNullOfParallelly(UInt64(Runtime.getRuntime().availableProcessors()), extractor)
}

suspend inline fun <R, T> Iterable<T>.firstNotNullOfParallelly(concurrentAmount: UInt64, crossinline extractor: Extractor<R?, T>): R {
    var result: R? = null

    return try {
        coroutineScope {
            val iterator = this@firstNotNullOfParallelly.iterator()
            while (iterator.hasNext()) {
                val promises = ArrayList<Deferred<R?>>()
                for (j in UInt64.zero until concurrentAmount) {
                    val v = iterator.next()
                    promises.add(async(Dispatchers.Default) {
                        extractor(v)
                    })

                    if (!iterator.hasNext()) {
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

        throw NoSuchElementException("No element of the collection was transformed to a non-null value.")
    } catch (e: CancellationException) {
        result!!
    }
}

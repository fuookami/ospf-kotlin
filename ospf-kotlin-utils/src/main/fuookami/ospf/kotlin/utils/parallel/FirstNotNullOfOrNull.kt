package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <R, T> Iterable<T>.firstNotNullOfOrNullParallelly(crossinline extractor: Extractor<R?, T>): R? {
    return this.firstNotNullOfOrNullParallelly(UInt64(Runtime.getRuntime().availableProcessors()), extractor)
}

suspend inline fun <R, T> Iterable<T>.firstNotNullOfOrNullParallelly(concurrentAmount: UInt64, crossinline extractor: Extractor<R?, T>): R? {
    var result: R? = null

    return try {
        coroutineScope {
            val iterator = this@firstNotNullOfOrNullParallelly.iterator()
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
        null
    } catch (e: CancellationException) {
        result!!
    }
}

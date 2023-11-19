package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.firstOrNullParallelly(crossinline predicate: Predicate<T>): T? {
    return this.firstOrNullParallelly(UInt64(Runtime.getRuntime().availableProcessors()), predicate)
}

suspend inline fun <T> Iterable<T>.firstOrNullParallelly(concurrentAmount: UInt64, crossinline predicate: Predicate<T>): T? {
    var result: T? = null

    return try {
        coroutineScope {
            val iterator = this@firstOrNullParallelly.iterator()
            while (iterator.hasNext()) {
                val promises = ArrayList<Deferred<T?>>()
                for (j in UInt64.zero until concurrentAmount) {
                    val v = iterator.next()
                    promises.add(async(Dispatchers.Default) {
                        if (predicate(v)) {
                            v
                        } else {
                            null
                        }
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

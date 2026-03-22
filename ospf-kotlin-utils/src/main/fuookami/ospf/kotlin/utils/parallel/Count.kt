package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.error.*

// countParallelly: Count elements matching the predicate in parallel / 并行计数匹配谓词的元素
suspend inline fun <T> Iterable<T>.countParallelly(
    crossinline predicate: SuspendPredicate<T>
): Int {
    return coroutineScope {
        val promises = ArrayList<Deferred<Boolean>>()
        for (element in this@countParallelly.iterator()) {
            promises.add(async(Dispatchers.Default) { predicate(element) })
        }
        promises.count { it.await() }
    }
}

// tryCountParallelly: Try version of countParallelly / countParallelly 的 try 版本
suspend inline fun <T> Iterable<T>.tryCountParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Int> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<Boolean>>>()
            for (element in this@tryCountParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) { predicate(element) })
            }
            Ok(promises.count {
                when (val result = it.await()) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        error = result.error
                        cancel()
                        false
                    }
                }
            })
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(0)
    }
}
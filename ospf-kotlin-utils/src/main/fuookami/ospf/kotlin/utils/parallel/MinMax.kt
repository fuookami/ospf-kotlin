package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.error.*

// minMaxByParallelly: Find min and max elements by selector in parallel / 并行按选择器查找最小和最大元素
suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxByParallelly(
    crossinline selector: SuspendExtractor<R, T>
): Pair<T, T> {
    return this.minMaxByOrNullParallelly(selector)
        ?: throw NoSuchElementException("Collection is empty.")
}

// tryMinMaxByParallelly: Try version of minMaxByParallelly / minMaxByParallelly 的 try 版本
suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinMaxByParallelly(
    crossinline selector: SuspendTryExtractor<R, T>
): Ret<Pair<T, T>> {
    return when (val result = this.tryMinMaxByOrNullParallelly(selector)) {
        is Ok -> {
            result.value
                ?.let { Ok(it) }
                ?: Failed(Err(ErrorCode.ApplicationException, "Collection is empty."))
        }

        is Failed -> {
            Failed(result.error)
        }
    }
}

// minMaxByOrNullParallelly: Find min and max elements by selector in parallel, or null / 并行按选择器查找最小和最大元素，或 null
suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxByOrNullParallelly(
    crossinline selector: SuspendExtractor<R, T>
): Pair<T, T>? {
    var minElement: T? = null
    var minValue: R? = null
    var maxElement: T? = null
    var maxValue: R? = null

    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, R>>>()
        for (element in this@minMaxByOrNullParallelly.iterator()) {
            promises.add(async(Dispatchers.Default) {
                element to selector(element)
            })
        }
        for (promise in promises) {
            val (element, value) = promise.await()
            if (minValue == null || value < minValue!!) {
                minValue = value
                minElement = element
            }
            if (maxValue == null || value > maxValue!!) {
                maxValue = value
                maxElement = element
            }
        }
        if (minElement != null && maxElement != null) {
            minElement to maxElement
        } else {
            null
        }
    }
}

// tryMinMaxByOrNullParallelly: Try version of minMaxByOrNullParallelly / minMaxByOrNullParallelly 的 try 版本
suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinMaxByOrNullParallelly(
    crossinline selector: SuspendTryExtractor<R, T>
): Ret<Pair<T, T>?> {
    var minElement: T? = null
    var minValue: R? = null
    var maxElement: T? = null
    var maxValue: R? = null
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<Pair<T, R>>>>()
            for (element in this@tryMinMaxByOrNullParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    selector(element).map { element to it }
                })
            }
            for (promise in promises) {
                when (val ret = promise.await()) {
                    is Ok -> {
                        val (element, value) = ret.value
                        if (minValue == null || value < minValue!!) {
                            minValue = value
                            minElement = element
                        }
                        if (maxValue == null || value > maxValue!!) {
                            maxValue = value
                            maxElement = element
                        }
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                    }
                }
            }
            Ok(if (minElement != null && maxElement != null) minElement to maxElement else null)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(if (minElement != null && maxElement != null) minElement to maxElement else null)
    }
}
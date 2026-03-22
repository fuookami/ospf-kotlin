package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.error.*

// maxByParallelly: Find max element by selector in parallel / 并行按选择器查找最大元素
suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxByParallelly(
    crossinline selector: SuspendExtractor<R, T>
): T {
    return this.maxByOrNullParallelly(selector)
        ?: throw NoSuchElementException("Collection is empty.")
}

// tryMaxByParallelly: Try version of maxByParallelly / maxByParallelly 的 try 版本
suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMaxByParallelly(
    crossinline selector: SuspendTryExtractor<R, T>
): Ret<T> {
    return when (val result = this.tryMaxByOrNullParallelly(selector)) {
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

// maxByOrNullParallelly: Find max element by selector in parallel, or null / 并行按选择器查找最大元素，或 null
suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxByOrNullParallelly(
    crossinline selector: SuspendExtractor<R, T>
): T? {
    var maxElement: T? = null
    var maxValue: R? = null

    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, R>>>()
        for (element in this@maxByOrNullParallelly.iterator()) {
            promises.add(async(Dispatchers.Default) {
                element to selector(element)
            })
        }
        for (promise in promises) {
            val (element, value) = promise.await()
            if (maxValue == null || value > maxValue!!) {
                maxValue = value
                maxElement = element
            }
        }
        maxElement
    }
}

// tryMaxByOrNullParallelly: Try version of maxByOrNullParallelly / maxByOrNullParallelly 的 try 版本
suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMaxByOrNullParallelly(
    crossinline selector: SuspendTryExtractor<R, T>
): Ret<T?> {
    var maxElement: T? = null
    var maxValue: R? = null
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<Pair<T, R>>>>()
            for (element in this@tryMaxByOrNullParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    selector(element).map { element to it }
                })
            }
            for (promise in promises) {
                when (val ret = promise.await()) {
                    is Ok -> {
                        val (element, value) = ret.value
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
            Ok(maxElement)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(maxElement)
    }
}

// minByParallelly: Find min element by selector in parallel / 并行按选择器查找最小元素
suspend inline fun <T, R : Comparable<R>> Iterable<T>.minByParallelly(
    crossinline selector: SuspendExtractor<R, T>
): T {
    return this.minByOrNullParallelly(selector)
        ?: throw NoSuchElementException("Collection is empty.")
}

// tryMinByParallelly: Try version of minByParallelly / minByParallelly 的 try 版本
suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinByParallelly(
    crossinline selector: SuspendTryExtractor<R, T>
): Ret<T> {
    return when (val result = this.tryMinByOrNullParallelly(selector)) {
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

// minByOrNullParallelly: Find min element by selector in parallel, or null / 并行按选择器查找最小元素，或 null
suspend inline fun <T, R : Comparable<R>> Iterable<T>.minByOrNullParallelly(
    crossinline selector: SuspendExtractor<R, T>
): T? {
    var minElement: T? = null
    var minValue: R? = null

    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, R>>>()
        for (element in this@minByOrNullParallelly.iterator()) {
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
        }
        minElement
    }
}

// tryMinByOrNullParallelly: Try version of minByOrNullParallelly / minByOrNullParallelly 的 try 版本
suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinByOrNullParallelly(
    crossinline selector: SuspendTryExtractor<R, T>
): Ret<T?> {
    var minElement: T? = null
    var minValue: R? = null
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<Pair<T, R>>>>()
            for (element in this@tryMinByOrNullParallelly.iterator()) {
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
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                    }
                }
            }
            Ok(minElement)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(minElement)
    }
}
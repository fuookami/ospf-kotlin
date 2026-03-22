package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.error.*

// flatMapParallelly: Flat map elements in parallel / 并行 flatMap 元素
suspend inline fun <R, T> Iterable<T>.flatMapParallelly(
    crossinline extractor: SuspendExtractor<Iterable<R>, T>
): List<R> {
    return this.flatMapToParallelly(ArrayList(), extractor)
}

// tryFlatMapToParallelly: Try version of flatMapParallelly / flatMapParallelly 的 try 版本
suspend inline fun <R, T> Iterable<T>.tryFlatMapToParallelly(
    crossinline extractor: SuspendTryExtractor<Iterable<R>, T>
): Ret<List<R>> {
    return this.tryFlatMapToParallelly(ArrayList(), extractor)
}

// flatMapToParallelly: Flat map elements to destination in parallel / 并行 flatMap 元素到目标集合
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapToParallelly(
    destination: C,
    crossinline extractor: SuspendExtractor<Iterable<R>, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<Iterable<R>>>()
        for (element in this@flatMapToParallelly.iterator()) {
            promises.add(async(Dispatchers.Default) {
                extractor(element)
            })
        }
        promises.flatMapTo(destination) { it.await() }
    }
}

// tryFlatMapToParallelly: Try version of flatMapToParallelly / flatMapToParallelly 的 try 版本
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapToParallelly(
    destination: C,
    crossinline extractor: SuspendTryExtractor<Iterable<R>, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<Iterable<R>>>>()
            for (element in this@tryFlatMapToParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    extractor(element)
                })
            }
            val result = promises.mapNotNull {
                when (val ret = it.await()) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        null
                    }
                }
            }
            destination.addAll(result.flatten())
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}

// flatMapIndexedParallelly: Flat map elements with index in parallel / 并行带索引 flatMap 元素
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedParallelly(
    crossinline extractor: SuspendIndexedExtractor<Iterable<R>, T>
): List<R> {
    return this.flatMapIndexedToParallelly(ArrayList(), extractor)
}

// tryFlatMapIndexedToParallelly: Try version of flatMapIndexedParallelly / flatMapIndexedParallelly 的 try 版本
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapIndexedToParallelly(
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R>, T>
): Ret<List<R>> {
    return this.tryFlatMapIndexedToParallelly(ArrayList(), extractor)
}

// flatMapIndexedToParallelly: Flat map elements with index to destination in parallel / 并行带索引 flatMap 元素到目标集合
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedToParallelly(
    destination: C,
    crossinline extractor: SuspendIndexedExtractor<Iterable<R>, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<Iterable<R>>>()
        for ((index, element) in this@flatMapIndexedToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) {
                extractor(index, element)
            })
        }
        promises.flatMapTo(destination) { it.await() }
    }
}

// tryFlatMapIndexedToParallelly: Try version of flatMapIndexedToParallelly / flatMapIndexedToParallelly 的 try 版本
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapIndexedToParallelly(
    destination: C,
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R>, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<Iterable<R>>>>()
            for ((index, element) in this@tryFlatMapIndexedToParallelly.withIndex()) {
                promises.add(async(Dispatchers.Default) {
                    extractor(index, element)
                })
            }
            val result = promises.mapNotNull {
                when (val ret = it.await()) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        null
                    }
                }
            }
            destination.addAll(result.flatten())
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}

// flatMapIndexedNotNullParallelly: Flat map elements with index to non-null results in parallel / 并行带索引 flatMap 元素到非空结果
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedNotNullParallelly(
    crossinline extractor: SuspendIndexedExtractor<Iterable<R?>, T>
): List<R> {
    return this.flatMapIndexedNotNullToParallelly(ArrayList(), extractor)
}

// tryFlatMapIndexedNotNullToParallelly: Try version of flatMapIndexedNotNullParallelly / flatMapIndexedNotNullParallelly 的 try 版本
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapIndexedNotNullToParallelly(
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R?>, T>
): Ret<List<R>> {
    return this.tryFlatMapIndexedNotNullToParallelly(ArrayList(), extractor)
}

// flatMapIndexedNotNullToParallelly: Flat map elements with index to non-null results to destination in parallel / 并行带索引 flatMap 元素到非空结果到目标集合
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedNotNullToParallelly(
    destination: C,
    crossinline extractor: SuspendIndexedExtractor<Iterable<R?>, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<Iterable<R?>>>()
        for ((index, element) in this@flatMapIndexedNotNullToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) {
                extractor(index, element)
            })
        }
        for (promise in promises) {
            destination.addAll(promise.await().filterNotNull())
        }
        destination
    }
}

// tryFlatMapIndexedNotNullToParallelly: Try version of flatMapIndexedNotNullToParallelly / flatMapIndexedNotNullToParallelly 的 try 版本
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapIndexedNotNullToParallelly(
    destination: C,
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R?>, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<Iterable<R?>>>>()
            for ((index, element) in this@tryFlatMapIndexedNotNullToParallelly.withIndex()) {
                promises.add(async(Dispatchers.Default) {
                    extractor(index, element)
                })
            }
            val result = promises.mapNotNull {
                when (val ret = it.await()) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        null
                    }
                }
            }
            for (iterable in result) {
                destination.addAll(iterable.filterNotNull())
            }
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}
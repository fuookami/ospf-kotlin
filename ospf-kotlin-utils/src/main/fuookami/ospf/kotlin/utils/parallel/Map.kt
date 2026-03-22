package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.error.*

// mapParallelly: Map elements in parallel / 并行映射元素
suspend inline fun <R : Any, T> Iterable<T>.mapParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): List<R> {
    return this.mapToParallelly(ArrayList(), extractor)
}

// tryMapParallelly: Try version of mapParallelly / mapParallelly 的 try 版本
suspend inline fun <R, T> Iterable<T>.tryMapParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<List<R>> {
    return this.tryMapToParallelly(ArrayList(), extractor)
}

// mapToParallelly: Map elements to destination in parallel / 并行映射元素到目标集合
suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.mapToParallelly(
    destination: C,
    crossinline extractor: SuspendExtractor<R, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        for (element in this@mapToParallelly.iterator()) {
            promises.add(async(Dispatchers.Default) {
                extractor(element)
            })
        }
        promises.mapTo(destination) { it.await() }
    }
}

// tryMapToParallelly: Try version of mapToParallelly / mapToParallelly 的 try 版本
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryMapToParallelly(
    destination: C,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<R>>>()
            for (element in this@tryMapToParallelly.iterator()) {
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
            destination.addAll(result)
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}

// mapNotNullParallelly: Map elements to non-null results in parallel / 并行映射元素到非空结果
suspend inline fun <R : Any, T> Iterable<T>.mapNotNullParallelly(
    crossinline extractor: SuspendExtractor<R?, T>
): List<R> {
    return this.mapNotNullToParallelly(ArrayList(), extractor)
}

// tryMapNotNullParallelly: Try version of mapNotNullParallelly / mapNotNullParallelly 的 try 版本
suspend inline fun <R, T> Iterable<T>.tryMapNotNullParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<List<R>> {
    return this.tryMapNotNullToParallelly(ArrayList(), extractor)
}

// mapNotNullToParallelly: Map elements to non-null results to destination in parallel / 并行映射元素到非空结果到目标集合
suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.mapNotNullToParallelly(
    destination: C,
    crossinline extractor: SuspendExtractor<R?, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        for (element in this@mapNotNullToParallelly.iterator()) {
            promises.add(async(Dispatchers.Default) {
                extractor(element)
            })
        }
        promises.mapNotNullTo(destination) { it.await() }
    }
}

// tryMapNotNullToParallelly: Try version of mapNotNullToParallelly / mapNotNullToParallelly 的 try 版本
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryMapNotNullToParallelly(
    destination: C,
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<R?>>>()
            for (element in this@tryMapNotNullToParallelly.iterator()) {
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
            destination.addAll(result)
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}

// mapIndexedParallelly: Map elements with index in parallel / 并行带索引映射元素
suspend inline fun <R, T> Iterable<T>.mapIndexedParallelly(
    crossinline extractor: SuspendIndexedExtractor<R, T>
): List<R> {
    return this.mapIndexedToParallelly(ArrayList(), extractor)
}

// tryMapIndexedParallelly: Try version of mapIndexedParallelly / mapIndexedParallelly 的 try 版本
suspend inline fun <R, T> Iterable<T>.tryMapIndexedParallelly(
    crossinline extractor: SuspendTryIndexedExtractor<R, T>
): Ret<List<R>> {
    return this.tryMapIndexedToParallelly(ArrayList(), extractor)
}

// mapIndexedToParallelly: Map elements with index to destination in parallel / 并行带索引映射元素到目标集合
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.mapIndexedToParallelly(
    destination: C,
    crossinline extractor: SuspendIndexedExtractor<R, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        for ((index, element) in this@mapIndexedToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) {
                extractor(index, element)
            })
        }
        promises.mapTo(destination) { it.await() }
    }
}

// tryMapIndexedToParallelly: Try version of mapIndexedToParallelly / mapIndexedToParallelly 的 try 版本
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryMapIndexedToParallelly(
    destination: C,
    crossinline extractor: SuspendTryIndexedExtractor<R, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<R>>>()
            for ((index, element) in this@tryMapIndexedToParallelly.withIndex()) {
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
            destination.addAll(result)
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}

// mapIndexedNotNullParallelly: Map elements with index to non-null results in parallel / 并行带索引映射元素到非空结果
suspend inline fun <R : Any, T> Iterable<T>.mapIndexedNotNullParallelly(
    crossinline extractor: SuspendIndexedExtractor<R?, T>
): List<R> {
    return this.mapIndexedNotNullToParallelly(ArrayList(), extractor)
}

// tryMapIndexedNotNullParallelly: Try version of mapIndexedNotNullParallelly / mapIndexedNotNullParallelly 的 try 版本
suspend inline fun <R, T> Iterable<T>.tryMapIndexedNotNullParallelly(
    crossinline extractor: SuspendTryIndexedExtractor<R?, T>
): Ret<List<R>> {
    return this.tryMapIndexedNotNullToParallelly(ArrayList(), extractor)
}

// mapIndexedNotNullToParallelly: Map elements with index to non-null results to destination in parallel / 并行带索引映射元素到非空结果到目标集合
suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.mapIndexedNotNullToParallelly(
    destination: C,
    crossinline extractor: SuspendIndexedExtractor<R?, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        for ((index, element) in this@mapIndexedNotNullToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) {
                extractor(index, element)
            })
        }
        promises.mapNotNullTo(destination) { it.await() }
    }
}

// tryMapIndexedNotNullToParallelly: Try version of mapIndexedNotNullToParallelly / mapIndexedNotNullToParallelly 的 try 版本
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryMapIndexedNotNullToParallelly(
    destination: C,
    crossinline extractor: SuspendTryIndexedExtractor<R?, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<R?>>>()
            for ((index, element) in this@tryMapIndexedNotNullToParallelly.withIndex()) {
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
            destination.addAll(result)
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}
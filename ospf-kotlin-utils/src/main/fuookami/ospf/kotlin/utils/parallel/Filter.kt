package fuookami.ospf.kotlin.utils.parallel

import kotlin.reflect.full.*
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.error.*

// filterParallelly: Filter elements matching the predicate in parallel / 并行过滤匹配谓词的元素
suspend inline fun <T : Any> Iterable<T>.filterParallelly(
    crossinline predicate: SuspendPredicate<T>
): List<T> {
    return this.filterToParallelly(ArrayList(), predicate)
}

// tryFilterParallelly: Try version of filterParallelly / filterParallelly 的 try 版本
suspend inline fun <T : Any> Iterable<T>.tryFilterParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<List<T>> {
    return this.tryFilterToParallelly(ArrayList(), predicate)
}

// filterToParallelly: Filter elements to destination in parallel / 并行过滤元素到目标集合
suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.filterToParallelly(
    destination: C,
    crossinline predicate: SuspendPredicate<T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        for (element in this@filterToParallelly.iterator()) {
            promises.add(async(Dispatchers.Default) {
                if (predicate(element)) {
                    element
                } else {
                    null
                }
            })
        }
        promises.mapNotNullTo(destination) { it.await() }
    }
}

// tryFilterToParallelly: Try version of filterToParallelly / filterToParallelly 的 try 版本
suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.tryFilterToParallelly(
    destination: C,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<T?>>>()
            for (element in this@tryFilterToParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    when (val ret = predicate(element)) {
                        is Ok -> {
                            if (ret.value) {
                                Ok(element)
                            } else {
                                Ok(null)
                            }
                        }

                        is Failed -> {
                            Failed(ret.error)
                        }
                    }
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

// filterNotNullParallelly: Filter non-null elements matching the predicate in parallel / 并行过滤非空匹配谓词的元素
suspend inline fun <T : Any> Iterable<T?>.filterNotNullParallelly(
    crossinline predicate: SuspendPredicate<T>
): List<T> {
    return this.filterNotNullToParallelly(ArrayList(), predicate)
}

// tryFilterNotNullParallelly: Try version of filterNotNullParallelly / filterNotNullParallelly 的 try 版本
suspend inline fun <T : Any> Iterable<T?>.tryFilterNotNullParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<List<T>> {
    return this.tryFilterNotNullToParallelly(ArrayList(), predicate)
}

// filterNotNullToParallelly: Filter non-null elements to destination in parallel / 并行过滤非空元素到目标集合
suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T?>.filterNotNullToParallelly(
    destination: C,
    crossinline predicate: SuspendPredicate<T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        for (element in this@filterNotNullToParallelly.iterator()) {
            if (element != null) {
                promises.add(async(Dispatchers.Default) {
                    if (predicate(element)) {
                        element
                    } else {
                        null
                    }
                })
            }
        }
        promises.mapNotNullTo(destination) { it.await() }
    }
}

// tryFilterNotNullToParallelly: Try version of filterNotNullToParallelly / filterNotNullToParallelly 的 try 版本
suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T?>.tryFilterNotNullToParallelly(
    destination: C,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<T?>>>()
            for (element in this@tryFilterNotNullToParallelly.iterator()) {
                if (element != null) {
                    promises.add(async(Dispatchers.Default) {
                        when (val ret = predicate(element)) {
                            is Ok -> {
                                if (ret.value) {
                                    Ok(element)
                                } else {
                                    Ok(null)
                                }
                            }

                            is Failed -> {
                                Failed(ret.error)
                            }
                        }
                    })
                }
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

// filterNotParallelly: Filter elements not matching the predicate in parallel / 并行过滤不匹配谓词的元素
suspend inline fun <T : Any> Iterable<T>.filterNotParallelly(
    crossinline predicate: SuspendPredicate<T>
): List<T> {
    return this.filterNotToParallelly(ArrayList(), predicate)
}

// tryFilterNotParallelly: Try version of filterNotParallelly / filterNotParallelly 的 try 版本
suspend inline fun <T : Any> Iterable<T>.tryFilterNotParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<List<T>> {
    return this.tryFilterNotToParallelly(ArrayList(), predicate)
}

// filterNotToParallelly: Filter elements not matching predicate to destination in parallel / 并行过滤不匹配谓词的元素到目标集合
suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.filterNotToParallelly(
    destination: C,
    crossinline predicate: SuspendPredicate<T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        for (element in this@filterNotToParallelly.iterator()) {
            promises.add(async(Dispatchers.Default) {
                if (predicate(element)) {
                    null
                } else {
                    element
                }
            })
        }
        promises.mapNotNullTo(destination) { it.await() }
    }
}

// tryFilterNotToParallelly: Try version of filterNotToParallelly / filterNotToParallelly 的 try 版本
suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.tryFilterNotToParallelly(
    destination: C,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<T?>>>()
            for (element in this@tryFilterNotToParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    when (val ret = predicate(element)) {
                        is Ok -> {
                            if (ret.value) {
                                Ok(null)
                            } else {
                                Ok(element)
                            }
                        }

                        is Failed -> {
                            Failed(ret.error)
                        }
                    }
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

// filterIndexedParallelly: Filter elements with index in parallel / 并行带索引过滤元素
suspend inline fun <T : Any> Iterable<T>.filterIndexedParallelly(
    crossinline predicate: SuspendIndexedPredicate<T>
): List<T> {
    return this.filterIndexedToParallelly(ArrayList(), predicate)
}

// tryFilterIndexedParallelly: Try version of filterIndexedParallelly / filterIndexedParallelly 的 try 版本
suspend inline fun <T : Any> Iterable<T>.tryFilterIndexedParallelly(
    crossinline predicate: SuspendTryIndexedPredicate<T>
): Ret<List<T>> {
    return this.tryFilterIndexedToParallelly(ArrayList(), predicate)
}

// filterIndexedToParallelly: Filter elements with index to destination in parallel / 并行带索引过滤元素到目标集合
suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.filterIndexedToParallelly(
    destination: C,
    crossinline predicate: SuspendIndexedPredicate<T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        for ((index, element) in this@filterIndexedToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) {
                if (predicate(index, element)) {
                    element
                } else {
                    null
                }
            })
        }
        promises.mapNotNullTo(destination) { it.await() }
    }
}

// tryFilterIndexedToParallelly: Try version of filterIndexedToParallelly / filterIndexedToParallelly 的 try 版本
suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.tryFilterIndexedToParallelly(
    destination: C,
    crossinline predicate: SuspendTryIndexedPredicate<T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<T?>>>()
            for ((index, element) in this@tryFilterIndexedToParallelly.withIndex()) {
                promises.add(async(Dispatchers.Default) {
                    when (val ret = predicate(index, element)) {
                        is Ok -> {
                            if (ret.value) {
                                Ok(element)
                            } else {
                                Ok(null)
                            }
                        }

                        is Failed -> {
                            Failed(ret.error)
                        }
                    }
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

// filterIsInstanceParallelly: Filter instances of specified type in parallel / 并行过滤指定类型的实例
suspend inline fun <reified U : Any, T> Iterable<T>.filterIsInstanceParallelly(
    crossinline predicate: SuspendPredicate<U>
): List<U> {
    return this.filterIsInstanceToParallelly(ArrayList(), predicate)
}

// tryFilterIsInstanceParallelly: Try version of filterIsInstanceParallelly / filterIsInstanceParallelly 的 try 版本
suspend inline fun <reified U : Any, T> Iterable<T>.tryFilterIsInstanceParallelly(
    crossinline predicate: SuspendTryPredicate<U>
): Ret<List<U>> {
    return this.tryFilterIsInstanceToParallelly(ArrayList(), predicate)
}

// filterIsInstanceToParallelly: Filter instances of specified type to destination in parallel / 并行过滤指定类型的实例到目标集合
suspend inline fun <reified U : Any, T, C : MutableCollection<U>> Iterable<T>.filterIsInstanceToParallelly(
    destination: C,
    crossinline predicate: SuspendPredicate<U>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<U?>>()
        for (element in this@filterIsInstanceToParallelly.iterator()) {
            if (element is U) {
                promises.add(async(Dispatchers.Default) {
                    if (predicate(element)) {
                        element
                    } else {
                        null
                    }
                })
            }
        }
        promises.mapNotNullTo(destination) { it.await() }
    }
}

// tryFilterIsInstanceToParallelly: Try version of filterIsInstanceToParallelly / filterIsInstanceToParallelly 的 try 版本
suspend inline fun <reified U : Any, T, C : MutableCollection<U>> Iterable<T>.tryFilterIsInstanceToParallelly(
    destination: C,
    crossinline predicate: SuspendTryPredicate<U>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<U?>>>()
            for (element in this@tryFilterIsInstanceToParallelly.iterator()) {
                if (element is U) {
                    promises.add(async(Dispatchers.Default) {
                        when (val ret = predicate(element)) {
                            is Ok -> {
                                if (ret.value) {
                                    Ok(element)
                                } else {
                                    Ok(null)
                                }
                            }

                            is Failed -> {
                                Failed(ret.error)
                            }
                        }
                    })
                }
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
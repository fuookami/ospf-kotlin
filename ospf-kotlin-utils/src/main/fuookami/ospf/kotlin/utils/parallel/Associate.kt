package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.error.*

// associateParallelly: Associate elements to pairs in parallel / 并行关联元素为键值对
suspend inline fun <K, V, T> Iterable<T>.associateParallelly(
    crossinline extractor: SuspendExtractor<Pair<K, V>, T>
): Map<K, V> {
    return this.associateToParallelly(LinkedHashMap(), extractor)
}

// tryAssociateToParallelly: Try version of associateParallelly / associateParallelly 的 try 版本
suspend inline fun <K, V, T> Iterable<T>.tryAssociateToParallelly(
    crossinline extractor: SuspendTryExtractor<Pair<K, V>, T>
): Ret<Map<K, V>> {
    return this.tryAssociateToParallelly(LinkedHashMap(), extractor)
}

// associateToParallelly: Associate elements to pairs to destination in parallel / 并行关联元素为键值对到目标 Map
suspend inline fun <K, V, T, M : MutableMap<in K, in V>> Iterable<T>.associateToParallelly(
    destination: M,
    crossinline extractor: SuspendExtractor<Pair<K, V>, T>
): M {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<K, V>>>()
        for (element in this@associateToParallelly.iterator()) {
            promises.add(async(Dispatchers.Default) {
                extractor(element)
            })
        }
        promises.associateTo(destination) { it.await() }
    }
}

// tryAssociateToParallelly: Try version of associateToParallelly / associateToParallelly 的 try 版本
suspend inline fun <K, V, T, M : MutableMap<in K, in V>> Iterable<T>.tryAssociateToParallelly(
    destination: M,
    crossinline extractor: SuspendTryExtractor<Pair<K, V>, T>
): Ret<M> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<Pair<K, V>>>>()
            for (element in this@tryAssociateToParallelly.iterator()) {
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
            destination.putAll(result)
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}

// associateByParallelly: Associate elements by key in parallel / 并行按键关联元素
suspend inline fun <K, T> Iterable<T>.associateByParallelly(
    crossinline keyExtractor: SuspendExtractor<K, T>
): Map<K, T> {
    return this.associateByToParallelly(LinkedHashMap(), keyExtractor)
}

// tryAssociateByToParallelly: Try version of associateByParallelly / associateByParallelly 的 try 版本
suspend inline fun <K, T> Iterable<T>.tryAssociateByToParallelly(
    crossinline keyExtractor: SuspendTryExtractor<K, T>
): Ret<Map<K, T>> {
    return this.tryAssociateByToParallelly(LinkedHashMap(), keyExtractor)
}

// associateByToParallelly: Associate elements by key to destination in parallel / 并行按键关联元素到目标 Map
suspend inline fun <K, T, M : MutableMap<in K, in T>> Iterable<T>.associateByToParallelly(
    destination: M,
    crossinline keyExtractor: SuspendExtractor<K, T>
): M {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<K, T>>>()
        for (element in this@associateByToParallelly.iterator()) {
            promises.add(async(Dispatchers.Default) {
                keyExtractor(element) to element
            })
        }
        promises.associateTo(destination) { it.await() }
    }
}

// tryAssociateByToParallelly: Try version of associateByToParallelly / associateByToParallelly 的 try 版本
suspend inline fun <K, T, M : MutableMap<in K, in T>> Iterable<T>.tryAssociateByToParallelly(
    destination: M,
    crossinline keyExtractor: SuspendTryExtractor<K, T>
): Ret<M> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<Pair<K, T>>>>()
            for (element in this@tryAssociateByToParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    keyExtractor(element).map {
                        it to element
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
            destination.putAll(result)
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}

// associateWithParallelly: Associate elements with value in parallel / 并行用值关联元素
suspend inline fun <V, T> Iterable<T>.associateWithParallelly(
    crossinline valueExtractor: SuspendExtractor<V, T>
): Map<T, V> {
    return this.associateWithToParallelly(LinkedHashMap(), valueExtractor)
}

// tryAssociateWithToParallelly: Try version of associateWithParallelly / associateWithParallelly 的 try 版本
suspend inline fun <V, T> Iterable<T>.tryAssociateWithToParallelly(
    crossinline valueExtractor: SuspendTryExtractor<V, T>
): Ret<Map<T, V>> {
    return this.tryAssociateWithToParallelly(LinkedHashMap(), valueExtractor)
}

// associateWithToParallelly: Associate elements with value to destination in parallel / 并行用值关联元素到目标 Map
suspend inline fun <V, T, M : MutableMap<in T, in V>> Iterable<T>.associateWithToParallelly(
    destination: M,
    crossinline valueExtractor: SuspendExtractor<V, T>
): M {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, V>>>()
        for (element in this@associateWithToParallelly.iterator()) {
            promises.add(async(Dispatchers.Default) {
                element to valueExtractor(element)
            })
        }
        promises.associateTo(destination) { it.await() }
    }
}

// tryAssociateWithToParallelly: Try version of associateWithToParallelly / associateWithToParallelly 的 try 版本
suspend inline fun <V, T, M : MutableMap<in T, in V>> Iterable<T>.tryAssociateWithToParallelly(
    destination: M,
    crossinline valueExtractor: SuspendTryExtractor<V, T>
): Ret<M> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<Pair<T, V>>>>()
            for (element in this@tryAssociateWithToParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    valueExtractor(element).map {
                        element to it
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
            destination.putAll(result)
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}
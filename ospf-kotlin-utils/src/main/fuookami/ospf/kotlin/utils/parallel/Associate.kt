package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.ExRet
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.SuspendExtractor
import fuookami.ospf.kotlin.utils.functional.SuspendTryExtractor

/**
 * 并行关联操作
 *
 * Parallel association operations for creating maps from iterables.
 *
 * UTL-005 TODO: 添加 concurrentAmount 参数控制并发上限
 * UTL-005 TODO: Add concurrentAmount parameter for concurrency control.
 */

suspend inline fun <K, V, T> Iterable<T>.associateParallelly(
    crossinline extractor: SuspendExtractor<Pair<K, V>, T>
): Map<K, V> {
    return associateToParallelly(LinkedHashMap(), extractor)
}

suspend inline fun <K, V, T> Iterable<T>.tryAssociateToParallelly(
    crossinline extractor: SuspendTryExtractor<Pair<K, V>, T>
): Ret<Map<K, V>> {
    return tryAssociateToParallelly(LinkedHashMap(), extractor)
}

suspend inline fun <K, V, T> Iterable<T>.exTryAssociateToParallelly(
    crossinline extractor: SuspendTryExtractor<Pair<K, V>, T>
): ExRet<Map<K, V>> {
    return exTryAssociateToParallelly(LinkedHashMap(), extractor)
}

suspend inline fun <K, V, T, M : MutableMap<in K, in V>> Iterable<T>.associateToParallelly(
    destination: M,
    crossinline extractor: SuspendExtractor<Pair<K, V>, T>
): M {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<K, V>>>()
        for (element in this@associateToParallelly) {
            promises.add(async(Dispatchers.Default) { extractor(element) })
        }
        for (promise in promises) {
            val (key, value) = promise.await()
            destination[key] = value
        }
        destination
    }
}

suspend inline fun <K, V, T, M : MutableMap<in K, in V>> Iterable<T>.tryAssociateToParallelly(
    destination: M,
    crossinline extractor: SuspendTryExtractor<Pair<K, V>, T>
): Ret<M> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<K, V>>>>()
        for (element in this@tryAssociateToParallelly) {
            promises.add(async(Dispatchers.Default) { extractor(element) })
        }
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> {
                    val (key, value) = ret.value
                    destination[key] = value
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}

suspend inline fun <K, V, T, M : MutableMap<in K, in V>> Iterable<T>.exTryAssociateToParallelly(
    destination: M,
    crossinline extractor: SuspendTryExtractor<Pair<K, V>, T>
): ExRet<M> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<K, V>>>>()
        for (element in this@exTryAssociateToParallelly) {
            promises.add(async(Dispatchers.Default) { extractor(element) })
        }
        val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error>()
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> {
                    val (key, value) = ret.value
                    destination[key] = value
                }

                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(destination, errors)
    }
}

suspend inline fun <K, T> Iterable<T>.associateByParallelly(
    crossinline keyExtractor: SuspendExtractor<K, T>
): Map<K, T> {
    return associateByToParallelly(LinkedHashMap(), keyExtractor)
}

suspend inline fun <K, T> Iterable<T>.tryAssociateByToParallelly(
    crossinline keyExtractor: SuspendTryExtractor<K, T>
): Ret<Map<K, T>> {
    return tryAssociateByToParallelly(LinkedHashMap(), keyExtractor)
}

suspend inline fun <K, T> Iterable<T>.exTryAssociateByToParallelly(
    crossinline keyExtractor: SuspendTryExtractor<K, T>
): ExRet<Map<K, T>> {
    return exTryAssociateByToParallelly(LinkedHashMap(), keyExtractor)
}

suspend inline fun <K, T, M : MutableMap<in K, in T>> Iterable<T>.associateByToParallelly(
    destination: M,
    crossinline keyExtractor: SuspendExtractor<K, T>
): M {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<K, T>>>()
        for (element in this@associateByToParallelly) {
            promises.add(async(Dispatchers.Default) { keyExtractor(element) to element })
        }
        for (promise in promises) {
            val (key, value) = promise.await()
            destination[key] = value
        }
        destination
    }
}

suspend inline fun <K, T, M : MutableMap<in K, in T>> Iterable<T>.tryAssociateByToParallelly(
    destination: M,
    crossinline keyExtractor: SuspendTryExtractor<K, T>
): Ret<M> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<K, T>>>>()
        for (element in this@tryAssociateByToParallelly) {
            promises.add(async(Dispatchers.Default) {
                when (val ret = keyExtractor(element)) {
                    is Ok -> Ok(ret.value to element)
                    is Failed -> Failed(ret.error)
                    is Fatal -> Fatal(ret.errors)
                }
            })
        }
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> {
                    val (key, value) = ret.value
                    destination[key] = value
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}

suspend inline fun <K, T, M : MutableMap<in K, in T>> Iterable<T>.exTryAssociateByToParallelly(
    destination: M,
    crossinline keyExtractor: SuspendTryExtractor<K, T>
): ExRet<M> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<K, T>>>>()
        for (element in this@exTryAssociateByToParallelly) {
            promises.add(async(Dispatchers.Default) {
                when (val ret = keyExtractor(element)) {
                    is Ok -> Ok(ret.value to element)
                    is Failed -> Failed(ret.error)
                    is Fatal -> Fatal(ret.errors)
                }
            })
        }
        val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error>()
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> {
                    val (key, value) = ret.value
                    destination[key] = value
                }

                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(destination, errors)
    }
}

suspend inline fun <V, T> Iterable<T>.associateWithParallelly(
    crossinline valueExtractor: SuspendExtractor<V, T>
): Map<T, V> {
    return associateWithToParallelly(LinkedHashMap(), valueExtractor)
}

suspend inline fun <V, T> Iterable<T>.tryAssociateWithToParallelly(
    crossinline valueExtractor: SuspendTryExtractor<V, T>
): Ret<Map<T, V>> {
    return tryAssociateWithToParallelly(LinkedHashMap(), valueExtractor)
}

suspend inline fun <V, T> Iterable<T>.exTryAssociateWithToParallelly(
    crossinline valueExtractor: SuspendTryExtractor<V, T>
): ExRet<Map<T, V>> {
    return exTryAssociateWithToParallelly(LinkedHashMap(), valueExtractor)
}

suspend inline fun <V, T, M : MutableMap<in T, in V>> Iterable<T>.associateWithToParallelly(
    destination: M,
    crossinline valueExtractor: SuspendExtractor<V, T>
): M {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, V>>>()
        for (element in this@associateWithToParallelly) {
            promises.add(async(Dispatchers.Default) { element to valueExtractor(element) })
        }
        for (promise in promises) {
            val (key, value) = promise.await()
            destination[key] = value
        }
        destination
    }
}

suspend inline fun <V, T, M : MutableMap<in T, in V>> Iterable<T>.tryAssociateWithToParallelly(
    destination: M,
    crossinline valueExtractor: SuspendTryExtractor<V, T>
): Ret<M> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<T, V>>>>()
        for (element in this@tryAssociateWithToParallelly) {
            promises.add(async(Dispatchers.Default) {
                when (val ret = valueExtractor(element)) {
                    is Ok -> Ok(element to ret.value)
                    is Failed -> Failed(ret.error)
                    is Fatal -> Fatal(ret.errors)
                }
            })
        }
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> {
                    val (key, value) = ret.value
                    destination[key] = value
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}

suspend inline fun <V, T, M : MutableMap<in T, in V>> Iterable<T>.exTryAssociateWithToParallelly(
    destination: M,
    crossinline valueExtractor: SuspendTryExtractor<V, T>
): ExRet<M> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<T, V>>>>()
        for (element in this@exTryAssociateWithToParallelly) {
            promises.add(async(Dispatchers.Default) {
                when (val ret = valueExtractor(element)) {
                    is Ok -> Ok(element to ret.value)
                    is Failed -> Failed(ret.error)
                    is Fatal -> Fatal(ret.errors)
                }
            })
        }
        val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error>()
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> {
                    val (key, value) = ret.value
                    destination[key] = value
                }

                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(destination, errors)
    }
}

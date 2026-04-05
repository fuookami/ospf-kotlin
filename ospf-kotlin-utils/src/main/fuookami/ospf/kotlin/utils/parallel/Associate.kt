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
 * Parallel association operations for creating maps from iterables with concurrency control.
 *
 * 并发控制已实现：使用 Semaphore 限制同时活跃的协程数量。
 * Concurrency control implemented: Uses Semaphore to limit active coroutines.
 */

suspend inline fun <K, V, T> Iterable<T>.associateParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendExtractor<Pair<K, V>, T>
): Map<K, V> {
    return associateToParallelly(LinkedHashMap(), concurrentAmount, extractor)
}

suspend inline fun <K, V, T> Iterable<T>.tryAssociateToParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<Pair<K, V>, T>
): Ret<Map<K, V>> {
    return tryAssociateToParallelly(LinkedHashMap(), concurrentAmount, extractor)
}

suspend inline fun <K, V, T> Iterable<T>.exTryAssociateToParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<Pair<K, V>, T>
): ExRet<Map<K, V>> {
    return exTryAssociateToParallelly(LinkedHashMap(), concurrentAmount, extractor)
}

suspend inline fun <K, V, T, M : MutableMap<in K, in V>> Iterable<T>.associateToParallelly(
    destination: M,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendExtractor<Pair<K, V>, T>
): M {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<K, V>>>()
        for (element in this@associateToParallelly) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    extractor(element)
                } finally {
                    semaphore.release()
                }
            })
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
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<Pair<K, V>, T>
): Ret<M> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<K, V>>>>()
        for (element in this@tryAssociateToParallelly) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    extractor(element)
                } finally {
                    semaphore.release()
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

suspend inline fun <K, V, T, M : MutableMap<in K, in V>> Iterable<T>.exTryAssociateToParallelly(
    destination: M,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<Pair<K, V>, T>
): ExRet<M> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<K, V>>>>()
        for (element in this@exTryAssociateToParallelly) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    extractor(element)
                } finally {
                    semaphore.release()
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

suspend inline fun <K, T> Iterable<T>.associateByParallelly(
    concurrentAmount: ULong? = null,
    crossinline keyExtractor: SuspendExtractor<K, T>
): Map<K, T> {
    return associateByToParallelly(LinkedHashMap(), concurrentAmount, keyExtractor)
}

suspend inline fun <K, T> Iterable<T>.tryAssociateByToParallelly(
    concurrentAmount: ULong? = null,
    crossinline keyExtractor: SuspendTryExtractor<K, T>
): Ret<Map<K, T>> {
    return tryAssociateByToParallelly(LinkedHashMap(), concurrentAmount, keyExtractor)
}

suspend inline fun <K, T> Iterable<T>.exTryAssociateByToParallelly(
    concurrentAmount: ULong? = null,
    crossinline keyExtractor: SuspendTryExtractor<K, T>
): ExRet<Map<K, T>> {
    return exTryAssociateByToParallelly(LinkedHashMap(), concurrentAmount, keyExtractor)
}

suspend inline fun <K, T, M : MutableMap<in K, in T>> Iterable<T>.associateByToParallelly(
    destination: M,
    concurrentAmount: ULong? = null,
    crossinline keyExtractor: SuspendExtractor<K, T>
): M {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<K, T>>>()
        for (element in this@associateByToParallelly) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    keyExtractor(element) to element
                } finally {
                    semaphore.release()
                }
            })
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
    concurrentAmount: ULong? = null,
    crossinline keyExtractor: SuspendTryExtractor<K, T>
): Ret<M> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<K, T>>>>()
        for (element in this@tryAssociateByToParallelly) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    when (val ret = keyExtractor(element)) {
                        is Ok -> Ok(ret.value to element)
                        is Failed -> Failed(ret.error)
                        is Fatal -> Fatal(ret.errors)
                    }
                } finally {
                    semaphore.release()
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
    concurrentAmount: ULong? = null,
    crossinline keyExtractor: SuspendTryExtractor<K, T>
): ExRet<M> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<K, T>>>>()
        for (element in this@exTryAssociateByToParallelly) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    when (val ret = keyExtractor(element)) {
                        is Ok -> Ok(ret.value to element)
                        is Failed -> Failed(ret.error)
                        is Fatal -> Fatal(ret.errors)
                    }
                } finally {
                    semaphore.release()
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
    concurrentAmount: ULong? = null,
    crossinline valueExtractor: SuspendExtractor<V, T>
): Map<T, V> {
    return associateWithToParallelly(LinkedHashMap(), concurrentAmount, valueExtractor)
}

suspend inline fun <V, T> Iterable<T>.tryAssociateWithToParallelly(
    concurrentAmount: ULong? = null,
    crossinline valueExtractor: SuspendTryExtractor<V, T>
): Ret<Map<T, V>> {
    return tryAssociateWithToParallelly(LinkedHashMap(), concurrentAmount, valueExtractor)
}

suspend inline fun <V, T> Iterable<T>.exTryAssociateWithToParallelly(
    concurrentAmount: ULong? = null,
    crossinline valueExtractor: SuspendTryExtractor<V, T>
): ExRet<Map<T, V>> {
    return exTryAssociateWithToParallelly(LinkedHashMap(), concurrentAmount, valueExtractor)
}

suspend inline fun <V, T, M : MutableMap<in T, in V>> Iterable<T>.associateWithToParallelly(
    destination: M,
    concurrentAmount: ULong? = null,
    crossinline valueExtractor: SuspendExtractor<V, T>
): M {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, V>>>()
        for (element in this@associateWithToParallelly) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    element to valueExtractor(element)
                } finally {
                    semaphore.release()
                }
            })
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
    concurrentAmount: ULong? = null,
    crossinline valueExtractor: SuspendTryExtractor<V, T>
): Ret<M> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<T, V>>>>()
        for (element in this@tryAssociateWithToParallelly) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    when (val ret = valueExtractor(element)) {
                        is Ok -> Ok(element to ret.value)
                        is Failed -> Failed(ret.error)
                        is Fatal -> Fatal(ret.errors)
                    }
                } finally {
                    semaphore.release()
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
    concurrentAmount: ULong? = null,
    crossinline valueExtractor: SuspendTryExtractor<V, T>
): ExRet<M> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<T, V>>>>()
        for (element in this@exTryAssociateWithToParallelly) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    when (val ret = valueExtractor(element)) {
                        is Ok -> Ok(element to ret.value)
                        is Failed -> Failed(ret.error)
                        is Fatal -> Fatal(ret.errors)
                    }
                } finally {
                    semaphore.release()
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
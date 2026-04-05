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
import fuookami.ospf.kotlin.utils.functional.SuspendIndexedExtractor
import fuookami.ospf.kotlin.utils.functional.SuspendTryExtractor
import fuookami.ospf.kotlin.utils.functional.SuspendTryIndexedExtractor

/**
 * 并行展平映射操作
 *
 * Parallel flat-map operations with concurrency control.
 *
 * 并发控制已实现：使用 Semaphore 限制同时活跃的协程数量。
 * Concurrency control implemented: Uses Semaphore to limit active coroutines.
 */

suspend inline fun <R, T> Iterable<T>.flatMapParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendExtractor<Iterable<R>, T>
): List<R> {
    return flatMapToParallelly(ArrayList(), concurrentAmount, extractor)
}

suspend inline fun <R, T> Iterable<T>.tryFlatMapToParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<Iterable<R>, T>
): Ret<List<R>> {
    return tryFlatMapToParallelly(ArrayList(), concurrentAmount, extractor)
}

suspend inline fun <R, T> Iterable<T>.exTryFlatMapToParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<Iterable<R>, T>
): ExRet<List<R>> {
    return exTryFlatMapToParallelly(ArrayList(), concurrentAmount, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendExtractor<Iterable<R>, T>
): C {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Iterable<R>>>()
        for (element in this@flatMapToParallelly) {
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
            destination.addAll(promise.await())
        }
        destination
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<Iterable<R>, T>
): Ret<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Iterable<R>>>>()
        for (element in this@tryFlatMapToParallelly) {
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
                is Ok -> destination.addAll(ret.value)
                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.exTryFlatMapToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<Iterable<R>, T>
): ExRet<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Iterable<R>>>>()
        for (element in this@exTryFlatMapToParallelly) {
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
                is Ok -> destination.addAll(ret.value)
                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(destination, errors)
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendIndexedExtractor<Iterable<R>, T>
): List<R> {
    return flatMapIndexedToParallelly(ArrayList(), concurrentAmount, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapIndexedToParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R>, T>
): Ret<List<R>> {
    return tryFlatMapIndexedToParallelly(ArrayList(), concurrentAmount, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.exTryFlatMapIndexedToParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R>, T>
): ExRet<List<R>> {
    return exTryFlatMapIndexedToParallelly(ArrayList(), concurrentAmount, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendIndexedExtractor<Iterable<R>, T>
): C {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Iterable<R>>>()
        for ((index, element) in this@flatMapIndexedToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    extractor(index, element)
                } finally {
                    semaphore.release()
                }
            })
        }
        for (promise in promises) {
            destination.addAll(promise.await())
        }
        destination
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapIndexedToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R>, T>
): Ret<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Iterable<R>>>>()
        for ((index, element) in this@tryFlatMapIndexedToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    extractor(index, element)
                } finally {
                    semaphore.release()
                }
            })
        }
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> destination.addAll(ret.value)
                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.exTryFlatMapIndexedToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R>, T>
): ExRet<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Iterable<R>>>>()
        for ((index, element) in this@exTryFlatMapIndexedToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    extractor(index, element)
                } finally {
                    semaphore.release()
                }
            })
        }
        val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error>()
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> destination.addAll(ret.value)
                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(destination, errors)
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedNotNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendIndexedExtractor<Iterable<R?>, T>
): List<R> {
    return flatMapIndexedNotNullToParallelly(ArrayList(), concurrentAmount, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapIndexedNotNullToParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R?>, T>
): Ret<List<R>> {
    return tryFlatMapIndexedNotNullToParallelly(ArrayList(), concurrentAmount, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.exTryFlatMapIndexedNotNullToParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R?>, T>
): ExRet<List<R>> {
    return exTryFlatMapIndexedNotNullToParallelly(ArrayList(), concurrentAmount, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedNotNullToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendIndexedExtractor<Iterable<R?>, T>
): C {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Iterable<R?>>>()
        for ((index, element) in this@flatMapIndexedNotNullToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    extractor(index, element)
                } finally {
                    semaphore.release()
                }
            })
        }
        for (promise in promises) {
            destination.addAll(promise.await().filterNotNull())
        }
        destination
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapIndexedNotNullToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R?>, T>
): Ret<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Iterable<R?>>>>()
        for ((index, element) in this@tryFlatMapIndexedNotNullToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    extractor(index, element)
                } finally {
                    semaphore.release()
                }
            })
        }
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> destination.addAll(ret.value.filterNotNull())
                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.exTryFlatMapIndexedNotNullToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R?>, T>
): ExRet<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Iterable<R?>>>>()
        for ((index, element) in this@exTryFlatMapIndexedNotNullToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    extractor(index, element)
                } finally {
                    semaphore.release()
                }
            })
        }
        val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error>()
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> destination.addAll(ret.value.filterNotNull())
                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(destination, errors)
    }
}
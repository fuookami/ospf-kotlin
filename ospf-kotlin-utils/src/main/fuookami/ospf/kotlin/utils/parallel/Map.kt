package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
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
 * 并行映射操作
 *
 * Parallel mapping operations with concurrency control.
 *
 * 并发控制已实现：使用 Semaphore 限制同时活跃的协程数量。
 * Concurrency control implemented: Uses Semaphore to limit active coroutines.
 */

/**
 * 并行映射集合元素
 *
 * Map collection elements in parallel with concurrency control.
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 提取器函数 / Extractor function
 * @return 映射后的列表 / Mapped list
 */
suspend inline fun <R : Any, T> Iterable<T>.mapParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendExtractor<R, T>
): List<R> {
    return mapToParallelly(ArrayList(), concurrentAmount, extractor)
}

/**
 * 并行映射集合元素（带错误处理）
 *
 * Map collection elements in parallel with error handling and concurrency control.
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 提取器函数 / Extractor function
 * @return 映射结果或错误 / Mapped result or error
 */
suspend inline fun <R, T> Iterable<T>.tryMapParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<List<R>> {
    return tryMapToParallelly(ArrayList(), concurrentAmount, extractor)
}

suspend inline fun <R, T> Iterable<T>.exTryMapParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R, T>
): ExRet<List<R>> {
    return exTryMapToParallelly(ArrayList(), concurrentAmount, extractor)
}

/**
 * 并行映射集合元素到目标集合
 *
 * Map collection elements in parallel to a destination collection with concurrency control.
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 提取器函数 / Extractor function
 * @return 目标集合 / Destination collection
 */
suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.mapToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendExtractor<R, T>
): C {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        for (element in this@mapToParallelly) {
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
            destination.add(promise.await())
        }
        destination
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryMapToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R>>>()
        for (element in this@tryMapToParallelly) {
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
                is Ok -> destination.add(ret.value)
                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.exTryMapToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R, T>
): ExRet<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R>>>()
        for (element in this@exTryMapToParallelly) {
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
                is Ok -> destination.add(ret.value)
                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(destination, errors)
    }
}

suspend inline fun <R : Any, T> Iterable<T>.mapNotNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendExtractor<R?, T>
): List<R> {
    return mapNotNullToParallelly(ArrayList(), concurrentAmount, extractor)
}

suspend inline fun <R : Any, T> Iterable<T>.tryMapNotNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<List<R>> {
    return tryMapNotNullToParallelly(ArrayList(), concurrentAmount, extractor)
}

suspend inline fun <R : Any, T> Iterable<T>.exTryMapNotNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): ExRet<List<R>> {
    return exTryMapNotNullToParallelly(ArrayList(), concurrentAmount, extractor)
}

suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.mapNotNullToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendExtractor<R?, T>
): C {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        for (element in this@mapNotNullToParallelly) {
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
            promise.await()?.let { destination.add(it) }
        }
        destination
    }
}

suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.tryMapNotNullToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R?>>>()
        for (element in this@tryMapNotNullToParallelly) {
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
                is Ok -> ret.value?.let { destination.add(it) }
                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}

suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.exTryMapNotNullToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): ExRet<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R?>>>()
        for (element in this@exTryMapNotNullToParallelly) {
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
                is Ok -> ret.value?.let { destination.add(it) }
                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(destination, errors)
    }
}

suspend inline fun <R, T> Iterable<T>.mapIndexedParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendIndexedExtractor<R, T>
): List<R> {
    return mapIndexedToParallelly(ArrayList(), concurrentAmount, extractor)
}

suspend inline fun <R, T> Iterable<T>.tryMapIndexedParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<R, T>
): Ret<List<R>> {
    return tryMapIndexedToParallelly(ArrayList(), concurrentAmount, extractor)
}

suspend inline fun <R, T> Iterable<T>.exTryMapIndexedParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<R, T>
): ExRet<List<R>> {
    return exTryMapIndexedToParallelly(ArrayList(), concurrentAmount, extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.mapIndexedToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendIndexedExtractor<R, T>
): C {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        for ((index, element) in this@mapIndexedToParallelly.withIndex()) {
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
            destination.add(promise.await())
        }
        destination
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryMapIndexedToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<R, T>
): Ret<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R>>>()
        for ((index, element) in this@tryMapIndexedToParallelly.withIndex()) {
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
                is Ok -> destination.add(ret.value)
                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.exTryMapIndexedToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<R, T>
): ExRet<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R>>>()
        for ((index, element) in this@exTryMapIndexedToParallelly.withIndex()) {
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
                is Ok -> destination.add(ret.value)
                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(destination, errors)
    }
}

suspend inline fun <R : Any, T> Iterable<T>.mapIndexedNotNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendIndexedExtractor<R?, T>
): List<R> {
    return mapIndexedNotNullToParallelly(ArrayList(), concurrentAmount, extractor)
}

suspend inline fun <R : Any, T> Iterable<T>.tryMapIndexedNotNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<R?, T>
): Ret<List<R>> {
    return tryMapIndexedNotNullToParallelly(ArrayList(), concurrentAmount, extractor)
}

suspend inline fun <R : Any, T> Iterable<T>.exTryMapIndexedNotNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<R?, T>
): ExRet<List<R>> {
    return exTryMapIndexedNotNullToParallelly(ArrayList(), concurrentAmount, extractor)
}

suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.mapIndexedNotNullToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendIndexedExtractor<R?, T>
): C {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        for ((index, element) in this@mapIndexedNotNullToParallelly.withIndex()) {
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
            promise.await()?.let { destination.add(it) }
        }
        destination
    }
}

suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.tryMapIndexedNotNullToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<R?, T>
): Ret<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R?>>>()
        for ((index, element) in this@tryMapIndexedNotNullToParallelly.withIndex()) {
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
                is Ok -> ret.value?.let { destination.add(it) }
                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}

suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.exTryMapIndexedNotNullToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<R?, T>
): ExRet<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R?>>>()
        for ((index, element) in this@exTryMapIndexedNotNullToParallelly.withIndex()) {
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
                is Ok -> ret.value?.let { destination.add(it) }
                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(destination, errors)
    }
}
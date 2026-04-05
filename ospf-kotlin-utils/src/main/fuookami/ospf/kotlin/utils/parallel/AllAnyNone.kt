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
import fuookami.ospf.kotlin.utils.functional.Warn
import fuookami.ospf.kotlin.utils.functional.SuspendPredicate
import fuookami.ospf.kotlin.utils.functional.SuspendTryPredicate

/**
 * 并行判断操作
 *
 * Parallel predicate operations (all, any, none) with concurrency control.
 *
 * 并发控制已实现：使用 Semaphore 限制同时活跃的协程数量。
 * Concurrency control implemented: Uses Semaphore to limit active coroutines.
 */

/**
 * 并行判断是否所有元素都满足条件
 *
 * Check if all elements satisfy the predicate in parallel with concurrency control.
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件 / Predicate function
 * @return 是否所有元素都满足条件 / Whether all elements satisfy the predicate
 */
suspend inline fun <T> Iterable<T>.allParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): Boolean {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Boolean>>()
        for (element in this@allParallelly) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    predicate(element)
                } finally {
                    semaphore.release()
                }
            })
        }
        promises.all { it.await() }
    }
}

suspend inline fun <T> Iterable<T>.tryAllParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Boolean> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Boolean>>>()
        for (element in this@tryAllParallelly) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    predicate(element)
                } finally {
                    semaphore.release()
                }
            })
        }
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> {
                    if (!ret.value) {
                        return@coroutineScope Ok(false)
                    }
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(true)
    }
}

suspend inline fun <T> Iterable<T>.exTryAllParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<Boolean> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Boolean>>>()
        for (element in this@exTryAllParallelly) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    predicate(element)
                } finally {
                    semaphore.release()
                }
            })
        }
        val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error>()
        var all = true
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> if (!ret.value) {
                    all = false
                }

                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(all, errors)
    }
}

suspend inline fun <T> Iterable<T>.anyParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): Boolean {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Boolean>>()
        for (element in this@anyParallelly) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    predicate(element)
                } finally {
                    semaphore.release()
                }
            })
        }
        promises.any { it.await() }
    }
}

suspend inline fun <T> Iterable<T>.tryAnyParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Boolean> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Boolean>>>()
        for (element in this@tryAnyParallelly) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    predicate(element)
                } finally {
                    semaphore.release()
                }
            })
        }
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> {
                    if (ret.value) {
                        return@coroutineScope Ok(true)
                    }
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(false)
    }
}

suspend inline fun <T> Iterable<T>.exTryAnyParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<Boolean> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Boolean>>>()
        for (element in this@exTryAnyParallelly) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    predicate(element)
                } finally {
                    semaphore.release()
                }
            })
        }
        val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error>()
        var any = false
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> if (ret.value) {
                    any = true
                }

                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(any, errors)
    }
}

suspend inline fun <T> Iterable<T>.noneParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): Boolean {
    return !anyParallelly(concurrentAmount, predicate)
}

suspend inline fun <T> Iterable<T>.tryNoneParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Boolean> {
    return when (val ret = tryAnyParallelly(concurrentAmount, predicate)) {
        is Ok -> Ok(!ret.value)
        is Failed -> Failed(ret.error)
        is Fatal -> Fatal(ret.errors)
    }
}

suspend inline fun <T> Iterable<T>.exTryNoneParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<Boolean> {
    return when (val ret = exTryAnyParallelly(concurrentAmount, predicate)) {
        is Ok -> Ok(!ret.value)
        is Failed -> Failed(ret.error)
        is Fatal -> Fatal(ret.errors)
        is Warn -> Warn(!ret.value, ret.warnings)
    }
}
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
import fuookami.ospf.kotlin.utils.functional.SuspendIndexedPredicate
import fuookami.ospf.kotlin.utils.functional.SuspendPredicate
import fuookami.ospf.kotlin.utils.functional.SuspendTryIndexedPredicate
import fuookami.ospf.kotlin.utils.functional.SuspendTryPredicate

/**
 * 并行过滤操作
 *
 * Parallel filtering operations with concurrency control.
 *
 * 并发控制已实现：使用 Semaphore 限制同时活跃的协程数量。
 * Concurrency control implemented: Uses Semaphore to limit active coroutines.
 */

/**
 * 并行过滤满足条件的元素
 *
 * Filter elements that satisfy the predicate in parallel with concurrency control.
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件 / Predicate function
 * @return 过滤后的列表 / Filtered list
 */
suspend inline fun <T : Any> Iterable<T>.filterParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): List<T> {
    return filterToParallelly(ArrayList(), concurrentAmount, predicate)
}

suspend inline fun <T : Any> Iterable<T>.tryFilterParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<List<T>> {
    return tryFilterToParallelly(ArrayList(), concurrentAmount, predicate)
}

suspend inline fun <T : Any> Iterable<T>.exTryFilterParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<List<T>> {
    return exTryFilterToParallelly(ArrayList(), concurrentAmount, predicate)
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.filterToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): C {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, Boolean>>>()
        for (element in this@filterToParallelly) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    element to predicate(element)
                } finally {
                    semaphore.release()
                }
            })
        }
        for (promise in promises) {
            val (element, keep) = promise.await()
            if (keep) {
                destination.add(element)
            }
        }
        destination
    }
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.tryFilterToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<T, Boolean>>>>()
        for (element in this@tryFilterToParallelly) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    when (val ret = predicate(element)) {
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
                    val (element, keep) = ret.value
                    if (keep) {
                        destination.add(element)
                    }
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.exTryFilterToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<T, Boolean>>>>()
        for (element in this@exTryFilterToParallelly) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    when (val ret = predicate(element)) {
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
                    val (element, keep) = ret.value
                    if (keep) {
                        destination.add(element)
                    }
                }

                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(destination, errors)
    }
}

suspend inline fun <T : Any> Iterable<T?>.filterNotNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): List<T> {
    return filterNotNullToParallelly(ArrayList(), concurrentAmount, predicate)
}

suspend inline fun <T : Any> Iterable<T?>.tryFilterNotNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<List<T>> {
    return tryFilterNotNullToParallelly(ArrayList(), concurrentAmount, predicate)
}

suspend inline fun <T : Any> Iterable<T?>.exTryFilterNotNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<List<T>> {
    return exTryFilterNotNullToParallelly(ArrayList(), concurrentAmount, predicate)
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T?>.filterNotNullToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): C {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, Boolean>>>()
        for (element in this@filterNotNullToParallelly) {
            if (element != null) {
                promises.add(async(Dispatchers.Default) {
                    semaphore.acquire()
                    try {
                        element to predicate(element)
                    } finally {
                        semaphore.release()
                    }
                })
            }
        }
        for (promise in promises) {
            val (element, keep) = promise.await()
            if (keep) {
                destination.add(element)
            }
        }
        destination
    }
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T?>.tryFilterNotNullToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<T, Boolean>>>>()
        for (element in this@tryFilterNotNullToParallelly) {
            if (element != null) {
                promises.add(async(Dispatchers.Default) {
                    semaphore.acquire()
                    try {
                        when (val ret = predicate(element)) {
                            is Ok -> Ok(element to ret.value)
                            is Failed -> Failed(ret.error)
                            is Fatal -> Fatal(ret.errors)
                        }
                    } finally {
                        semaphore.release()
                    }
                })
            }
        }
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> {
                    val (element, keep) = ret.value
                    if (keep) {
                        destination.add(element)
                    }
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T?>.exTryFilterNotNullToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<T, Boolean>>>>()
        for (element in this@exTryFilterNotNullToParallelly) {
            if (element != null) {
                promises.add(async(Dispatchers.Default) {
                    semaphore.acquire()
                    try {
                        when (val ret = predicate(element)) {
                            is Ok -> Ok(element to ret.value)
                            is Failed -> Failed(ret.error)
                            is Fatal -> Fatal(ret.errors)
                        }
                    } finally {
                        semaphore.release()
                    }
                })
            }
        }
        val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error>()
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> {
                    val (element, keep) = ret.value
                    if (keep) {
                        destination.add(element)
                    }
                }

                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(destination, errors)
    }
}

suspend inline fun <T : Any> Iterable<T>.filterNotParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): List<T> {
    return filterNotToParallelly(ArrayList(), concurrentAmount, predicate)
}

suspend inline fun <T : Any> Iterable<T>.tryFilterNotParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<List<T>> {
    return tryFilterNotToParallelly(ArrayList(), concurrentAmount, predicate)
}

suspend inline fun <T : Any> Iterable<T>.exTryFilterNotParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<List<T>> {
    return exTryFilterNotToParallelly(ArrayList(), concurrentAmount, predicate)
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.filterNotToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): C {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, Boolean>>>()
        for (element in this@filterNotToParallelly) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    element to !predicate(element)
                } finally {
                    semaphore.release()
                }
            })
        }
        for (promise in promises) {
            val (element, keep) = promise.await()
            if (keep) {
                destination.add(element)
            }
        }
        destination
    }
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.tryFilterNotToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<T, Boolean>>>>()
        for (element in this@tryFilterNotToParallelly) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    when (val ret = predicate(element)) {
                        is Ok -> Ok(element to !ret.value)
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
                    val (element, keep) = ret.value
                    if (keep) {
                        destination.add(element)
                    }
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.exTryFilterNotToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<T, Boolean>>>>()
        for (element in this@exTryFilterNotToParallelly) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    when (val ret = predicate(element)) {
                        is Ok -> Ok(element to !ret.value)
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
                    val (element, keep) = ret.value
                    if (keep) {
                        destination.add(element)
                    }
                }

                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(destination, errors)
    }
}

suspend inline fun <T : Any> Iterable<T>.filterIndexedParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendIndexedPredicate<T>
): List<T> {
    return filterIndexedToParallelly(ArrayList(), concurrentAmount, predicate)
}

suspend inline fun <T : Any> Iterable<T>.tryFilterIndexedParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryIndexedPredicate<T>
): Ret<List<T>> {
    return tryFilterIndexedToParallelly(ArrayList(), concurrentAmount, predicate)
}

suspend inline fun <T : Any> Iterable<T>.exTryFilterIndexedParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryIndexedPredicate<T>
): ExRet<List<T>> {
    return exTryFilterIndexedToParallelly(ArrayList(), concurrentAmount, predicate)
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.filterIndexedToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendIndexedPredicate<T>
): C {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, Boolean>>>()
        for ((index, element) in this@filterIndexedToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    element to predicate(index, element)
                } finally {
                    semaphore.release()
                }
            })
        }
        for (promise in promises) {
            val (element, keep) = promise.await()
            if (keep) {
                destination.add(element)
            }
        }
        destination
    }
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.tryFilterIndexedToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryIndexedPredicate<T>
): Ret<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<T, Boolean>>>>()
        for ((index, element) in this@tryFilterIndexedToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    when (val ret = predicate(index, element)) {
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
                    val (element, keep) = ret.value
                    if (keep) {
                        destination.add(element)
                    }
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.exTryFilterIndexedToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryIndexedPredicate<T>
): ExRet<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<T, Boolean>>>>()
        for ((index, element) in this@exTryFilterIndexedToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) {
                semaphore.acquire()
                try {
                    when (val ret = predicate(index, element)) {
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
                    val (element, keep) = ret.value
                    if (keep) {
                        destination.add(element)
                    }
                }

                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(destination, errors)
    }
}

suspend inline fun <reified U : Any, T> Iterable<T>.filterIsInstanceParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<U>
): List<U> {
    return filterIsInstanceToParallelly(ArrayList(), concurrentAmount, predicate)
}

suspend inline fun <reified U : Any, T> Iterable<T>.tryFilterIsInstanceParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<U>
): Ret<List<U>> {
    return tryFilterIsInstanceToParallelly(ArrayList(), concurrentAmount, predicate)
}

suspend inline fun <reified U : Any, T> Iterable<T>.exTryFilterIsInstanceParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<U>
): ExRet<List<U>> {
    return exTryFilterIsInstanceToParallelly(ArrayList(), concurrentAmount, predicate)
}

suspend inline fun <reified U : Any, T, C : MutableCollection<U>> Iterable<T>.filterIsInstanceToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<U>
): C {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<U, Boolean>>>()
        for (element in this@filterIsInstanceToParallelly) {
            if (element is U) {
                promises.add(async(Dispatchers.Default) {
                    semaphore.acquire()
                    try {
                        element to predicate(element)
                    } finally {
                        semaphore.release()
                    }
                })
            }
        }
        for (promise in promises) {
            val (element, keep) = promise.await()
            if (keep) {
                destination.add(element)
            }
        }
        destination
    }
}

suspend inline fun <reified U : Any, T, C : MutableCollection<U>> Iterable<T>.tryFilterIsInstanceToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<U>
): Ret<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<U, Boolean>>>>()
        for (element in this@tryFilterIsInstanceToParallelly) {
            if (element is U) {
                promises.add(async(Dispatchers.Default) {
                    semaphore.acquire()
                    try {
                        when (val ret = predicate(element)) {
                            is Ok -> Ok(element to ret.value)
                            is Failed -> Failed(ret.error)
                            is Fatal -> Fatal(ret.errors)
                        }
                    } finally {
                        semaphore.release()
                    }
                })
            }
        }
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> {
                    val (element, keep) = ret.value
                    if (keep) {
                        destination.add(element)
                    }
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}

suspend inline fun <reified U : Any, T, C : MutableCollection<U>> Iterable<T>.exTryFilterIsInstanceToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<U>
): ExRet<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val semaphore = createConcurrencySemaphore(limit)
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<U, Boolean>>>>()
        for (element in this@exTryFilterIsInstanceToParallelly) {
            if (element is U) {
                promises.add(async(Dispatchers.Default) {
                    semaphore.acquire()
                    try {
                        when (val ret = predicate(element)) {
                            is Ok -> Ok(element to ret.value)
                            is Failed -> Failed(ret.error)
                            is Fatal -> Fatal(ret.errors)
                        }
                    } finally {
                        semaphore.release()
                    }
                })
            }
        }
        val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error>()
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> {
                    val (element, keep) = ret.value
                    if (keep) {
                        destination.add(element)
                    }
                }

                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(destination, errors)
    }
}
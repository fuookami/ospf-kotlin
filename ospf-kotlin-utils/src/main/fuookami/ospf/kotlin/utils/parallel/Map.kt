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
 * 并行映射操作
 *
 * Parallel mapping operations.
 *
 * UTL-005 TODO: 添加 concurrentAmount 参数控制并发上限
 * UTL-005 TODO: Add concurrentAmount parameter for concurrency control.
 * 当前实现一次性为所有元素创建协程，对于大集合可能导致资源问题。
 * Current implementation creates coroutines for all elements at once, which may cause resource issues for large collections.
 */

/**
 * 并行映射集合元素
 *
 * Map collection elements in parallel.
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param extractor 提取器函数 / Extractor function
 * @return 映射后的列表 / Mapped list
 */
suspend inline fun <R : Any, T> Iterable<T>.mapParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): List<R> {
    return mapToParallelly(ArrayList(), extractor)
}

/**
 * 并行映射集合元素（带错误处理）
 *
 * Map collection elements in parallel with error handling.
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param extractor 提取器函数 / Extractor function
 * @return 映射结果或错误 / Mapped result or error
 */
suspend inline fun <R, T> Iterable<T>.tryMapParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<List<R>> {
    return tryMapToParallelly(ArrayList(), extractor)
}

suspend inline fun <R, T> Iterable<T>.exTryMapParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): ExRet<List<R>> {
    return exTryMapToParallelly(ArrayList(), extractor)
}

/**
 * 并行映射集合元素到目标集合
 *
 * Map collection elements in parallel to a destination collection.
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param extractor 提取器函数 / Extractor function
 * @return 目标集合 / Destination collection
 */
suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.mapToParallelly(
    destination: C,
    crossinline extractor: SuspendExtractor<R, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        for (element in this@mapToParallelly) {
            promises.add(async(Dispatchers.Default) { extractor(element) })
        }
        for (promise in promises) {
            destination.add(promise.await())
        }
        destination
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryMapToParallelly(
    destination: C,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<C> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R>>>()
        for (element in this@tryMapToParallelly) {
            promises.add(async(Dispatchers.Default) { extractor(element) })
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
    crossinline extractor: SuspendTryExtractor<R, T>
): ExRet<C> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R>>>()
        for (element in this@exTryMapToParallelly) {
            promises.add(async(Dispatchers.Default) { extractor(element) })
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
    crossinline extractor: SuspendExtractor<R?, T>
): List<R> {
    return mapNotNullToParallelly(ArrayList(), extractor)
}

suspend inline fun <R : Any, T> Iterable<T>.tryMapNotNullParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<List<R>> {
    return tryMapNotNullToParallelly(ArrayList(), extractor)
}

suspend inline fun <R : Any, T> Iterable<T>.exTryMapNotNullParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): ExRet<List<R>> {
    return exTryMapNotNullToParallelly(ArrayList(), extractor)
}

suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.mapNotNullToParallelly(
    destination: C,
    crossinline extractor: SuspendExtractor<R?, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        for (element in this@mapNotNullToParallelly) {
            promises.add(async(Dispatchers.Default) { extractor(element) })
        }
        for (promise in promises) {
            promise.await()?.let { destination.add(it) }
        }
        destination
    }
}

suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.tryMapNotNullToParallelly(
    destination: C,
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<C> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R?>>>()
        for (element in this@tryMapNotNullToParallelly) {
            promises.add(async(Dispatchers.Default) { extractor(element) })
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
    crossinline extractor: SuspendTryExtractor<R?, T>
): ExRet<C> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R?>>>()
        for (element in this@exTryMapNotNullToParallelly) {
            promises.add(async(Dispatchers.Default) { extractor(element) })
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
    crossinline extractor: SuspendIndexedExtractor<R, T>
): List<R> {
    return mapIndexedToParallelly(ArrayList(), extractor)
}

suspend inline fun <R, T> Iterable<T>.tryMapIndexedParallelly(
    crossinline extractor: SuspendTryIndexedExtractor<R, T>
): Ret<List<R>> {
    return tryMapIndexedToParallelly(ArrayList(), extractor)
}

suspend inline fun <R, T> Iterable<T>.exTryMapIndexedParallelly(
    crossinline extractor: SuspendTryIndexedExtractor<R, T>
): ExRet<List<R>> {
    return exTryMapIndexedToParallelly(ArrayList(), extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.mapIndexedToParallelly(
    destination: C,
    crossinline extractor: SuspendIndexedExtractor<R, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        for ((index, element) in this@mapIndexedToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) { extractor(index, element) })
        }
        for (promise in promises) {
            destination.add(promise.await())
        }
        destination
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryMapIndexedToParallelly(
    destination: C,
    crossinline extractor: SuspendTryIndexedExtractor<R, T>
): Ret<C> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R>>>()
        for ((index, element) in this@tryMapIndexedToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) { extractor(index, element) })
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
    crossinline extractor: SuspendTryIndexedExtractor<R, T>
): ExRet<C> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R>>>()
        for ((index, element) in this@exTryMapIndexedToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) { extractor(index, element) })
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
    crossinline extractor: SuspendIndexedExtractor<R?, T>
): List<R> {
    return mapIndexedNotNullToParallelly(ArrayList(), extractor)
}

suspend inline fun <R : Any, T> Iterable<T>.tryMapIndexedNotNullParallelly(
    crossinline extractor: SuspendTryIndexedExtractor<R?, T>
): Ret<List<R>> {
    return tryMapIndexedNotNullToParallelly(ArrayList(), extractor)
}

suspend inline fun <R : Any, T> Iterable<T>.exTryMapIndexedNotNullParallelly(
    crossinline extractor: SuspendTryIndexedExtractor<R?, T>
): ExRet<List<R>> {
    return exTryMapIndexedNotNullToParallelly(ArrayList(), extractor)
}

suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.mapIndexedNotNullToParallelly(
    destination: C,
    crossinline extractor: SuspendIndexedExtractor<R?, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        for ((index, element) in this@mapIndexedNotNullToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) { extractor(index, element) })
        }
        for (promise in promises) {
            promise.await()?.let { destination.add(it) }
        }
        destination
    }
}

suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.tryMapIndexedNotNullToParallelly(
    destination: C,
    crossinline extractor: SuspendTryIndexedExtractor<R?, T>
): Ret<C> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R?>>>()
        for ((index, element) in this@tryMapIndexedNotNullToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) { extractor(index, element) })
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
    crossinline extractor: SuspendTryIndexedExtractor<R?, T>
): ExRet<C> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<R?>>>()
        for ((index, element) in this@exTryMapIndexedNotNullToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) { extractor(index, element) })
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

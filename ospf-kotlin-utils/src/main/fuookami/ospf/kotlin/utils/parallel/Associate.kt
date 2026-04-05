package fuookami.ospf.kotlin.utils.parallel

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.ExRet
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Warn
import fuookami.ospf.kotlin.utils.functional.SuspendExtractor
import fuookami.ospf.kotlin.utils.functional.SuspendTryExtractor

/**
 * 并行关联操作
 *
 * Parallel association operations for creating maps from iterables with concurrency control.
 *
 * RVW-009 改进：使用 Worker Pool 方案实现真正的协程数量控制。
 * Improvement for RVW-009: Uses Worker Pool to truly control coroutine count.
 * 协程数量与 concurrentAmount 绑定，而非按输入规模预创建。
 * Coroutine count is bound to concurrentAmount, not pre-created by input size.
 */

// ============================================================================
// associate 系列
// ============================================================================

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
    val results = executeWithWorkerPool<Pair<K, V>, T>(this, limit) { _, element -> extractor(element) }
    for ((key, value) in results) {
        destination[key] = value
    }
    return destination
}

suspend inline fun <K, V, T, M : MutableMap<in K, in V>> Iterable<T>.tryAssociateToParallelly(
    destination: M,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<Pair<K, V>, T>
): Ret<M> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeTryWithWorkerPool<Pair<K, V>, T>(this, limit) { _, element -> extractor(element) }
    return when (result) {
        is Ok -> {
            for ((key, value) in result.value) {
                destination[key] = value
            }
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

suspend inline fun <K, V, T, M : MutableMap<in K, in V>> Iterable<T>.exTryAssociateToParallelly(
    destination: M,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<Pair<K, V>, T>
): ExRet<M> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeExTryWithWorkerPool<Pair<K, V>, T>(this, limit) { _, element -> extractor(element) }
    return when (result) {
        is Ok -> {
            for ((key, value) in result.value) {
                destination[key] = value
            }
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            for ((key, value) in result.value) {
                destination[key] = value
            }
            Ok(destination)
        }
    }
}

// ============================================================================
// associateBy 系列
// ============================================================================

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
    val results = executeWithWorkerPool<Pair<K, T>, T>(this, limit) { _, element -> keyExtractor(element) to element }
    for ((key, value) in results) {
        destination[key] = value
    }
    return destination
}

suspend inline fun <K, T, M : MutableMap<in K, in T>> Iterable<T>.tryAssociateByToParallelly(
    destination: M,
    concurrentAmount: ULong? = null,
    crossinline keyExtractor: SuspendTryExtractor<K, T>
): Ret<M> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeTryWithWorkerPool<Pair<K, T>, T>(this, limit) { _, element ->
        when (val ret = keyExtractor(element)) {
            is Ok -> Ok(ret.value to element)
            is Failed -> Failed(ret.error)
            is Fatal -> Fatal(ret.errors)
        }
    }
    return when (result) {
        is Ok -> {
            for ((key, value) in result.value) {
                destination[key] = value
            }
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

suspend inline fun <K, T, M : MutableMap<in K, in T>> Iterable<T>.exTryAssociateByToParallelly(
    destination: M,
    concurrentAmount: ULong? = null,
    crossinline keyExtractor: SuspendTryExtractor<K, T>
): ExRet<M> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeExTryWithWorkerPool<Pair<K, T>, T>(this, limit) { _, element ->
        when (val ret = keyExtractor(element)) {
            is Ok -> Ok(ret.value to element)
            is Failed -> Failed(ret.error)
            is Fatal -> Fatal(ret.errors)
        }
    }
    return when (result) {
        is Ok -> {
            for ((key, value) in result.value) {
                destination[key] = value
            }
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            for ((key, value) in result.value) {
                destination[key] = value
            }
            Ok(destination)
        }
    }
}

// ============================================================================
// associateWith 系列
// ============================================================================

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
    val results = executeWithWorkerPool<Pair<T, V>, T>(this, limit) { _, element -> element to valueExtractor(element) }
    for ((key, value) in results) {
        destination[key] = value
    }
    return destination
}

suspend inline fun <V, T, M : MutableMap<in T, in V>> Iterable<T>.tryAssociateWithToParallelly(
    destination: M,
    concurrentAmount: ULong? = null,
    crossinline valueExtractor: SuspendTryExtractor<V, T>
): Ret<M> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeTryWithWorkerPool<Pair<T, V>, T>(this, limit) { _, element ->
        when (val ret = valueExtractor(element)) {
            is Ok -> Ok(element to ret.value)
            is Failed -> Failed(ret.error)
            is Fatal -> Fatal(ret.errors)
        }
    }
    return when (result) {
        is Ok -> {
            for ((key, value) in result.value) {
                destination[key] = value
            }
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

suspend inline fun <V, T, M : MutableMap<in T, in V>> Iterable<T>.exTryAssociateWithToParallelly(
    destination: M,
    concurrentAmount: ULong? = null,
    crossinline valueExtractor: SuspendTryExtractor<V, T>
): ExRet<M> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeExTryWithWorkerPool<Pair<T, V>, T>(this, limit) { _, element ->
        when (val ret = valueExtractor(element)) {
            is Ok -> Ok(element to ret.value)
            is Failed -> Failed(ret.error)
            is Fatal -> Fatal(ret.errors)
        }
    }
    return when (result) {
        is Ok -> {
            for ((key, value) in result.value) {
                destination[key] = value
            }
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            for ((key, value) in result.value) {
                destination[key] = value
            }
            Ok(destination)
        }
    }
}
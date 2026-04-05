package fuookami.ospf.kotlin.utils.parallel

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.ExRet
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Warn
import fuookami.ospf.kotlin.utils.functional.SuspendExtractor
import fuookami.ospf.kotlin.utils.functional.SuspendIndexedExtractor
import fuookami.ospf.kotlin.utils.functional.SuspendTryExtractor
import fuookami.ospf.kotlin.utils.functional.SuspendTryIndexedExtractor

/**
 * 并行展平映射操作
 *
 * Parallel flat-map operations with concurrency control.
 *
 * RVW-009 改进：使用 Worker Pool 方案实现真正的协程数量控制。
 * Improvement for RVW-009: Uses Worker Pool to truly control coroutine count.
 * 协程数量与 concurrentAmount 绑定，而非按输入规模预创建。
 * Coroutine count is bound to concurrentAmount, not pre-created by input size.
 */

// ============================================================================
// flatMap 系列
// ============================================================================

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
    val results = executeFlatMapWithWorkerPool(this, limit) { _, element -> extractor(element) }
    destination.addAll(results)
    return destination
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<Iterable<R>, T>
): Ret<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeTryFlatMapWithWorkerPool(this, limit) { _, element -> extractor(element) }
    return when (result) {
        is Ok -> {
            destination.addAll(result.value)
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.exTryFlatMapToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<Iterable<R>, T>
): ExRet<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeExTryWithWorkerPool<Iterable<R>, T>(this, limit) { _, element -> extractor(element) }
    return when (result) {
        is Ok -> {
            destination.addAll(result.value.flatten())
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            destination.addAll(result.value.flatten())
            Ok(destination)
        }
    }
}

// ============================================================================
// flatMapIndexed 系列
// ============================================================================

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
    val results = executeFlatMapWithWorkerPool(this, limit) { index, element -> extractor(index, element) }
    destination.addAll(results)
    return destination
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapIndexedToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R>, T>
): Ret<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeTryFlatMapWithWorkerPool(this, limit) { index, element -> extractor(index, element) }
    return when (result) {
        is Ok -> {
            destination.addAll(result.value)
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.exTryFlatMapIndexedToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R>, T>
): ExRet<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeExTryWithWorkerPool<Iterable<R>, T>(this, limit) { index, element -> extractor(index, element) }
    return when (result) {
        is Ok -> {
            destination.addAll(result.value.flatten())
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            destination.addAll(result.value.flatten())
            Ok(destination)
        }
    }
}

// ============================================================================
// flatMapIndexedNotNull 系列
// ============================================================================

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
    val results = executeWithWorkerPool<Iterable<R?>, T>(this, limit) { index, element -> extractor(index, element) }
    for (iterable in results) {
        destination.addAll(iterable.filterNotNull())
    }
    return destination
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapIndexedNotNullToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R?>, T>
): Ret<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeTryWithWorkerPool<Iterable<R?>, T>(this, limit) { index, element -> extractor(index, element) }
    return when (result) {
        is Ok -> {
            for (iterable in result.value) {
                destination.addAll(iterable.filterNotNull())
            }
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.exTryFlatMapIndexedNotNullToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R?>, T>
): ExRet<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeExTryWithWorkerPool<Iterable<R?>, T>(this, limit) { index, element -> extractor(index, element) }
    return when (result) {
        is Ok -> {
            for (iterable in result.value) {
                destination.addAll(iterable.filterNotNull())
            }
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            for (iterable in result.value) {
                destination.addAll(iterable.filterNotNull())
            }
            Ok(destination)
        }
    }
}
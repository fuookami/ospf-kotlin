package fuookami.ospf.kotlin.utils.parallel

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.ExRet
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Warn
import fuookami.ospf.kotlin.utils.functional.SuspendIndexedPredicate
import fuookami.ospf.kotlin.utils.functional.SuspendPredicate
import fuookami.ospf.kotlin.utils.functional.SuspendTryIndexedPredicate
import fuookami.ospf.kotlin.utils.functional.SuspendTryPredicate

/**
 * 并行过滤操作
 *
 * Parallel filtering operations with concurrency control.
 *
 * RVW-009 改进：使用 Worker Pool 方案实现真正的协程数量控制。
 * Improvement for RVW-009: Uses Worker Pool to truly control coroutine count.
 * 协程数量与 concurrentAmount 绑定，而非按输入规模预创建。
 * Coroutine count is bound to concurrentAmount, not pre-created by input size.
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
    val results = executeFilterWithWorkerPool(this, limit) { _, element -> predicate(element) }
    for ((element, keep) in results) {
        if (keep) destination.add(element)
    }
    return destination
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.tryFilterToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeTryFilterWithWorkerPool(this, limit) { _, element -> predicate(element) }
    return when (result) {
        is Ok -> {
            for ((element, keep) in result.value) {
                if (keep) destination.add(element)
            }
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.exTryFilterToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeExTryWithWorkerPool<Pair<T, Boolean>, T>(this, limit) { _, element ->
        when (val ret = predicate(element)) {
            is Ok -> Ok(element to ret.value)
            is Failed -> Failed(ret.error)
            is Fatal -> Fatal(ret.errors)
        }
    }
    return when (result) {
        is Ok -> {
            for ((element, keep) in result.value) {
                if (keep) destination.add(element)
            }
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            for ((element, keep) in result.value) {
                if (keep) destination.add(element)
            }
            Ok(destination)
        }
    }
}

// ============================================================================
// filterNotNull 系列
// ============================================================================

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
    val nonNullElements = filterNotNull()
    val limit = resolveConcurrentAmount(concurrentAmount, nonNullElements.defaultConcurrentAmount)
    val results = executeFilterWithWorkerPool(nonNullElements, limit) { _, element -> predicate(element) }
    for ((element, keep) in results) {
        if (keep) destination.add(element)
    }
    return destination
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T?>.tryFilterNotNullToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<C> {
    val nonNullElements = filterNotNull()
    val limit = resolveConcurrentAmount(concurrentAmount, nonNullElements.defaultConcurrentAmount)
    val result = executeTryFilterWithWorkerPool(nonNullElements, limit) { _, element -> predicate(element) }
    return when (result) {
        is Ok -> {
            for ((element, keep) in result.value) {
                if (keep) destination.add(element)
            }
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T?>.exTryFilterNotNullToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<C> {
    val nonNullElements = filterNotNull()
    val limit = resolveConcurrentAmount(concurrentAmount, nonNullElements.defaultConcurrentAmount)
    val result = executeExTryWithWorkerPool<Pair<T, Boolean>, T>(nonNullElements, limit) { _, element ->
        when (val ret = predicate(element)) {
            is Ok -> Ok(element to ret.value)
            is Failed -> Failed(ret.error)
            is Fatal -> Fatal(ret.errors)
        }
    }
    return when (result) {
        is Ok -> {
            for ((element, keep) in result.value) {
                if (keep) destination.add(element)
            }
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            for ((element, keep) in result.value) {
                if (keep) destination.add(element)
            }
            Ok(destination)
        }
    }
}

// ============================================================================
// filterNot 系列
// ============================================================================

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
    val results = executeFilterWithWorkerPool(this, limit) { _, element -> !predicate(element) }
    for ((element, keep) in results) {
        if (keep) destination.add(element)
    }
    return destination
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.tryFilterNotToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeTryFilterWithWorkerPool(this, limit) { _, element ->
        when (val ret = predicate(element)) {
            is Ok -> Ok(!ret.value)
            is Failed -> Failed(ret.error)
            is Fatal -> Fatal(ret.errors)
        }
    }
    return when (result) {
        is Ok -> {
            for ((element, keep) in result.value) {
                if (keep) destination.add(element)
            }
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.exTryFilterNotToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeExTryWithWorkerPool<Pair<T, Boolean>, T>(this, limit) { _, element ->
        when (val ret = predicate(element)) {
            is Ok -> Ok(element to !ret.value)
            is Failed -> Failed(ret.error)
            is Fatal -> Fatal(ret.errors)
        }
    }
    return when (result) {
        is Ok -> {
            for ((element, keep) in result.value) {
                if (keep) destination.add(element)
            }
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            for ((element, keep) in result.value) {
                if (keep) destination.add(element)
            }
            Ok(destination)
        }
    }
}

// ============================================================================
// filterIndexed 系列
// ============================================================================

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
    val results = executeFilterWithWorkerPool(this, limit) { index, element -> predicate(index, element) }
    for ((element, keep) in results) {
        if (keep) destination.add(element)
    }
    return destination
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.tryFilterIndexedToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryIndexedPredicate<T>
): Ret<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeTryFilterWithWorkerPool(this, limit) { index, element -> predicate(index, element) }
    return when (result) {
        is Ok -> {
            for ((element, keep) in result.value) {
                if (keep) destination.add(element)
            }
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.exTryFilterIndexedToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryIndexedPredicate<T>
): ExRet<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeExTryWithWorkerPool<Pair<T, Boolean>, T>(this, limit) { index, element ->
        when (val ret = predicate(index, element)) {
            is Ok -> Ok(element to ret.value)
            is Failed -> Failed(ret.error)
            is Fatal -> Fatal(ret.errors)
        }
    }
    return when (result) {
        is Ok -> {
            for ((element, keep) in result.value) {
                if (keep) destination.add(element)
            }
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            for ((element, keep) in result.value) {
                if (keep) destination.add(element)
            }
            Ok(destination)
        }
    }
}

// ============================================================================
// filterIsInstance 系列
// ============================================================================

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
    return exTryIsInstanceToParallelly(ArrayList(), concurrentAmount, predicate)
}

suspend inline fun <reified U : Any, T, C : MutableCollection<U>> Iterable<T>.filterIsInstanceToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<U>
): C {
    val instanceElements = filterIsInstance<U>()
    val limit = resolveConcurrentAmount(concurrentAmount, instanceElements.defaultConcurrentAmount)
    val results = executeFilterWithWorkerPool(instanceElements, limit) { _, element -> predicate(element) }
    for ((element, keep) in results) {
        if (keep) destination.add(element)
    }
    return destination
}

suspend inline fun <reified U : Any, T, C : MutableCollection<U>> Iterable<T>.tryFilterIsInstanceToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<U>
): Ret<C> {
    val instanceElements = filterIsInstance<U>()
    val limit = resolveConcurrentAmount(concurrentAmount, instanceElements.defaultConcurrentAmount)
    val result = executeTryFilterWithWorkerPool(instanceElements, limit) { _, element -> predicate(element) }
    return when (result) {
        is Ok -> {
            for ((element, keep) in result.value) {
                if (keep) destination.add(element)
            }
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

suspend inline fun <reified U : Any, T, C : MutableCollection<U>> Iterable<T>.exTryIsInstanceToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<U>
): ExRet<C> {
    val instanceElements = filterIsInstance<U>()
    val limit = resolveConcurrentAmount(concurrentAmount, instanceElements.defaultConcurrentAmount)
    val result = executeExTryWithWorkerPool<Pair<U, Boolean>, U>(instanceElements, limit) { _, element ->
        when (val ret = predicate(element)) {
            is Ok -> Ok(element to ret.value)
            is Failed -> Failed(ret.error)
            is Fatal -> Fatal(ret.errors)
        }
    }
    return when (result) {
        is Ok -> {
            for ((element, keep) in result.value) {
                if (keep) destination.add(element)
            }
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            for ((element, keep) in result.value) {
                if (keep) destination.add(element)
            }
            Ok(destination)
        }
    }
}
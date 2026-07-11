/**
 * 并行过滤操作
 *
 * Parallel filtering operations with concurrency control.
*/
package fuookami.ospf.kotlin.utils.parallel

import fuookami.ospf.kotlin.utils.functional.*

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

/**
 * 并行过滤满足条件的元素（带错误处理）
 *
 * Filter elements that satisfy the predicate in parallel with error handling.
 * 并发过滤满足条件的元素，支持错误处理。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 过滤后的列表或错误 / Filtered list or error
*/
suspend inline fun <T : Any> Iterable<T>.tryFilterParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<List<T>> {
    return tryFilterToParallelly(ArrayList(), concurrentAmount, predicate)
}

/**
 * 并行过滤满足条件的元素（带错误收集）
 *
 * Filter elements that satisfy the predicate in parallel with error collection.
 * 并发过滤满足条件的元素，收集所有错误。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 过滤后的列表或错误集合 / Filtered list or error collection
*/
suspend inline fun <T : Any> Iterable<T>.exTryFilterParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<List<T>> {
    return exTryFilterToParallelly(ArrayList(), concurrentAmount, predicate)
}

/**
 * 并行过滤满足条件的元素到目标集合
 *
 * Filter elements that satisfy the predicate in parallel to a destination collection.
 * 并发过滤满足条件的元素到目标集合。
 *
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件 / Predicate function
 * @return 目标集合 / Destination collection
*/
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

/**
 * 并行过滤满足条件的元素到目标集合（带错误处理）
 *
 * Filter elements that satisfy the predicate in parallel to a destination collection with error handling.
 * 并发过滤满足条件的元素到目标集合，支持错误处理。
 *
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 目标集合或错误 / Destination collection or error
*/
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

/**
 * 并行过滤满足条件的元素到目标集合（带错误收集）
 *
 * Filter elements that satisfy the predicate in parallel to a destination collection with error collection.
 * 并发过滤满足条件的元素到目标集合，收集所有错误。
 *
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 目标集合或错误集合 / Destination collection or error collection
*/
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
// filterNotNull series
// ============================================================================

/**
 * 并行过滤非空元素
 *
 * Filter non-null elements that satisfy the predicate in parallel.
 * 并发过滤满足条件的非空元素。
 *
 * @param T 元素类型（非空）/ Element type (non-null)
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件 / Predicate function
 * @return 过滤后的非空元素列表 / Filtered non-null element list
*/
suspend inline fun <T : Any> Iterable<T?>.filterNotNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): List<T> {
    return filterNotNullToParallelly(ArrayList(), concurrentAmount, predicate)
}

/**
 * 并行过滤非空元素（带错误处理）
 *
 * Filter non-null elements that satisfy the predicate in parallel with error handling.
 * 并发过滤满足条件的非空元素，支持错误处理。
 *
 * @param T 元素类型（非空）/ Element type (non-null)
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 过滤后的非空元素列表或错误 / Filtered non-null element list or error
*/
suspend inline fun <T : Any> Iterable<T?>.tryFilterNotNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<List<T>> {
    return tryFilterNotNullToParallelly(ArrayList(), concurrentAmount, predicate)
}

/**
 * 并行过滤非空元素（带错误收集）
 *
 * Filter non-null elements that satisfy the predicate in parallel with error collection.
 * 并发过滤满足条件的非空元素，收集所有错误。
 *
 * @param T 元素类型（非空）/ Element type (non-null)
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 过滤后的非空元素列表或错误集合 / Filtered non-null element list or error collection
*/
suspend inline fun <T : Any> Iterable<T?>.exTryFilterNotNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<List<T>> {
    return exTryFilterNotNullToParallelly(ArrayList(), concurrentAmount, predicate)
}

/**
 * 并行过滤非空元素到目标集合
 *
 * Filter non-null elements that satisfy the predicate in parallel to a destination collection.
 * 并发过滤满足条件的非空元素到目标集合。
 *
 * @param T 元素类型（非空）/ Element type (non-null)
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件 / Predicate function
 * @return 目标集合 / Destination collection
*/
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

/**
 * 并行过滤非空元素到目标集合（带错误处理）
 *
 * Filter non-null elements that satisfy the predicate in parallel to a destination collection with error handling.
 * 并发过滤满足条件的非空元素到目标集合，支持错误处理。
 *
 * @param T 元素类型（非空）/ Element type (non-null)
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 目标集合或错误 / Destination collection or error
*/
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

/**
 * 并行过滤非空元素到目标集合（带错误收集）
 *
 * Filter non-null elements that satisfy the predicate in parallel to a destination collection with error collection.
 * 并发过滤满足条件的非空元素到目标集合，收集所有错误。
 *
 * @param T 元素类型（非空）/ Element type (non-null)
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 目标集合或错误集合 / Destination collection or error collection
*/
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
// filterNot series
// ============================================================================

/**
 * 并行过滤不满足条件的元素
 *
 * Filter elements that do NOT satisfy the predicate in parallel.
 * 并发过滤不满足条件的元素。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件 / Predicate function
 * @return 过滤后的列表 / Filtered list
*/
suspend inline fun <T : Any> Iterable<T>.filterNotParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): List<T> {
    return filterNotToParallelly(ArrayList(), concurrentAmount, predicate)
}

/**
 * 并行过滤不满足条件的元素（带错误处理）
 *
 * Filter elements that do NOT satisfy the predicate in parallel with error handling.
 * 并发过滤不满足条件的元素，支持错误处理。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 过滤后的列表或错误 / Filtered list or error
*/
suspend inline fun <T : Any> Iterable<T>.tryFilterNotParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<List<T>> {
    return tryFilterNotToParallelly(ArrayList(), concurrentAmount, predicate)
}

/**
 * 并行过滤不满足条件的元素（带错误收集）
 *
 * Filter elements that do NOT satisfy the predicate in parallel with error collection.
 * 并发过滤不满足条件的元素，收集所有错误。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 过滤后的列表或错误集合 / Filtered list or error collection
*/
suspend inline fun <T : Any> Iterable<T>.exTryFilterNotParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<List<T>> {
    return exTryFilterNotToParallelly(ArrayList(), concurrentAmount, predicate)
}

/**
 * 并行过滤不满足条件的元素到目标集合
 *
 * Filter elements that do NOT satisfy the predicate in parallel to a destination collection.
 * 并发过滤不满足条件的元素到目标集合。
 *
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件 / Predicate function
 * @return 目标集合 / Destination collection
*/
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

/**
 * 并行过滤不满足条件的元素到目标集合（带错误处理）
 *
 * Filter elements that do NOT satisfy the predicate in parallel to a destination collection with error handling.
 * 并发过滤不满足条件的元素到目标集合，支持错误处理。
 *
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 目标集合或错误 / Destination collection or error
*/
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

/**
 * 并行过滤不满足条件的元素到目标集合（带错误收集）
 *
 * Filter elements that do NOT satisfy the predicate in parallel to a destination collection with error collection.
 * 并发过滤不满足条件的元素到目标集合，收集所有错误。
 *
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 目标集合或错误集合 / Destination collection or error collection
*/
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
// filterIndexed series
// ============================================================================

/**
 * 并行过滤满足条件的元素（带索引）
 *
 * Filter elements that satisfy the indexed predicate in parallel.
 * 并发过滤满足索引判断条件的元素。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 带索引的判断条件 / Indexed predicate function
 * @return 过滤后的列表 / Filtered list
*/
suspend inline fun <T : Any> Iterable<T>.filterIndexedParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendIndexedPredicate<T>
): List<T> {
    return filterIndexedToParallelly(ArrayList(), concurrentAmount, predicate)
}

/**
 * 并行过滤满足条件的元素（带索引，带错误处理）
 *
 * Filter elements that satisfy the indexed predicate in parallel with error handling.
 * 并发过滤满足索引判断条件的元素，支持错误处理。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 带索引的判断条件（返回 Ret）/ Indexed predicate function (returns Ret)
 * @return 过滤后的列表或错误 / Filtered list or error
*/
suspend inline fun <T : Any> Iterable<T>.tryFilterIndexedParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryIndexedPredicate<T>
): Ret<List<T>> {
    return tryFilterIndexedToParallelly(ArrayList(), concurrentAmount, predicate)
}

/**
 * 并行过滤满足条件的元素（带索引，带错误收集）
 *
 * Filter elements that satisfy the indexed predicate in parallel with error collection.
 * 并发过滤满足索引判断条件的元素，收集所有错误。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 带索引的判断条件（返回 Ret）/ Indexed predicate function (returns Ret)
 * @return 过滤后的列表或错误集合 / Filtered list or error collection
*/
suspend inline fun <T : Any> Iterable<T>.exTryFilterIndexedParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryIndexedPredicate<T>
): ExRet<List<T>> {
    return exTryFilterIndexedToParallelly(ArrayList(), concurrentAmount, predicate)
}

/**
 * 并行过滤满足条件的元素到目标集合（带索引）
 *
 * Filter elements that satisfy the indexed predicate in parallel to a destination collection.
 * 并发过滤满足索引判断条件的元素到目标集合。
 *
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 带索引的判断条件 / Indexed predicate function
 * @return 目标集合 / Destination collection
*/
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

/**
 * 并行过滤满足条件的元素到目标集合（带索引，带错误处理）
 *
 * Filter elements that satisfy the indexed predicate in parallel to a destination collection with error handling.
 * 并发过滤满足索引判断条件的元素到目标集合，支持错误处理。
 *
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 带索引的判断条件（返回 Ret）/ Indexed predicate function (returns Ret)
 * @return 目标集合或错误 / Destination collection or error
*/
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

/**
 * 并行过滤满足条件的元素到目标集合（带索引，带错误收集）
 *
 * Filter elements that satisfy the indexed predicate in parallel to a destination collection with error collection.
 * 并发过滤满足索引判断条件的元素到目标集合，收集所有错误。
 *
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 带索引的判断条件（返回 Ret）/ Indexed predicate function (returns Ret)
 * @return 目标集合或错误集合 / Destination collection or error collection
*/
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
// filterIsInstance series
// ============================================================================

/**
 * 并行过滤指定类型的元素
 *
 * Filter elements of a specific type that satisfy the predicate in parallel.
 * 并发过滤指定类型且满足条件的元素。
 *
 * @param U 目标类型 / Target type
 * @param T 源类型 / Source type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件 / Predicate function
 * @return 过滤后的指定类型元素列表 / Filtered list of specific type elements
*/
suspend inline fun <reified U : Any, T> Iterable<T>.filterIsInstanceParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<U>
): List<U> {
    return filterIsInstanceToParallelly(ArrayList(), concurrentAmount, predicate)
}

/**
 * 并行过滤指定类型的元素（带错误处理）
 *
 * Filter elements of a specific type that satisfy the predicate in parallel with error handling.
 * 并发过滤指定类型且满足条件的元素，支持错误处理。
 *
 * @param U 目标类型 / Target type
 * @param T 源类型 / Source type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 过滤后的指定类型元素列表或错误 / Filtered list of specific type elements or error
*/
suspend inline fun <reified U : Any, T> Iterable<T>.tryFilterIsInstanceParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<U>
): Ret<List<U>> {
    return tryFilterIsInstanceToParallelly(ArrayList(), concurrentAmount, predicate)
}

/**
 * 并行过滤指定类型的元素（带错误收集）
 *
 * Filter elements of a specific type that satisfy the predicate in parallel with error collection.
 * 并发过滤指定类型且满足条件的元素，收集所有错误。
 *
 * @param U 目标类型 / Target type
 * @param T 源类型 / Source type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 过滤后的指定类型元素列表或错误集合 / Filtered list of specific type elements or error collection
*/
suspend inline fun <reified U : Any, T> Iterable<T>.exTryFilterIsInstanceParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<U>
): ExRet<List<U>> {
    return exTryIsInstanceToParallelly(ArrayList(), concurrentAmount, predicate)
}

/**
 * 并行过滤指定类型的元素到目标集合
 *
 * Filter elements of a specific type that satisfy the predicate in parallel to a destination collection.
 * 并发过滤指定类型且满足条件的元素到目标集合。
 *
 * @param U 目标类型 / Target type
 * @param T 源类型 / Source type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件 / Predicate function
 * @return 目标集合 / Destination collection
*/
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

/**
 * 并行过滤指定类型的元素到目标集合（带错误处理）
 *
 * Filter elements of a specific type that satisfy the predicate in parallel to a destination collection with error handling.
 * 并发过滤指定类型且满足条件的元素到目标集合，支持错误处理。
 *
 * @param U 目标类型 / Target type
 * @param T 源类型 / Source type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 目标集合或错误 / Destination collection or error
*/
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

/**
 * 并行过滤指定类型的元素到目标集合（带错误收集）
 *
 * Filter elements of a specific type that satisfy the predicate in parallel to a destination collection with error collection.
 * 并发过滤指定类型且满足条件的元素到目标集合，收集所有错误。
 *
 * @param U 目标类型 / Target type
 * @param T 源类型 / Source type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 目标集合或错误集合 / Destination collection or error collection
*/
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

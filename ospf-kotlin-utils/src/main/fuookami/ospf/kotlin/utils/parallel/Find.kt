/**
 * 并行查找操作
 *
 * Parallel find operations (first, firstOrNull, last, lastOrNull) with concurrency control.
 */
package fuookami.ospf.kotlin.utils.parallel

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

// ============================================================================
// first 系列
// first series
// ============================================================================

/**
 * 并行查找第一个满足条件的元素
 *
 * Find the first element that satisfies the predicate in parallel.
 * 并发查找第一个满足条件的元素。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件 / Predicate function
 * @return 第一个满足条件的元素 / First element satisfying the predicate
 * @throws NoSuchElementException 如果没有元素满足条件 / If no element satisfies the predicate
 */
suspend inline fun <T> Iterable<T>.firstParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): T {
    return firstOrNullParallelly(concurrentAmount, predicate)
        ?: throw NoSuchElementException("Collection contains no element matching the predicate.")
}

/**
 * 并行查找第一个满足条件的元素（带错误处理）
 *
 * Find the first element that satisfies the predicate in parallel with error handling.
 * 并发查找第一个满足条件的元素，支持错误处理。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 第一个满足条件的元素或错误 / First element satisfying the predicate or error
 */
suspend inline fun <T> Iterable<T>.tryFirstParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T> {
    return when (val result = tryFirstOrNullParallelly(concurrentAmount, predicate)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行查找第一个满足条件的元素（带错误收集）
 *
 * Find the first element that satisfies the predicate in parallel with error collection.
 * 并发查找第一个满足条件的元素，收集所有错误。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 第一个满足条件的元素或错误集合 / First element satisfying the predicate or error collection
 */
suspend inline fun <T> Iterable<T>.exTryFirstParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<T> {
    return when (val result = exTryFirstOrNullParallelly(concurrentAmount, predicate)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))
    }
}

/**
 * 并行查找第一个满足条件的元素（可能为空）
 *
 * Find the first element that satisfies the predicate in parallel, or null if not found.
 * 并发查找第一个满足条件的元素，如果未找到则返回 null。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件 / Predicate function
 * @return 第一个满足条件的元素或 null / First element satisfying the predicate or null
 */
suspend inline fun <T> Iterable<T>.firstOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): T? {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val results = executePredicateWithWorkerPool(elements, limit) { _, element -> predicate(element) }
    for ((index, matched) in results.withIndex()) {
        if (matched) return elements[index]
    }
    return null
}

/**
 * 并行查找第一个满足条件的元素（带错误处理，可能为空）
 *
 * Find the first element that satisfies the predicate in parallel with error handling, or null if not found.
 * 并发查找第一个满足条件的元素，支持错误处理，如果未找到则返回 null。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 第一个满足条件的元素或 null 或错误 / First element satisfying the predicate or null or error
 */
suspend inline fun <T> Iterable<T>.tryFirstOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val result = executeTryPredicateWithWorkerPool(elements, limit) { _, element -> predicate(element) }
    return when (result) {
        is Ok -> {
            for ((index, matched) in result.value.withIndex()) {
                if (matched) return Ok(elements[index])
            }
            Ok(null)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行查找第一个满足条件的元素（带错误收集，可能为空）
 *
 * Find the first element that satisfies the predicate in parallel with error collection, or null if not found.
 * 并发查找第一个满足条件的元素，收集所有错误，如果未找到则返回 null。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 第一个满足条件的元素或 null 或错误集合 / First element satisfying the predicate or null or error collection
 */
suspend inline fun <T> Iterable<T>.exTryFirstOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<T?> {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val result = executeExTryWithWorkerPool<Boolean, T>(elements, limit) { _, element -> predicate(element) }
    return when (result) {
        is Ok -> {
            var matched: T? = null
            for ((index, value) in result.value.withIndex()) {
                if (matched == null && value) matched = elements[index]
            }
            Ok(matched)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            var matched: T? = null
            for ((index, value) in result.value.withIndex()) {
                if (matched == null && value) matched = elements[index]
            }
            Ok(matched)
        }
    }
}

// ============================================================================
// firstNotNullOf 系列
// firstNotNullOf series
// ============================================================================

/**
 * 并行查找第一个非空转换结果
 *
 * Find the first non-null result from transforming elements in parallel.
 * 并发查找第一个非空的元素转换结果。
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 转换函数（可能返回空值）/ Extractor function (may return null)
 * @return 第一个非空转换结果 / First non-null result
 * @throws NoSuchElementException 如果没有非空结果 / If no non-null result found
 */
suspend inline fun <R, T> Iterable<T>.firstNotNullOfParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendExtractor<R?, T>
): R {
    return firstNotNullOfOrNullParallelly(concurrentAmount, extractor)
        ?: throw NoSuchElementException("No element of the collection was transformed to a non-null value.")
}

/**
 * 并行查找第一个非空转换结果（带错误处理）
 *
 * Find the first non-null result from transforming elements in parallel with error handling.
 * 并发查找第一个非空的元素转换结果，支持错误处理。
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 转换函数（返回 Ret，可能包含空值）/ Extractor function (returns Ret, may contain null)
 * @return 第一个非空转换结果或错误 / First non-null result or error
 */
suspend inline fun <R, T> Iterable<T>.tryFirstNotNullOfParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<R> {
    return when (val result = tryFirstNotNullOfOrNullParallelly(concurrentAmount, extractor)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "No element of the collection was transformed to a non-null value."))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行查找第一个非空转换结果（带错误收集）
 *
 * Find the first non-null result from transforming elements in parallel with error collection.
 * 并发查找第一个非空的元素转换结果，收集所有错误。
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 转换函数（返回 Ret，可能包含空值）/ Extractor function (returns Ret, may contain null)
 * @return 第一个非空转换结果或错误集合 / First non-null result or error collection
 */
suspend inline fun <R, T> Iterable<T>.exTryFirstNotNullOfParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): ExRet<R> {
    return when (val result = exTryFirstNotNullOfOrNullParallelly(concurrentAmount, extractor)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "No element of the collection was transformed to a non-null value."))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "No element of the collection was transformed to a non-null value."))
    }
}

/**
 * 并行查找第一个非空转换结果（可能为空）
 *
 * Find the first non-null result from transforming elements in parallel, or null if not found.
 * 并发查找第一个非空的元素转换结果，如果未找到则返回 null。
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 转换函数（可能返回空值）/ Extractor function (may return null)
 * @return 第一个非空转换结果或 null / First non-null result or null
 */
suspend inline fun <R, T> Iterable<T>.firstNotNullOfOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendExtractor<R?, T>
): R? {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val results = executeWithWorkerPool<R?, T>(elements, limit) { _, element -> extractor(element) }
    for (value in results) {
        if (value != null) return value
    }
    return null
}

/**
 * 并行查找第一个非空转换结果（带错误处理，可能为空）
 *
 * Find the first non-null result from transforming elements in parallel with error handling, or null if not found.
 * 并发查找第一个非空的元素转换结果，支持错误处理，如果未找到则返回 null。
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 转换函数（返回 Ret，可能包含空值）/ Extractor function (returns Ret, may contain null)
 * @return 第一个非空转换结果或 null 或错误 / First non-null result or null or error
 */
suspend inline fun <R, T> Iterable<T>.tryFirstNotNullOfOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<R?> {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val result = executeTryWithWorkerPool<R?, T>(elements, limit) { _, element -> extractor(element) }
    return when (result) {
        is Ok -> {
            for (value in result.value) {
                if (value != null) return Ok(value)
            }
            Ok(null)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行查找第一个非空转换结果（带错误收集，可能为空）
 *
 * Find the first non-null result from transforming elements in parallel with error collection, or null if not found.
 * 并发查找第一个非空的元素转换结果，收集所有错误，如果未找到则返回 null。
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 转换函数（返回 Ret，可能包含空值）/ Extractor function (returns Ret, may contain null)
 * @return 第一个非空转换结果或 null 或错误集合 / First non-null result or null or error collection
 */
suspend inline fun <R, T> Iterable<T>.exTryFirstNotNullOfOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): ExRet<R?> {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val result = executeExTryWithWorkerPool<R?, T>(elements, limit) { _, element -> extractor(element) }
    return when (result) {
        is Ok -> {
            var found: R? = null
            for (value in result.value) {
                if (found == null && value != null) found = value
            }
            Ok(found)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            var found: R? = null
            for (value in result.value) {
                if (found == null && value != null) found = value
            }
            Ok(found)
        }
    }
}

// ============================================================================
// last 系列
// last series
// ============================================================================

/**
 * 并行查找最后一个满足条件的元素
 *
 * Find the last element that satisfies the predicate in parallel.
 * 并发查找最后一个满足条件的元素。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件 / Predicate function
 * @return 最后一个满足条件的元素 / Last element satisfying the predicate
 * @throws NoSuchElementException 如果没有元素满足条件 / If no element satisfies the predicate
 */
suspend inline fun <T> Iterable<T>.lastParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): T {
    return lastOrNullParallelly(concurrentAmount, predicate)
        ?: throw NoSuchElementException("Collection contains no element matching the predicate.")
}

/**
 * 并行查找最后一个满足条件的元素（带错误处理）
 *
 * Find the last element that satisfies the predicate in parallel with error handling.
 * 并发查找最后一个满足条件的元素，支持错误处理。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 最后一个满足条件的元素或错误 / Last element satisfying the predicate or error
 */
suspend inline fun <T> Iterable<T>.tryLastParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T> {
    return when (val result = tryLastOrNullParallelly(concurrentAmount, predicate)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行查找最后一个满足条件的元素（带错误收集）
 *
 * Find the last element that satisfies the predicate in parallel with error collection.
 * 并发查找最后一个满足条件的元素，收集所有错误。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 最后一个满足条件的元素或错误集合 / Last element satisfying the predicate or error collection
 */
suspend inline fun <T> Iterable<T>.exTryLastParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<T> {
    return when (val result = exTryLastOrNullParallelly(concurrentAmount, predicate)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))
    }
}

/**
 * 并行查找最后一个满足条件的元素（可能为空）
 *
 * Find the last element that satisfies the predicate in parallel, or null if not found.
 * 并发查找最后一个满足条件的元素，如果未找到则返回 null。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件 / Predicate function
 * @return 最后一个满足条件的元素或 null / Last element satisfying the predicate or null
 */
suspend inline fun <T> Iterable<T>.lastOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): T? {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val results = executePredicateWithWorkerPool(elements, limit) { _, element -> predicate(element) }
    for (index in results.indices.reversed()) {
        if (results[index]) return elements[index]
    }
    return null
}

/**
 * 并行查找最后一个满足条件的元素（带错误处理，可能为空）
 *
 * Find the last element that satisfies the predicate in parallel with error handling, or null if not found.
 * 并发查找最后一个满足条件的元素，支持错误处理，如果未找到则返回 null。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 最后一个满足条件的元素或 null 或错误 / Last element satisfying the predicate or null or error
 */
suspend inline fun <T> Iterable<T>.tryLastOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val result = executeTryPredicateWithWorkerPool(elements, limit) { _, element -> predicate(element) }
    return when (result) {
        is Ok -> {
            for (index in result.value.indices.reversed()) {
                if (result.value[index]) return Ok(elements[index])
            }
            Ok(null)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行查找最后一个满足条件的元素（带错误收集，可能为空）
 *
 * Find the last element that satisfies the predicate in parallel with error collection, or null if not found.
 * 并发查找最后一个满足条件的元素，收集所有错误，如果未找到则返回 null。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 最后一个满足条件的元素或 null 或错误集合 / Last element satisfying the predicate or null or error collection
 */
suspend inline fun <T> Iterable<T>.exTryLastOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<T?> {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val result = executeExTryWithWorkerPool<Boolean, T>(elements, limit) { _, element -> predicate(element) }
    return when (result) {
        is Ok -> {
            var matched: T? = null
            for (index in result.value.indices.reversed()) {
                if (matched == null && result.value[index]) matched = elements[index]
            }
            Ok(matched)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            var matched: T? = null
            for (index in result.value.indices.reversed()) {
                if (matched == null && result.value[index]) matched = elements[index]
            }
            Ok(matched)
        }
    }
}

// ============================================================================
// lastNotNullOf 系列
// lastNotNullOf series
// ============================================================================

/**
 * 并行查找最后一个非空转换结果
 *
 * Find the last non-null result from transforming elements in parallel.
 * 并发查找最后一个非空的元素转换结果。
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 转换函数（可能返回空值）/ Extractor function (may return null)
 * @return 最后一个非空转换结果 / Last non-null result
 * @throws NoSuchElementException 如果没有非空结果 / If no non-null result found
 */
suspend inline fun <R, T> Iterable<T>.lastNotNullOfParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendExtractor<R?, T>
): R {
    return lastNotNullOfOrNullParallelly(concurrentAmount, extractor)
        ?: throw NoSuchElementException("No element of the collection was transformed to a non-null value.")
}

/**
 * 并行查找最后一个非空转换结果（带错误处理）
 *
 * Find the last non-null result from transforming elements in parallel with error handling.
 * 并发查找最后一个非空的元素转换结果，支持错误处理。
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 转换函数（返回 Ret，可能包含空值）/ Extractor function (returns Ret, may contain null)
 * @return 最后一个非空转换结果或错误 / Last non-null result or error
 */
suspend inline fun <R, T> Iterable<T>.tryLastNotNullOfParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<R> {
    return when (val result = tryLastNotNullOfOrNullParallelly(concurrentAmount, extractor)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "No element of the collection was transformed to a non-null value."))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行查找最后一个非空转换结果（带错误收集）
 *
 * Find the last non-null result from transforming elements in parallel with error collection.
 * 并发查找最后一个非空的元素转换结果，收集所有错误。
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 转换函数（返回 Ret，可能包含空值）/ Extractor function (returns Ret, may contain null)
 * @return 最后一个非空转换结果或错误集合 / Last non-null result or error collection
 */
suspend inline fun <R, T> Iterable<T>.exTryLastNotNullOfParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): ExRet<R> {
    return when (val result = exTryLastNotNullOfOrNullParallelly(concurrentAmount, extractor)) {
        is Ok -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "No element of the collection was transformed to a non-null value."))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> result.value?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "No element of the collection was transformed to a non-null value."))
    }
}

/**
 * 并行查找最后一个非空转换结果（可能为空）
 *
 * Find the last non-null result from transforming elements in parallel, or null if not found.
 * 并发查找最后一个非空的元素转换结果，如果未找到则返回 null。
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 转换函数（可能返回空值）/ Extractor function (may return null)
 * @return 最后一个非空转换结果或 null / Last non-null result or null
 */
suspend inline fun <R, T> Iterable<T>.lastNotNullOfOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendExtractor<R?, T>
): R? {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val results = executeWithWorkerPool<R?, T>(elements, limit) { _, element -> extractor(element) }
    for (index in results.indices.reversed()) {
        val value = results[index]
        if (value != null) return value
    }
    return null
}

/**
 * 并行查找最后一个非空转换结果（带错误处理，可能为空）
 *
 * Find the last non-null result from transforming elements in parallel with error handling, or null if not found.
 * 并发查找最后一个非空的元素转换结果，支持错误处理，如果未找到则返回 null。
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 转换函数（返回 Ret，可能包含空值）/ Extractor function (returns Ret, may contain null)
 * @return 最后一个非空转换结果或 null 或错误 / Last non-null result or null or error
 */
suspend inline fun <R, T> Iterable<T>.tryLastNotNullOfOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<R?> {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val result = executeTryWithWorkerPool<R?, T>(elements, limit) { _, element -> extractor(element) }
    return when (result) {
        is Ok -> {
            for (index in result.value.indices.reversed()) {
                val value = result.value[index]
                if (value != null) return Ok(value)
            }
            Ok(null)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行查找最后一个非空转换结果（带错误收集，可能为空）
 *
 * Find the last non-null result from transforming elements in parallel with error collection, or null if not found.
 * 并发查找最后一个非空的元素转换结果，收集所有错误，如果未找到则返回 null。
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 转换函数（返回 Ret，可能包含空值）/ Extractor function (returns Ret, may contain null)
 * @return 最后一个非空转换结果或 null 或错误集合 / Last non-null result or null or error collection
 */
suspend inline fun <R, T> Iterable<T>.exTryLastNotNullOfOrNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): ExRet<R?> {
    val elements = toList()
    val limit = resolveConcurrentAmount(concurrentAmount, elements.defaultConcurrentAmount)
    val result = executeExTryWithWorkerPool<R?, T>(elements, limit) { _, element -> extractor(element) }
    return when (result) {
        is Ok -> {
            var found: R? = null
            for (index in result.value.indices.reversed()) {
                val value = result.value[index]
                if (found == null && value != null) found = value
            }
            Ok(found)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            var found: R? = null
            for (index in result.value.indices.reversed()) {
                val value = result.value[index]
                if (found == null && value != null) found = value
            }
            Ok(found)
        }
    }
}

// ============================================================================
// find 系列 (别名)
// find series (aliases)
// ============================================================================

/**
 * 并行查找满足条件的元素（firstOrNull 的别名）
 *
 * Find an element that satisfies the predicate in parallel (alias for firstOrNullParallelly).
 * 并发查找满足条件的元素（firstOrNullParallelly 的别名）。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件 / Predicate function
 * @return 满足条件的元素或 null / Element satisfying the predicate or null
 */
suspend inline fun <T> Iterable<T>.findParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): T? {
    return firstOrNullParallelly(concurrentAmount, predicate)
}

/**
 * 并行查找满足条件的元素（带错误处理，tryFirstOrNull 的别名）
 *
 * Find an element that satisfies the predicate in parallel with error handling (alias for tryFirstOrNullParallelly).
 * 并发查找满足条件的元素，支持错误处理（tryFirstOrNullParallelly 的别名）。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 满足条件的元素或 null 或错误 / Element satisfying the predicate or null or error
 */
suspend inline fun <T> Iterable<T>.tryFindParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    return tryFirstOrNullParallelly(concurrentAmount, predicate)
}

/**
 * 并行查找满足条件的元素（带错误收集，exTryFirstOrNull 的别名）
 *
 * Find an element that satisfies the predicate in parallel with error collection (alias for exTryFirstOrNullParallelly).
 * 并发查找满足条件的元素，收集所有错误（exTryFirstOrNullParallelly 的别名）。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 满足条件的元素或 null 或错误集合 / Element satisfying the predicate or null or error collection
 */
suspend inline fun <T> Iterable<T>.exTryFindParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<T?> {
    return exTryFirstOrNullParallelly(concurrentAmount, predicate)
}

/**
 * 并行查找最后一个满足条件的元素（lastOrNull 的别名）
 *
 * Find the last element that satisfies the predicate in parallel (alias for lastOrNullParallelly).
 * 并发查找最后一个满足条件的元素（lastOrNullParallelly 的别名）。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件 / Predicate function
 * @return 最后一个满足条件的元素或 null / Last element satisfying the predicate or null
 */
suspend inline fun <T> Iterable<T>.findLastParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): T? {
    return lastOrNullParallelly(concurrentAmount, predicate)
}

/**
 * 并行查找最后一个满足条件的元素（带错误处理，tryLastOrNull 的别名）
 *
 * Find the last element that satisfies the predicate in parallel with error handling (alias for tryLastOrNullParallelly).
 * 并发查找最后一个满足条件的元素，支持错误处理（tryLastOrNullParallelly 的别名）。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 最后一个满足条件的元素或 null 或错误 / Last element satisfying the predicate or null or error
 */
suspend inline fun <T> Iterable<T>.tryFindLastParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    return tryLastOrNullParallelly(concurrentAmount, predicate)
}

/**
 * 并行查找最后一个满足条件的元素（带错误收集，exTryLastOrNull 的别名）
 *
 * Find the last element that satisfies the predicate in parallel with error collection (alias for exTryLastOrNullParallelly).
 * 并发查找最后一个满足条件的元素，收集所有错误（exTryLastOrNullParallelly 的别名）。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 最后一个满足条件的元素或 null 或错误集合 / Last element satisfying the predicate or null or error collection
 */
suspend inline fun <T> Iterable<T>.exTryFindLastParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<T?> {
    return exTryLastOrNullParallelly(concurrentAmount, predicate)
}

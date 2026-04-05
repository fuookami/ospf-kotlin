package fuookami.ospf.kotlin.utils.parallel

import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.ExRet
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Warn
import fuookami.ospf.kotlin.utils.functional.SuspendExtractor
import fuookami.ospf.kotlin.utils.functional.SuspendPredicate
import fuookami.ospf.kotlin.utils.functional.SuspendTryExtractor
import fuookami.ospf.kotlin.utils.functional.SuspendTryPredicate

/**
 * 并行查找操作
 *
 * Parallel find operations (first, firstOrNull, last, lastOrNull) with concurrency control.
 *
 * RVW-009 改进：使用 Worker Pool 方案实现真正的协程数量控制。
 * Improvement for RVW-009: Uses Worker Pool to truly control coroutine count.
 * 协程数量与 concurrentAmount 绑定，而非按输入规模预创建。
 * Coroutine count is bound to concurrentAmount, not pre-created by input size.
 */

// ============================================================================
// first 系列
// ============================================================================

suspend inline fun <T> Iterable<T>.firstParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): T {
    return firstOrNullParallelly(concurrentAmount, predicate)
        ?: throw NoSuchElementException("Collection contains no element matching the predicate.")
}

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
// ============================================================================

suspend inline fun <R, T> Iterable<T>.firstNotNullOfParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendExtractor<R?, T>
): R {
    return firstNotNullOfOrNullParallelly(concurrentAmount, extractor)
        ?: throw NoSuchElementException("No element of the collection was transformed to a non-null value.")
}

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
// ============================================================================

suspend inline fun <T> Iterable<T>.lastParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): T {
    return lastOrNullParallelly(concurrentAmount, predicate)
        ?: throw NoSuchElementException("Collection contains no element matching the predicate.")
}

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
// ============================================================================

suspend inline fun <R, T> Iterable<T>.lastNotNullOfParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendExtractor<R?, T>
): R {
    return lastNotNullOfOrNullParallelly(concurrentAmount, extractor)
        ?: throw NoSuchElementException("No element of the collection was transformed to a non-null value.")
}

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
// ============================================================================

suspend inline fun <T> Iterable<T>.findParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): T? {
    return firstOrNullParallelly(concurrentAmount, predicate)
}

suspend inline fun <T> Iterable<T>.tryFindParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    return tryFirstOrNullParallelly(concurrentAmount, predicate)
}

suspend inline fun <T> Iterable<T>.exTryFindParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<T?> {
    return exTryFirstOrNullParallelly(concurrentAmount, predicate)
}

suspend inline fun <T> Iterable<T>.findLastParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): T? {
    return lastOrNullParallelly(concurrentAmount, predicate)
}

suspend inline fun <T> Iterable<T>.tryFindLastParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    return tryLastOrNullParallelly(concurrentAmount, predicate)
}

suspend inline fun <T> Iterable<T>.exTryFindLastParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<T?> {
    return exTryLastOrNullParallelly(concurrentAmount, predicate)
}
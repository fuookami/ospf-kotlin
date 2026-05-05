package fuookami.ospf.kotlin.utils.parallel

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.ExRet
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.SuspendExtractor
import fuookami.ospf.kotlin.utils.functional.SuspendIndexedExtractor
import fuookami.ospf.kotlin.utils.functional.SuspendTryExtractor
import fuookami.ospf.kotlin.utils.functional.SuspendTryIndexedExtractor
import fuookami.ospf.kotlin.utils.functional.Warn

/**
 * 并行映射操作
 *
 * Parallel mapping operations with concurrency control.
 *
 * RVW-009 改进：使用 Worker Pool 方案实现真正的协程数量控制。
 * Improvement for RVW-009: Uses Worker Pool to truly control coroutine count.
 * 协程数量与 concurrentAmount 绑定，而非按输入规模预创建。
 * Coroutine count is bound to concurrentAmount, not pre-created by input size.
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
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    return executeWithWorkerPool(this, limit) { _, element -> extractor(element) }
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
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    return executeTryWithWorkerPool(this, limit) { _, element -> extractor(element) }
}

/**
 * 并行映射集合元素（带错误收集）
 *
 * Map collection elements in parallel with error collection and concurrency control.
 * 并发映射集合元素并收集所有错误。
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 提取器函数（返回 Ret）/ Extractor function (returns Ret)
 * @return 映射结果或错误集合 / Mapped result or error collection
 */
suspend inline fun <R, T> Iterable<T>.exTryMapParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R, T>
): ExRet<List<R>> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    return executeExTryWithWorkerPool(this, limit) { _, element -> extractor(element) }
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
    val results = executeWithWorkerPool(this, limit) { _, element -> extractor(element) }
    destination.addAll(results)
    return destination
}

/**
 * 并行映射集合元素到目标集合（带错误处理）
 *
 * Map collection elements in parallel to a destination collection with error handling.
 * 并发映射集合元素到目标集合，支持错误处理。
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 提取器函数（返回 Ret）/ Extractor function (returns Ret)
 * @return 目标集合或错误 / Destination collection or error
 */
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryMapToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    return when (val result = executeTryWithWorkerPool<R, T>(this, limit) { _, element -> extractor(element) }) {
        is Ok -> {
            destination.addAll(result.value)
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行映射集合元素到目标集合（带错误收集）
 *
 * Map collection elements in parallel to a destination collection with error collection.
 * 并发映射集合元素到目标集合，收集所有错误。
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 提取器函数（返回 Ret）/ Extractor function (returns Ret)
 * @return 目标集合或错误集合 / Destination collection or error collection
 */
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.exTryMapToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R, T>
): ExRet<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    return when (val result = executeExTryWithWorkerPool<R, T>(this, limit) { _, element -> extractor(element) }) {
        is Ok -> {
            destination.addAll(result.value)
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            destination.addAll(result.value)
            Warn(destination, result.warnings)
        }
    }
}

/**
 * 并行映射集合元素（过滤空值）
 *
 * Map collection elements in parallel, filtering out null results.
 * 并发映射集合元素，过滤掉空值结果。
 *
 * @param R 结果类型（非空）/ Result type (non-null)
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 提取器函数（可能返回空值）/ Extractor function (may return null)
 * @return 过滤空值后的结果列表 / Result list with nulls filtered out
 */
suspend inline fun <R : Any, T> Iterable<T>.mapNotNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendExtractor<R?, T>
): List<R> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val results = executeWithWorkerPool(this, limit) { _, element -> extractor(element) } as List<R>
    return results.filterNotNull()
}

/**
 * 并行映射集合元素（过滤空值，带错误处理）
 *
 * Map collection elements in parallel, filtering out null results with error handling.
 * 并发映射集合元素，过滤掉空值结果，支持错误处理。
 *
 * @param R 结果类型（非空）/ Result type (non-null)
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 提取器函数（返回 Ret，可能包含空值）/ Extractor function (returns Ret, may contain null)
 * @return 过滤空值后的结果列表或错误 / Result list with nulls filtered out or error
 */
suspend inline fun <R : Any, T> Iterable<T>.tryMapNotNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<List<R>> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    return when (val result = executeTryWithWorkerPool<R?, T>(this, limit) { _, element -> extractor(element) }) {
        is Ok -> {
            Ok(result.value.filterNotNull() as List<R>)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行映射集合元素（过滤空值，带错误收集）
 *
 * Map collection elements in parallel, filtering out null results with error collection.
 * 并发映射集合元素，过滤掉空值结果，收集所有错误。
 *
 * @param R 结果类型（非空）/ Result type (non-null)
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 提取器函数（返回 Ret，可能包含空值）/ Extractor function (returns Ret, may contain null)
 * @return 过滤空值后的结果列表或错误集合 / Result list with nulls filtered out or error collection
 */
suspend inline fun <R : Any, T> Iterable<T>.exTryMapNotNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): ExRet<List<R>> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    return when (val result = executeExTryWithWorkerPool<R?, T>(this, limit) { _, element -> extractor(element) }) {
        is Ok -> {
            Ok(result.value.filterNotNull() as List<R>)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            Ok(result.value.filterNotNull() as List<R>)
        }
    }
}

/**
 * 并行映射集合元素到目标集合（过滤空值）
 *
 * Map collection elements in parallel to a destination collection, filtering out null results.
 * 并发映射集合元素到目标集合，过滤掉空值结果。
 *
 * @param R 结果类型（非空）/ Result type (non-null)
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 提取器函数（可能返回空值）/ Extractor function (may return null)
 * @return 目标集合 / Destination collection
 */
suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.mapNotNullToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendExtractor<R?, T>
): C {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val results = executeWithWorkerPool<R?, T>(this, limit) { _, element -> extractor(element) }
    results.filterNotNull().forEach { destination.add(it) }
    return destination
}

/**
 * 并行映射集合元素到目标集合（过滤空值，带错误处理）
 *
 * Map collection elements in parallel to a destination collection, filtering out null results with error handling.
 * 并发映射集合元素到目标集合，过滤掉空值结果，支持错误处理。
 *
 * @param R 结果类型（非空）/ Result type (non-null)
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 提取器函数（返回 Ret，可能包含空值）/ Extractor function (returns Ret, may contain null)
 * @return 目标集合或错误 / Destination collection or error
 */
suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.tryMapNotNullToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    return when (val result = executeTryWithWorkerPool<R?, T>(this, limit) { _, element -> extractor(element) }) {
        is Ok -> {
            result.value.filterNotNull().forEach { destination.add(it) }
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行映射集合元素到目标集合（过滤空值，带错误收集）
 *
 * Map collection elements in parallel to a destination collection, filtering out null results with error collection.
 * 并发映射集合元素到目标集合，过滤掉空值结果，收集所有错误。
 *
 * @param R 结果类型（非空）/ Result type (non-null)
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 提取器函数（返回 Ret，可能包含空值）/ Extractor function (returns Ret, may contain null)
 * @return 目标集合或错误集合 / Destination collection or error collection
 */
suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.exTryMapNotNullToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<R?, T>
): ExRet<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    return when (val result = executeExTryWithWorkerPool<R?, T>(this, limit) { _, element -> extractor(element) }) {
        is Ok -> {
            result.value.filterNotNull().forEach { destination.add(it) }
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            result.value.filterNotNull().forEach { destination.add(it) }
            Warn(destination, result.warnings)
        }
    }
}

/**
 * 并行映射集合元素（带索引）
 *
 * Map collection elements in parallel with index information.
 * 并发映射集合元素，提供元素索引信息。
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的提取器函数 / Indexed extractor function
 * @return 映射后的列表 / Mapped list
 */
suspend inline fun <R, T> Iterable<T>.mapIndexedParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendIndexedExtractor<R, T>
): List<R> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    return executeWithWorkerPool(this, limit) { index, element -> extractor(index, element) }
}

/**
 * 并行映射集合元素（带索引，带错误处理）
 *
 * Map collection elements in parallel with index information and error handling.
 * 并发映射集合元素，提供元素索引信息，支持错误处理。
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的提取器函数（返回 Ret）/ Indexed extractor function (returns Ret)
 * @return 映射结果或错误 / Mapped result or error
 */
suspend inline fun <R, T> Iterable<T>.tryMapIndexedParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<R, T>
): Ret<List<R>> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    return executeTryWithWorkerPool(this, limit) { index, element -> extractor(index, element) }
}

/**
 * 并行映射集合元素（带索引，带错误收集）
 *
 * Map collection elements in parallel with index information and error collection.
 * 并发映射集合元素，提供元素索引信息，收集所有错误。
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的提取器函数（返回 Ret）/ Indexed extractor function (returns Ret)
 * @return 映射结果或错误集合 / Mapped result or error collection
 */
suspend inline fun <R, T> Iterable<T>.exTryMapIndexedParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<R, T>
): ExRet<List<R>> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    return executeExTryWithWorkerPool(this, limit) { index, element -> extractor(index, element) }
}

/**
 * 并行映射集合元素到目标集合（带索引）
 *
 * Map collection elements in parallel to a destination collection with index information.
 * 并发映射集合元素到目标集合，提供元素索引信息。
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的提取器函数 / Indexed extractor function
 * @return 目标集合 / Destination collection
 */
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.mapIndexedToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendIndexedExtractor<R, T>
): C {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val results = executeWithWorkerPool(this, limit) { index, element -> extractor(index, element) }
    destination.addAll(results)
    return destination
}

/**
 * 并行映射集合元素到目标集合（带索引，带错误处理）
 *
 * Map collection elements in parallel to a destination collection with index information and error handling.
 * 并发映射集合元素到目标集合，提供元素索引信息，支持错误处理。
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的提取器函数（返回 Ret）/ Indexed extractor function (returns Ret)
 * @return 目标集合或错误 / Destination collection or error
 */
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryMapIndexedToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<R, T>
): Ret<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    return when (val result = executeTryWithWorkerPool<R, T>(this, limit) { index, element -> extractor(index, element) }) {
        is Ok -> {
            destination.addAll(result.value)
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行映射集合元素到目标集合（带索引，带错误收集）
 *
 * Map collection elements in parallel to a destination collection with index information and error collection.
 * 并发映射集合元素到目标集合，提供元素索引信息，收集所有错误。
 *
 * @param R 结果类型 / Result type
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的提取器函数（返回 Ret）/ Indexed extractor function (returns Ret)
 * @return 目标集合或错误集合 / Destination collection or error collection
 */
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.exTryMapIndexedToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<R, T>
): ExRet<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    return when (val result = executeExTryWithWorkerPool<R, T>(this, limit) { index, element -> extractor(index, element) }) {
        is Ok -> {
            destination.addAll(result.value)
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            destination.addAll(result.value)
            Warn(destination, result.warnings)
        }
    }
}

/**
 * 并行映射集合元素（带索引，过滤空值）
 *
 * Map collection elements in parallel with index information, filtering out null results.
 * 并发映射集合元素，提供元素索引信息，过滤掉空值结果。
 *
 * @param R 结果类型（非空）/ Result type (non-null)
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的提取器函数（可能返回空值）/ Indexed extractor function (may return null)
 * @return 过滤空值后的结果列表 / Result list with nulls filtered out
 */
suspend inline fun <R : Any, T> Iterable<T>.mapIndexedNotNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendIndexedExtractor<R?, T>
): List<R> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val results = executeWithWorkerPool(this, limit) { index, element -> extractor(index, element) } as List<R>
    return results.filterNotNull()
}

/**
 * 并行映射集合元素（带索引，过滤空值，带错误处理）
 *
 * Map collection elements in parallel with index information, filtering out null results with error handling.
 * 并发映射集合元素，提供元素索引信息，过滤掉空值结果，支持错误处理。
 *
 * @param R 结果类型（非空）/ Result type (non-null)
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的提取器函数（返回 Ret，可能包含空值）/ Indexed extractor function (returns Ret, may contain null)
 * @return 过滤空值后的结果列表或错误 / Result list with nulls filtered out or error
 */
suspend inline fun <R : Any, T> Iterable<T>.tryMapIndexedNotNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<R?, T>
): Ret<List<R>> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    return when (val result = executeTryWithWorkerPool<R?, T>(this, limit) { index, element -> extractor(index, element) }) {
        is Ok -> {
            Ok(result.value.filterNotNull() as List<R>)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行映射集合元素（带索引，过滤空值，带错误收集）
 *
 * Map collection elements in parallel with index information, filtering out null results with error collection.
 * 并发映射集合元素，提供元素索引信息，过滤掉空值结果，收集所有错误。
 *
 * @param R 结果类型（非空）/ Result type (non-null)
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的提取器函数（返回 Ret，可能包含空值）/ Indexed extractor function (returns Ret, may contain null)
 * @return 过滤空值后的结果列表或错误集合 / Result list with nulls filtered out or error collection
 */
suspend inline fun <R : Any, T> Iterable<T>.exTryMapIndexedNotNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<R?, T>
): ExRet<List<R>> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    return when (val result = executeExTryWithWorkerPool<R?, T>(this, limit) { index, element -> extractor(index, element) }) {
        is Ok -> {
            Ok(result.value.filterNotNull() as List<R>)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            Ok(result.value.filterNotNull() as List<R>)
        }
    }
}

/**
 * 并行映射集合元素到目标集合（带索引，过滤空值）
 *
 * Map collection elements in parallel to a destination collection with index information, filtering out null results.
 * 并发映射集合元素到目标集合，提供元素索引信息，过滤掉空值结果。
 *
 * @param R 结果类型（非空）/ Result type (non-null)
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的提取器函数（可能返回空值）/ Indexed extractor function (may return null)
 * @return 目标集合 / Destination collection
 */
suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.mapIndexedNotNullToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendIndexedExtractor<R?, T>
): C {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val results = executeWithWorkerPool<R?, T>(this, limit) { index, element -> extractor(index, element) }
    results.filterNotNull().forEach { destination.add(it) }
    return destination
}

/**
 * 并行映射集合元素到目标集合（带索引，过滤空值，带错误处理）
 *
 * Map collection elements in parallel to a destination collection with index information, filtering out null results with error handling.
 * 并发映射集合元素到目标集合，提供元素索引信息，过滤掉空值结果，支持错误处理。
 *
 * @param R 结果类型（非空）/ Result type (non-null)
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的提取器函数（返回 Ret，可能包含空值）/ Indexed extractor function (returns Ret, may contain null)
 * @return 目标集合或错误 / Destination collection or error
 */
suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.tryMapIndexedNotNullToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<R?, T>
): Ret<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    return when (val result = executeTryWithWorkerPool<R?, T>(this, limit) { index, element -> extractor(index, element) }) {
        is Ok -> {
            result.value.filterNotNull().forEach { destination.add(it) }
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行映射集合元素到目标集合（带索引，过滤空值，带错误收集）
 *
 * Map collection elements in parallel to a destination collection with index information, filtering out null results with error collection.
 * 并发映射集合元素到目标集合，提供元素索引信息，过滤掉空值结果，收集所有错误。
 *
 * @param R 结果类型（非空）/ Result type (non-null)
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的提取器函数（返回 Ret，可能包含空值）/ Indexed extractor function (returns Ret, may contain null)
 * @return 目标集合或错误集合 / Destination collection or error collection
 */
suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.exTryMapIndexedNotNullToParallelly(
    destination: C,
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<R?, T>
): ExRet<C> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    return when (val result = executeExTryWithWorkerPool<R?, T>(this, limit) { index, element -> extractor(index, element) }) {
        is Ok -> {
            result.value.filterNotNull().forEach { destination.add(it) }
            Ok(destination)
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> {
            result.value.filterNotNull().forEach { destination.add(it) }
            Warn(destination, result.warnings)
        }
    }
}
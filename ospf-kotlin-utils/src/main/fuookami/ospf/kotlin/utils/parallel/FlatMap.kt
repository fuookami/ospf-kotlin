/**
 * 并行展平映射操作
 *
 * Parallel flat-map operations with concurrency control.
*/
package fuookami.ospf.kotlin.utils.parallel

import fuookami.ospf.kotlin.utils.functional.*

// ============================================================================
// flatMap 系列
// flatMap series
// ============================================================================

/**
 * 并行展平映射集合元素
 *
 * Flat-map collection elements in parallel.
 * 并发展平映射集合元素。
 *
 * @param R 结果元素类型 / Result element type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 展平映射函数 / Flat-map extractor function
 * @return 展平后的结果列表 / Flattened result list
*/
suspend inline fun <R, T> Iterable<T>.flatMapParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendExtractor<Iterable<R>, T>
): List<R> {
    return flatMapToParallelly(ArrayList(), concurrentAmount, extractor)
}

/**
 * 并行展平映射集合元素（带错误处理）
 *
 * Flat-map collection elements in parallel with error handling.
 * 并发展平映射集合元素，支持错误处理。
 *
 * @param R 结果元素类型 / Result element type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 展平映射函数（返回 Ret）/ Flat-map extractor function (returns Ret)
 * @return 展平后的结果列表或错误 / Flattened result list or error
*/
suspend inline fun <R, T> Iterable<T>.tryFlatMapToParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<Iterable<R>, T>
): Ret<List<R>> {
    return tryFlatMapToParallelly(ArrayList(), concurrentAmount, extractor)
}

/**
 * 并行展平映射集合元素（带错误收集）
 *
 * Flat-map collection elements in parallel with error collection.
 * 并发展平映射集合元素，收集所有错误。
 *
 * @param R 结果元素类型 / Result element type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 展平映射函数（返回 Ret）/ Flat-map extractor function (returns Ret)
 * @return 展平后的结果列表或错误集合 / Flattened result list or error collection
*/
suspend inline fun <R, T> Iterable<T>.exTryFlatMapToParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<Iterable<R>, T>
): ExRet<List<R>> {
    return exTryFlatMapToParallelly(ArrayList(), concurrentAmount, extractor)
}

/**
 * 并行展平映射集合元素到目标集合
 *
 * Flat-map collection elements in parallel to a destination collection.
 * 并发展平映射集合元素到目标集合。
 *
 * @param R 结果元素类型 / Result element type
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 展平映射函数 / Flat-map extractor function
 * @return 目标集合 / Destination collection
*/
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

/**
 * 并行展平映射集合元素到目标集合（带错误处理）
 *
 * Flat-map collection elements in parallel to a destination collection with error handling.
 * 并发展平映射集合元素到目标集合，支持错误处理。
 *
 * @param R 结果元素类型 / Result element type
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 展平映射函数（返回 Ret）/ Flat-map extractor function (returns Ret)
 * @return 目标集合或错误 / Destination collection or error
*/
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

/**
 * 并行展平映射集合元素到目标集合（带错误收集）
 *
 * Flat-map collection elements in parallel to a destination collection with error collection.
 * 并发展平映射集合元素到目标集合，收集所有错误。
 *
 * @param R 结果元素类型 / Result element type
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 展平映射函数（返回 Ret）/ Flat-map extractor function (returns Ret)
 * @return 目标集合或错误集合 / Destination collection or error collection
*/
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
// flatMapIndexed series
// ============================================================================

/**
 * 并行展平映射集合元素（带索引）
 *
 * Flat-map collection elements in parallel with index information.
 * 并发展平映射集合元素，提供元素索引信息。
 *
 * @param R 结果元素类型 / Result element type
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的展平映射函数 / Indexed flat-map extractor function
 * @return 展平后的结果列表 / Flattened result list
*/
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendIndexedExtractor<Iterable<R>, T>
): List<R> {
    return flatMapIndexedToParallelly(ArrayList(), concurrentAmount, extractor)
}

/**
 * 并行展平映射集合元素（带索引，带错误处理）
 *
 * Flat-map collection elements in parallel with index information and error handling.
 * 并发展平映射集合元素，提供元素索引信息，支持错误处理。
 *
 * @param R 结果元素类型 / Result element type
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的展平映射函数（返回 Ret）/ Indexed flat-map extractor function (returns Ret)
 * @return 展平后的结果列表或错误 / Flattened result list or error
*/
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapIndexedToParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R>, T>
): Ret<List<R>> {
    return tryFlatMapIndexedToParallelly(ArrayList(), concurrentAmount, extractor)
}

/**
 * 并行展平映射集合元素（带索引，带错误收集）
 *
 * Flat-map collection elements in parallel with index information and error collection.
 * 并发展平映射集合元素，提供元素索引信息，收集所有错误。
 *
 * @param R 结果元素类型 / Result element type
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的展平映射函数（返回 Ret）/ Indexed flat-map extractor function (returns Ret)
 * @return 展平后的结果列表或错误集合 / Flattened result list or error collection
*/
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.exTryFlatMapIndexedToParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R>, T>
): ExRet<List<R>> {
    return exTryFlatMapIndexedToParallelly(ArrayList(), concurrentAmount, extractor)
}

/**
 * 并行展平映射集合元素到目标集合（带索引）
 *
 * Flat-map collection elements in parallel to a destination collection with index information.
 * 并发展平映射集合元素到目标集合，提供元素索引信息。
 *
 * @param R 结果元素类型 / Result element type
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的展平映射函数 / Indexed flat-map extractor function
 * @return 目标集合 / Destination collection
*/
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

/**
 * 并行展平映射集合元素到目标集合（带索引，带错误处理）
 *
 * Flat-map collection elements in parallel to a destination collection with index information and error handling.
 * 并发展平映射集合元素到目标集合，提供元素索引信息，支持错误处理。
 *
 * @param R 结果元素类型 / Result element type
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的展平映射函数（返回 Ret）/ Indexed flat-map extractor function (returns Ret)
 * @return 目标集合或错误 / Destination collection or error
*/
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

/**
 * 并行展平映射集合元素到目标集合（带索引，带错误收集）
 *
 * Flat-map collection elements in parallel to a destination collection with index information and error collection.
 * 并发展平映射集合元素到目标集合，提供元素索引信息，收集所有错误。
 *
 * @param R 结果元素类型 / Result element type
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的展平映射函数（返回 Ret）/ Indexed flat-map extractor function (returns Ret)
 * @return 目标集合或错误集合 / Destination collection or error collection
*/
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
// flatMapIndexedNotNull series
// ============================================================================

/**
 * 并行展平映射集合元素（带索引，过滤空值）
 *
 * Flat-map collection elements in parallel with index information, filtering out null results.
 * 并发展平映射集合元素，提供元素索引信息，过滤掉空值结果。
 *
 * @param R 结果元素类型（非空）/ Result element type (non-null)
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的展平映射函数（可能返回空值）/ Indexed flat-map extractor function (may return null)
 * @return 过滤空值后的展平结果列表 / Flattened result list with nulls filtered out
*/
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedNotNullParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendIndexedExtractor<Iterable<R?>, T>
): List<R> {
    return flatMapIndexedNotNullToParallelly(ArrayList(), concurrentAmount, extractor)
}

/**
 * 并行展平映射集合元素（带索引，过滤空值，带错误处理）
 *
 * Flat-map collection elements in parallel with index information, filtering out null results with error handling.
 * 并发展平映射集合元素，提供元素索引信息，过滤掉空值结果，支持错误处理。
 *
 * @param R 结果元素类型（非空）/ Result element type (non-null)
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的展平映射函数（返回 Ret，可能包含空值）/ Indexed flat-map extractor function (returns Ret, may contain null)
 * @return 过滤空值后的展平结果列表或错误 / Flattened result list with nulls filtered out or error
*/
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapIndexedNotNullToParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R?>, T>
): Ret<List<R>> {
    return tryFlatMapIndexedNotNullToParallelly(ArrayList(), concurrentAmount, extractor)
}

/**
 * 并行展平映射集合元素（带索引，过滤空值，带错误收集）
 *
 * Flat-map collection elements in parallel with index information, filtering out null results with error collection.
 * 并发展平映射集合元素，提供元素索引信息，过滤掉空值结果，收集所有错误。
 *
 * @param R 结果元素类型（非空）/ Result element type (non-null)
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的展平映射函数（返回 Ret，可能包含空值）/ Indexed flat-map extractor function (returns Ret, may contain null)
 * @return 过滤空值后的展平结果列表或错误集合 / Flattened result list with nulls filtered out or error collection
*/
suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.exTryFlatMapIndexedNotNullToParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R?>, T>
): ExRet<List<R>> {
    return exTryFlatMapIndexedNotNullToParallelly(ArrayList(), concurrentAmount, extractor)
}

/**
 * 并行展平映射集合元素到目标集合（带索引，过滤空值）
 *
 * Flat-map collection elements in parallel to a destination collection with index information, filtering out null results.
 * 并发展平映射集合元素到目标集合，提供元素索引信息，过滤掉空值结果。
 *
 * @param R 结果元素类型（非空）/ Result element type (non-null)
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的展平映射函数（可能返回空值）/ Indexed flat-map extractor function (may return null)
 * @return 目标集合 / Destination collection
*/
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

/**
 * 并行展平映射集合元素到目标集合（带索引，过滤空值，带错误处理）
 *
 * Flat-map collection elements in parallel to a destination collection with index information, filtering out null results with error handling.
 * 并发展平映射集合元素到目标集合，提供元素索引信息，过滤掉空值结果，支持错误处理。
 *
 * @param R 结果元素类型（非空）/ Result element type (non-null)
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的展平映射函数（返回 Ret，可能包含空值）/ Indexed flat-map extractor function (returns Ret, may contain null)
 * @return 目标集合或错误 / Destination collection or error
*/
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

/**
 * 并行展平映射集合元素到目标集合（带索引，过滤空值，带错误收集）
 *
 * Flat-map collection elements in parallel to a destination collection with index information, filtering out null results with error collection.
 * 并发展平映射集合元素到目标集合，提供元素索引信息，过滤掉空值结果，收集所有错误。
 *
 * @param R 结果元素类型（非空）/ Result element type (non-null)
 * @param T 元素类型 / Element type
 * @param C 目标集合类型 / Destination collection type
 * @param destination 目标集合 / Destination collection
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 带索引的展平映射函数（返回 Ret，可能包含空值）/ Indexed flat-map extractor function (returns Ret, may contain null)
 * @return 目标集合或错误集合 / Destination collection or error collection
*/
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

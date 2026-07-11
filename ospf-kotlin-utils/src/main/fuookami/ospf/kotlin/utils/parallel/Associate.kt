/**
 * 并行关联操作
 *
 * Parallel association operations for creating maps from iterables with concurrency control.
*/
package fuookami.ospf.kotlin.utils.parallel

import fuookami.ospf.kotlin.utils.functional.*

// ============================================================================
// associate 系列
// associate series
// ============================================================================

/**
 * 并行关联集合元素到 Map
 *
 * Associate collection elements to a Map in parallel.
 * 并发将集合元素关联到 Map。
 *
 * @param K 键类型 / Key type
 * @param V 值类型 / Value type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 提取键值对的函数 / Key-value pair extractor function
 * @return 关联后的 Map / Associated Map
*/
suspend inline fun <K, V, T> Iterable<T>.associateParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendExtractor<Pair<K, V>, T>
): Map<K, V> {
    return associateToParallelly(LinkedHashMap(), concurrentAmount, extractor)
}

/**
 * 并行关联集合元素到 Map（带错误处理）
 *
 * Associate collection elements to a Map in parallel with error handling.
 * 并发将集合元素关联到 Map，支持错误处理。
 *
 * @param K 键类型 / Key type
 * @param V 值类型 / Value type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 提取键值对的函数（返回 Ret）/ Key-value pair extractor function (returns Ret)
 * @return 关联后的 Map 或错误 / Associated Map or error
*/
suspend inline fun <K, V, T> Iterable<T>.tryAssociateToParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<Pair<K, V>, T>
): Ret<Map<K, V>> {
    return tryAssociateToParallelly(LinkedHashMap(), concurrentAmount, extractor)
}

/**
 * 并行关联集合元素到 Map（带错误收集）
 *
 * Associate collection elements to a Map in parallel with error collection.
 * 并发将集合元素关联到 Map，收集所有错误。
 *
 * @param K 键类型 / Key type
 * @param V 值类型 / Value type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 提取键值对的函数（返回 Ret）/ Key-value pair extractor function (returns Ret)
 * @return 关联后的 Map 或错误集合 / Associated Map or error collection
*/
suspend inline fun <K, V, T> Iterable<T>.exTryAssociateToParallelly(
    concurrentAmount: ULong? = null,
    crossinline extractor: SuspendTryExtractor<Pair<K, V>, T>
): ExRet<Map<K, V>> {
    return exTryAssociateToParallelly(LinkedHashMap(), concurrentAmount, extractor)
}

/**
 * 并行关联集合元素到目标 Map
 *
 * Associate collection elements to a destination Map in parallel.
 * 并发将集合元素关联到目标 Map。
 *
 * @param K 键类型 / Key type
 * @param V 值类型 / Value type
 * @param T 元素类型 / Element type
 * @param M 目标 Map 类型 / Destination Map type
 * @param destination 目标 Map / Destination Map
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 提取键值对的函数 / Key-value pair extractor function
 * @return 目标 Map / Destination Map
*/
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

/**
 * 并行关联集合元素到目标 Map（带错误处理）
 *
 * Associate collection elements to a destination Map in parallel with error handling.
 * 并发将集合元素关联到目标 Map，支持错误处理。
 *
 * @param K 键类型 / Key type
 * @param V 值类型 / Value type
 * @param T 元素类型 / Element type
 * @param M 目标 Map 类型 / Destination Map type
 * @param destination 目标 Map / Destination Map
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 提取键值对的函数（返回 Ret）/ Key-value pair extractor function (returns Ret)
 * @return 目标 Map 或错误 / Destination Map or error
*/
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

/**
 * 并行关联集合元素到目标 Map（带错误收集）
 *
 * Associate collection elements to a destination Map in parallel with error collection.
 * 并发将集合元素关联到目标 Map，收集所有错误。
 *
 * @param K 键类型 / Key type
 * @param V 值类型 / Value type
 * @param T 元素类型 / Element type
 * @param M 目标 Map 类型 / Destination Map type
 * @param destination 目标 Map / Destination Map
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param extractor 提取键值对的函数（返回 Ret）/ Key-value pair extractor function (returns Ret)
 * @return 目标 Map 或错误集合 / Destination Map or error collection
*/
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
// associateBy series
// ============================================================================

/**
 * 并行按键关联集合元素到 Map
 *
 * Associate collection elements to a Map by key in parallel.
 * 并发按键将集合元素关联到 Map。
 *
 * @param K 键类型 / Key type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param keyExtractor 键提取函数 / Key extractor function
 * @return 关联后的 Map / Associated Map
*/
suspend inline fun <K, T> Iterable<T>.associateByParallelly(
    concurrentAmount: ULong? = null,
    crossinline keyExtractor: SuspendExtractor<K, T>
): Map<K, T> {
    return associateByToParallelly(LinkedHashMap(), concurrentAmount, keyExtractor)
}

/**
 * 并行按键关联集合元素到 Map（带错误处理）
 *
 * Associate collection elements to a Map by key in parallel with error handling.
 * 并发按键将集合元素关联到 Map，支持错误处理。
 *
 * @param K 键类型 / Key type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param keyExtractor 键提取函数（返回 Ret）/ Key extractor function (returns Ret)
 * @return 关联后的 Map 或错误 / Associated Map or error
*/
suspend inline fun <K, T> Iterable<T>.tryAssociateByToParallelly(
    concurrentAmount: ULong? = null,
    crossinline keyExtractor: SuspendTryExtractor<K, T>
): Ret<Map<K, T>> {
    return tryAssociateByToParallelly(LinkedHashMap(), concurrentAmount, keyExtractor)
}

/**
 * 并行按键关联集合元素到 Map（带错误收集）
 *
 * Associate collection elements to a Map by key in parallel with error collection.
 * 并发按键将集合元素关联到 Map，收集所有错误。
 *
 * @param K 键类型 / Key type
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param keyExtractor 键提取函数（返回 Ret）/ Key extractor function (returns Ret)
 * @return 关联后的 Map 或错误集合 / Associated Map or error collection
*/
suspend inline fun <K, T> Iterable<T>.exTryAssociateByToParallelly(
    concurrentAmount: ULong? = null,
    crossinline keyExtractor: SuspendTryExtractor<K, T>
): ExRet<Map<K, T>> {
    return exTryAssociateByToParallelly(LinkedHashMap(), concurrentAmount, keyExtractor)
}

/**
 * 并行按键关联集合元素到目标 Map
 *
 * Associate collection elements to a destination Map by key in parallel.
 * 并发按键将集合元素关联到目标 Map。
 *
 * @param K 键类型 / Key type
 * @param T 元素类型 / Element type
 * @param M 目标 Map 类型 / Destination Map type
 * @param destination 目标 Map / Destination Map
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param keyExtractor 键提取函数 / Key extractor function
 * @return 目标 Map / Destination Map
*/
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

/**
 * 并行按键关联集合元素到目标 Map（带错误处理）
 *
 * Associate collection elements to a destination Map by key in parallel with error handling.
 * 并发按键将集合元素关联到目标 Map，支持错误处理。
 *
 * @param K 键类型 / Key type
 * @param T 元素类型 / Element type
 * @param M 目标 Map 类型 / Destination Map type
 * @param destination 目标 Map / Destination Map
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param keyExtractor 键提取函数（返回 Ret）/ Key extractor function (returns Ret)
 * @return 目标 Map 或错误 / Destination Map or error
*/
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

/**
 * 并行按键关联集合元素到目标 Map（带错误收集）
 *
 * Associate collection elements to a destination Map by key in parallel with error collection.
 * 并发按键将集合元素关联到目标 Map，收集所有错误。
 *
 * @param K 键类型 / Key type
 * @param T 元素类型 / Element type
 * @param M 目标 Map 类型 / Destination Map type
 * @param destination 目标 Map / Destination Map
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param keyExtractor 键提取函数（返回 Ret）/ Key extractor function (returns Ret)
 * @return 目标 Map 或错误集合 / Destination Map or error collection
*/
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
// associateWith series
// ============================================================================

/**
 * 并行按值关联集合元素到 Map
 *
 * Associate collection elements to a Map with value in parallel.
 * 并发按值将集合元素关联到 Map。
 *
 * @param V 值类型 / Value type
 * @param T 元素类型（作为键）/ Element type (as key)
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param valueExtractor 值提取函数 / Value extractor function
 * @return 关联后的 Map / Associated Map
*/
suspend inline fun <V, T> Iterable<T>.associateWithParallelly(
    concurrentAmount: ULong? = null,
    crossinline valueExtractor: SuspendExtractor<V, T>
): Map<T, V> {
    return associateWithToParallelly(LinkedHashMap(), concurrentAmount, valueExtractor)
}

/**
 * 并行按值关联集合元素到 Map（带错误处理）
 *
 * Associate collection elements to a Map with value in parallel with error handling.
 * 并发按值将集合元素关联到 Map，支持错误处理。
 *
 * @param V 值类型 / Value type
 * @param T 元素类型（作为键）/ Element type (as key)
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param valueExtractor 值提取函数（返回 Ret）/ Value extractor function (returns Ret)
 * @return 关联后的 Map 或错误 / Associated Map or error
*/
suspend inline fun <V, T> Iterable<T>.tryAssociateWithToParallelly(
    concurrentAmount: ULong? = null,
    crossinline valueExtractor: SuspendTryExtractor<V, T>
): Ret<Map<T, V>> {
    return tryAssociateWithToParallelly(LinkedHashMap(), concurrentAmount, valueExtractor)
}

/**
 * 并行按值关联集合元素到 Map（带错误收集）
 *
 * Associate collection elements to a Map with value in parallel with error collection.
 * 并发按值将集合元素关联到 Map，收集所有错误。
 *
 * @param V 值类型 / Value type
 * @param T 元素类型（作为键）/ Element type (as key)
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param valueExtractor 值提取函数（返回 Ret）/ Value extractor function (returns Ret)
 * @return 关联后的 Map 或错误集合 / Associated Map or error collection
*/
suspend inline fun <V, T> Iterable<T>.exTryAssociateWithToParallelly(
    concurrentAmount: ULong? = null,
    crossinline valueExtractor: SuspendTryExtractor<V, T>
): ExRet<Map<T, V>> {
    return exTryAssociateWithToParallelly(LinkedHashMap(), concurrentAmount, valueExtractor)
}

/**
 * 并行按值关联集合元素到目标 Map
 *
 * Associate collection elements to a destination Map with value in parallel.
 * 并发按值将集合元素关联到目标 Map。
 *
 * @param V 值类型 / Value type
 * @param T 元素类型（作为键）/ Element type (as key)
 * @param M 目标 Map 类型 / Destination Map type
 * @param destination 目标 Map / Destination Map
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param valueExtractor 值提取函数 / Value extractor function
 * @return 目标 Map / Destination Map
*/
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

/**
 * 并行按值关联集合元素到目标 Map（带错误处理）
 *
 * Associate collection elements to a destination Map with value in parallel with error handling.
 * 并发按值将集合元素关联到目标 Map，支持错误处理。
 *
 * @param V 值类型 / Value type
 * @param T 元素类型（作为键）/ Element type (as key)
 * @param M 目标 Map 类型 / Destination Map type
 * @param destination 目标 Map / Destination Map
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param valueExtractor 值提取函数（返回 Ret）/ Value extractor function (returns Ret)
 * @return 目标 Map 或错误 / Destination Map or error
*/
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

/**
 * 并行按值关联集合元素到目标 Map（带错误收集）
 *
 * Associate collection elements to a destination Map with value in parallel with error collection.
 * 并发按值将集合元素关联到目标 Map，收集所有错误。
 *
 * @param V 值类型 / Value type
 * @param T 元素类型（作为键）/ Element type (as key)
 * @param M 目标 Map 类型 / Destination Map type
 * @param destination 目标 Map / Destination Map
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param valueExtractor 值提取函数（返回 Ret）/ Value extractor function (returns Ret)
 * @return 目标 Map 或错误集合 / Destination Map or error collection
*/
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

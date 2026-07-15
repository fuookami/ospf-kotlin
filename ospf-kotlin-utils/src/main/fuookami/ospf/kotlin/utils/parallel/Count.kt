/**
 * 并行计数操作
 *
 * Parallel counting operations with concurrency control.
*/
package fuookami.ospf.kotlin.utils.parallel

import fuookami.ospf.kotlin.utils.functional.*

/**
 * 并行计数满足条件的元素
 *
 * Count elements that satisfy the predicate in parallel with concurrency control.
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件 / Predicate function
 * @return 满足条件的元素数量 / Count of elements satisfying the predicate
*/
suspend inline fun <T> Iterable<T>.countParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): Int {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val results = executePredicateWithWorkerPool(this, limit) { _, element -> predicate(element) }
    return results.count { it }
}

/**
 * 并行计数满足条件的元素（带错误处理）
 *
 * Count elements that satisfy the predicate in parallel with error handling.
 * 并发计数满足条件的元素，支持错误处理。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 计数结果或错误 / Count result or error
*/
suspend inline fun <T> Iterable<T>.tryCountParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Int> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeTryPredicateWithWorkerPool(this, limit) { _, element -> predicate(element) }
    return when (result) {
        is Ok -> Ok(result.value.count { it })
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行计数满足条件的元素（带错误收集）
 *
 * Count elements that satisfy the predicate in parallel with error collection.
 * 并发计数满足条件的元素，收集所有错误。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 计数结果或错误集合 / Count result or error collection
*/
suspend inline fun <T> Iterable<T>.exTryCountParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<Int> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeExTryWithWorkerPool<Boolean, T>(this, limit) { _, element -> predicate(element) }
    return when (result) {
        is Ok -> Ok(result.value.count { it })
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> Ok(result.value.count { it })
    }
}

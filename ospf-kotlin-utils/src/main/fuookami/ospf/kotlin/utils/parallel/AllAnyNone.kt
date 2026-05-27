/**
 * 并行判断操作
 *
 * Parallel predicate operations (all, any, none) with concurrency control.
 */
package fuookami.ospf.kotlin.utils.parallel

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.ExRet
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Warn
import fuookami.ospf.kotlin.utils.functional.SuspendPredicate
import fuookami.ospf.kotlin.utils.functional.SuspendTryPredicate

/**
 * 并行判断是否所有元素都满足条件
 *
 * Check if all elements satisfy the predicate in parallel with concurrency control.
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件 / Predicate function
 * @return 是否所有元素都满足条件 / Whether all elements satisfy the predicate
 */
suspend inline fun <T> Iterable<T>.allParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): Boolean {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val results = executePredicateWithWorkerPool(this, limit) { _, element -> predicate(element) }
    return results.all { it }
}

/**
 * 并行判断是否所有元素都满足条件（带错误处理）
 *
 * Check if all elements satisfy the predicate in parallel with error handling.
 * 并发判断是否所有元素都满足条件，支持错误处理。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 判断结果或错误 / Boolean result or error
 */
suspend inline fun <T> Iterable<T>.tryAllParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Boolean> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeTryPredicateWithWorkerPool(this, limit) { _, element -> predicate(element) }
    return when (result) {
        is Ok -> Ok(result.value.all { it })
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行判断是否所有元素都满足条件（带错误收集）
 *
 * Check if all elements satisfy the predicate in parallel with error collection.
 * 并发判断是否所有元素都满足条件，收集所有错误。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 判断结果或错误集合 / Boolean result or error collection
 */
suspend inline fun <T> Iterable<T>.exTryAllParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<Boolean> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeExTryWithWorkerPool<Boolean, T>(this, limit) { _, element -> predicate(element) }
    return when (result) {
        is Ok -> Ok(result.value.all { it })
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> Ok(result.value.all { it })
    }
}

/**
 * 并行判断是否存在元素满足条件
 *
 * Check if any element satisfies the predicate in parallel with concurrency control.
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件 / Predicate function
 * @return 是否存在元素满足条件 / Whether any element satisfies the predicate
 */
suspend inline fun <T> Iterable<T>.anyParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): Boolean {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val results = executePredicateWithWorkerPool(this, limit) { _, element -> predicate(element) }
    return results.any { it }
}

/**
 * 并行判断是否存在元素满足条件（带错误处理）
 *
 * Check if any element satisfies the predicate in parallel with error handling.
 * 并发判断是否存在元素满足条件，支持错误处理。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 判断结果或错误 / Boolean result or error
 */
suspend inline fun <T> Iterable<T>.tryAnyParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Boolean> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeTryPredicateWithWorkerPool(this, limit) { _, element -> predicate(element) }
    return when (result) {
        is Ok -> Ok(result.value.any { it })
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 并行判断是否存在元素满足条件（带错误收集）
 *
 * Check if any element satisfies the predicate in parallel with error collection.
 * 并发判断是否存在元素满足条件，收集所有错误。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 判断结果或错误集合 / Boolean result or error collection
 */
suspend inline fun <T> Iterable<T>.exTryAnyParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<Boolean> {
    val limit = resolveConcurrentAmount(concurrentAmount, this.defaultConcurrentAmount)
    val result = executeExTryWithWorkerPool<Boolean, T>(this, limit) { _, element -> predicate(element) }
    return when (result) {
        is Ok -> Ok(result.value.any { it })
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Warn -> Ok(result.value.any { it })
    }
}

/**
 * 并行判断是否没有元素满足条件
 *
 * Check if no element satisfies the predicate in parallel with concurrency control.
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件 / Predicate function
 * @return 是否没有元素满足条件 / Whether no element satisfies the predicate
 */
suspend inline fun <T> Iterable<T>.noneParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendPredicate<T>
): Boolean {
    return !anyParallelly(concurrentAmount, predicate)
}

/**
 * 并行判断是否没有元素满足条件（带错误处理）
 *
 * Check if no element satisfies the predicate in parallel with error handling.
 * 并发判断是否没有元素满足条件，支持错误处理。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 判断结果或错误 / Boolean result or error
 */
suspend inline fun <T> Iterable<T>.tryNoneParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Boolean> {
    return when (val ret = tryAnyParallelly(concurrentAmount, predicate)) {
        is Ok -> Ok(!ret.value)
        is Failed -> Failed(ret.error)
        is Fatal -> Fatal(ret.errors)
    }
}

/**
 * 并行判断是否没有元素满足条件（带错误收集）
 *
 * Check if no element satisfies the predicate in parallel with error collection.
 * 并发判断是否没有元素满足条件，收集所有错误。
 *
 * @param T 元素类型 / Element type
 * @param concurrentAmount 并发上限，默认使用 defaultConcurrentAmount / Concurrency limit, defaults to defaultConcurrentAmount
 * @param predicate 判断条件（返回 Ret）/ Predicate function (returns Ret)
 * @return 判断结果或错误集合 / Boolean result or error collection
 */
suspend inline fun <T> Iterable<T>.exTryNoneParallelly(
    concurrentAmount: ULong? = null,
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<Boolean> {
    return when (val ret = exTryAnyParallelly(concurrentAmount, predicate)) {
        is Ok -> Ok(!ret.value)
        is Failed -> Failed(ret.error)
        is Fatal -> Fatal(ret.errors)
        is Warn -> Ok(!ret.value)
    }
}

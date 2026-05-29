@file:JvmName("MathParallelFoldKt")
package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.operator.Plus

/**
 * 并行折叠
 * Parallel Fold Operations
 *
 * 提供并行计算集合总和的功能，使用 Kotlin 协程实现并发计算。
 * 将集合按 chunkSize 分块，每个块在独立协程中计算部分和，最后合并结果。
 * sumOfParallelly：并行计算总和，使甌suspend 提取器从元素中提取值，
 * 要求元素类型支持 Plus 操作（加法）和Arithmetic 常量。
 * trySumOfParallelly：并行计算总和并处理错误，提取器返囌Ret 类型，
 * 遇到 Failed 戌Fatal 错误时立即中止并返回错误。
 * exTrySumOfParallelly：并行计算总和并收集所有错误，
 * 不立即中止，而是继续计算并收集所有错误，最终返囌ExRet 类型。
 * 边界情况：空集合返回 zero，chunkSize 默认丌100。
 * 使用 Dispatchers.Default 作为协程调度器，适合 CPU 密集型计算。
 * 线程安全：每个协程独立计算部分和，最终合并使用单线程串行操作。
 *
 * Provides parallel sum computation functionality using Kotlin coroutines for concurrent execution.
 * Splits collection by chunkSize, each chunk computed in separate coroutine, results merged at end.
 * sumOfParallelly: parallel sum computation using suspend extractor to extract values from elements,
 * requires element type supporting Plus operation and Arithmetic constants.
 * trySumOfParallelly: parallel sum with error handling, extractor returns Ret type,
 * aborts immediately on Failed or Fatal error and returns error.
 * exTrySumOfParallelly: parallel sum collecting all errors,
 * continues computation collecting all errors, finally returns ExRet type.
 * Boundary cases: empty collection returns zero; default chunkSize is 100.
 * Uses Dispatchers.Default as coroutine dispatcher, suitable for CPU-intensive computation.
 * Thread-safe: each coroutine computes partial sum independently, final merge uses single-thread serial operation.
 */

/** 从 Ret 结果中提取错误并追加到列表 / Extract errors from Ret result and append to list */
@PublishedApi
internal fun MutableList<Error<ErrorCode>>.appendFrom(ret: Ret<*>) {
    when (ret) {
        is Ok -> {}
        is Failed -> add(ret.error)
        is Fatal -> addAll(ret.errors)
    }
}

/** 根据值和错误列表构造 ExRet 结果 / Construct ExRet result from value and error list */
@PublishedApi
internal fun <T> exResultOf(value: T, errors: List<Error<ErrorCode>>): ExRet<T> {
    return if (errors.isEmpty()) {
        Ok(value)
    } else {
        Fatal(errors)
    }
}

/** 并行计算集合元素之和，使用 suspend 提取器提取值 / Compute sum of collection elements in parallel using suspend extractor */
suspend inline fun <T, U> Iterable<T>.sumOfParallelly(
    constants: ArithmeticConstants<U>,
    chunkSize: Int = 100,
    crossinline extractor: SuspendExtractor<U, T>
): U where U : Arithmetic<U>, U : Plus<U, U> {
    return coroutineScope {
        val elements = this@sumOfParallelly.toList()
        val chunks = elements.chunked(chunkSize)

        val promises = ArrayList<Deferred<U>>()
        for (chunk in chunks) {
            promises.add(async(Dispatchers.Default) {
                chunk.fold(constants.zero) { acc, element -> acc + extractor(element) }
            })
        }

        var sum = constants.zero
        for (promise in promises) {
            sum = sum + promise.await()
        }
        sum
    }
}

/** 并行计算集合元素之和（自动解析常量） / Compute sum of collection elements in parallel (auto-resolve constants) */
suspend inline fun <T, reified U> Iterable<T>.sumOfParallelly(
    chunkSize: Int = 100,
    crossinline extractor: SuspendExtractor<U, T>
): U where U : Arithmetic<U>, U : Plus<U, U> {
    return sumOfParallelly(resolveArithmeticConstants<U>("Fold"), chunkSize, extractor)
}

/** 并行计算集合元素之和（带错误处理），遇到错误时立即中止 / Compute sum in parallel with error handling, aborts on first error */
suspend inline fun <T, U> Iterable<T>.trySumOfParallelly(
    constants: ArithmeticConstants<U>,
    chunkSize: Int = 100,
    crossinline extractor: SuspendTryExtractor<U, T>
): Ret<U> where U : Arithmetic<U>, U : Plus<U, U> {
    return coroutineScope {
        val elements = this@trySumOfParallelly.toList()
        val chunks = elements.chunked(chunkSize)

        val promises = ArrayList<Deferred<Ret<U>>>()
        for (chunk in chunks) {
            promises.add(async(Dispatchers.Default) {
                var sum = constants.zero
                for (element in chunk) {
                    when (val ret = extractor(element)) {
                        is Ok -> sum = sum + ret.value
                        is Failed -> return@async Failed(ret.error)
                        is Fatal -> return@async Fatal(ret.errors)
                    }
                }
                Ok(sum)
            })
        }

        var sum = constants.zero
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> sum = sum + ret.value
                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(sum)
    }
}

/** 并行计算集合元素之和（带错误处理，自动解析常量） / Compute sum in parallel with error handling (auto-resolve constants) */
suspend inline fun <T, reified U> Iterable<T>.trySumOfParallelly(
    chunkSize: Int = 100,
    crossinline extractor: SuspendTryExtractor<U, T>
): Ret<U> where U : Arithmetic<U>, U : Plus<U, U> {
    return trySumOfParallelly(resolveArithmeticConstants<U>("Fold"), chunkSize, extractor)
}

/** 并行计算集合元素之和（收集所有错误），继续计算并收集所有错误 / Compute sum in parallel collecting all errors, continues computation */
suspend inline fun <T, U> Iterable<T>.exTrySumOfParallelly(
    constants: ArithmeticConstants<U>,
    chunkSize: Int = 100,
    crossinline extractor: SuspendTryExtractor<U, T>
): ExRet<U> where U : Arithmetic<U>, U : Plus<U, U> {
    return coroutineScope {
        val elements = this@exTrySumOfParallelly.toList()
        val chunks = elements.chunked(chunkSize)

        val promises = ArrayList<Deferred<Pair<U, List<Error<ErrorCode>>>>>()
        for (chunk in chunks) {
            promises.add(async(Dispatchers.Default) {
                val errors = ArrayList<Error<ErrorCode>>()
                var sum = constants.zero
                for (element in chunk) {
                    when (val ret = extractor(element)) {
                        is Ok -> sum = sum + ret.value
                        is Failed, is Fatal -> errors.appendFrom(ret)
                    }
                }
                Pair(sum, errors)
            })
        }

        val errors = ArrayList<Error<ErrorCode>>()
        var sum = constants.zero
        for (promise in promises) {
            val (chunkSum, chunkErrors) = promise.await()
            sum = sum + chunkSum
            errors.addAll(chunkErrors)
        }
        exResultOf(sum, errors)
    }
}

/** 并行计算集合元素之和（收集所有错误，自动解析常量） / Compute sum in parallel collecting all errors (auto-resolve constants) */
suspend inline fun <T, reified U> Iterable<T>.exTrySumOfParallelly(
    chunkSize: Int = 100,
    crossinline extractor: SuspendTryExtractor<U, T>
): ExRet<U> where U : Arithmetic<U>, U : Plus<U, U> {
    return exTrySumOfParallelly(resolveArithmeticConstants<U>("Fold"), chunkSize, extractor)
}

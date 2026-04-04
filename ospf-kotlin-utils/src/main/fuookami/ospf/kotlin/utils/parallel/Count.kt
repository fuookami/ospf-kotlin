package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.ExRet
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.SuspendPredicate
import fuookami.ospf.kotlin.utils.functional.SuspendTryPredicate

/**
 * 并行计数操作
 *
 * Parallel counting operations.
 *
 * UTL-005 TODO: 添加 concurrentAmount 参数控制并发上限
 * UTL-005 TODO: Add concurrentAmount parameter for concurrency control.
 */

/**
 * 并行计数满足条件的元素
 *
 * Count elements that satisfy the predicate in parallel.
 *
 * @param T 元素类型 / Element type
 * @param predicate 判断条件 / Predicate function
 * @return 满足条件的元素数量 / Count of elements satisfying the predicate
 */
suspend inline fun <T> Iterable<T>.countParallelly(
    crossinline predicate: SuspendPredicate<T>
): Int {
    return coroutineScope {
        val promises = ArrayList<Deferred<Boolean>>()
        for (element in this@countParallelly) {
            promises.add(async(Dispatchers.Default) { predicate(element) })
        }
        promises.count { it.await() }
    }
}

suspend inline fun <T> Iterable<T>.tryCountParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Int> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Boolean>>>()
        for (element in this@tryCountParallelly) {
            promises.add(async(Dispatchers.Default) { predicate(element) })
        }
        var count = 0
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> if (ret.value) ++count
                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(count)
    }
}

suspend inline fun <T> Iterable<T>.exTryCountParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<Int> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Boolean>>>()
        for (element in this@exTryCountParallelly) {
            promises.add(async(Dispatchers.Default) { predicate(element) })
        }
        val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error>()
        var count = 0
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> if (ret.value) {
                    ++count
                }

                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(count, errors)
    }
}

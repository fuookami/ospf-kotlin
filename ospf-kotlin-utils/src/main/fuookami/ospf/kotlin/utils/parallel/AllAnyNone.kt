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
import fuookami.ospf.kotlin.utils.functional.Warn
import fuookami.ospf.kotlin.utils.functional.SuspendPredicate
import fuookami.ospf.kotlin.utils.functional.SuspendTryPredicate

/**
 * 并行判断操作
 *
 * Parallel predicate operations (all, any, none).
 *
 * UTL-005 TODO: 添加 concurrentAmount 参数控制并发上限
 * UTL-005 TODO: Add concurrentAmount parameter for concurrency control.
 */

/**
 * 并行判断是否所有元素都满足条件
 *
 * Check if all elements satisfy the predicate in parallel.
 *
 * @param T 元素类型 / Element type
 * @param predicate 判断条件 / Predicate function
 * @return 是否所有元素都满足条件 / Whether all elements satisfy the predicate
 */
suspend inline fun <T> Iterable<T>.allParallelly(
    crossinline predicate: SuspendPredicate<T>
): Boolean {
    return coroutineScope {
        val promises = ArrayList<Deferred<Boolean>>()
        for (element in this@allParallelly) {
            promises.add(async(Dispatchers.Default) { predicate(element) })
        }
        promises.all { it.await() }
    }
}

suspend inline fun <T> Iterable<T>.tryAllParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Boolean> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Boolean>>>()
        for (element in this@tryAllParallelly) {
            promises.add(async(Dispatchers.Default) { predicate(element) })
        }
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> {
                    if (!ret.value) {
                        return@coroutineScope Ok(false)
                    }
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(true)
    }
}

suspend inline fun <T> Iterable<T>.exTryAllParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<Boolean> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Boolean>>>()
        for (element in this@exTryAllParallelly) {
            promises.add(async(Dispatchers.Default) { predicate(element) })
        }
        val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error>()
        var all = true
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> if (!ret.value) {
                    all = false
                }

                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(all, errors)
    }
}

suspend inline fun <T> Iterable<T>.anyParallelly(
    crossinline predicate: SuspendPredicate<T>
): Boolean {
    return coroutineScope {
        val promises = ArrayList<Deferred<Boolean>>()
        for (element in this@anyParallelly) {
            promises.add(async(Dispatchers.Default) { predicate(element) })
        }
        promises.any { it.await() }
    }
}

suspend inline fun <T> Iterable<T>.tryAnyParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Boolean> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Boolean>>>()
        for (element in this@tryAnyParallelly) {
            promises.add(async(Dispatchers.Default) { predicate(element) })
        }
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> {
                    if (ret.value) {
                        return@coroutineScope Ok(true)
                    }
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(false)
    }
}

suspend inline fun <T> Iterable<T>.exTryAnyParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<Boolean> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Boolean>>>()
        for (element in this@exTryAnyParallelly) {
            promises.add(async(Dispatchers.Default) { predicate(element) })
        }
        val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error>()
        var any = false
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> if (ret.value) {
                    any = true
                }

                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(any, errors)
    }
}

suspend inline fun <T> Iterable<T>.noneParallelly(
    crossinline predicate: SuspendPredicate<T>
): Boolean {
    return !anyParallelly(predicate)
}

suspend inline fun <T> Iterable<T>.tryNoneParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Boolean> {
    return when (val ret = tryAnyParallelly(predicate)) {
        is Ok -> Ok(!ret.value)
        is Failed -> Failed(ret.error)
        is Fatal -> Fatal(ret.errors)
    }
}

suspend inline fun <T> Iterable<T>.exTryNoneParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): ExRet<Boolean> {
    return when (val ret = exTryAnyParallelly(predicate)) {
        is Ok -> Ok(!ret.value)
        is Failed -> Failed(ret.error)
        is Fatal -> Fatal(ret.errors)
        is Warn -> Warn(!ret.value, ret.warnings)
    }
}

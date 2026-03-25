package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.SuspendPredicate
import fuookami.ospf.kotlin.utils.functional.SuspendTryPredicate

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

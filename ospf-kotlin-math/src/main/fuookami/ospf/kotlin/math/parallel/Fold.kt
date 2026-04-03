package fuookami.ospf.kotlin.math.parallel

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.SuspendExtractor
import fuookami.ospf.kotlin.utils.functional.SuspendTryExtractor
import fuookami.ospf.kotlin.math.algebra.concept.Arithmetic
import fuookami.ospf.kotlin.math.algebra.concept.ArithmeticConstants
import fuookami.ospf.kotlin.math.algebra.concept.resolveArithmeticConstants
import fuookami.ospf.kotlin.math.operator.Plus

@PublishedApi
internal fun MutableList<fuookami.ospf.kotlin.utils.error.Error>.appendFrom(ret: Ret<*>) {
    when (ret) {
        is Ok -> {}
        is Failed -> add(ret.error)
        is Fatal -> addAll(ret.errors)
    }
}

@PublishedApi
internal fun <T> exResultOf(value: T, errors: List<fuookami.ospf.kotlin.utils.error.Error>): fuookami.ospf.kotlin.utils.functional.ExRet<T> {
    return if (errors.isEmpty()) {
        Ok(value)
    } else {
        Fatal(errors)
    }
}

suspend inline fun <T, U> Iterable<T>.sumOfParallelly(
    constants: ArithmeticConstants<U>,
    crossinline extractor: SuspendExtractor<U, T>
): U where U : Arithmetic<U>, U : Plus<U, U> {
    return coroutineScope {
        val promises = ArrayList<Deferred<U>>()
        for (element in this@sumOfParallelly) {
            promises.add(async(Dispatchers.Default) { extractor(element) })
        }
        var sum = constants.zero
        for (promise in promises) {
            sum = sum + promise.await()
        }
        sum
    }
}

suspend inline fun <T, reified U> Iterable<T>.sumOfParallelly(
    crossinline extractor: SuspendExtractor<U, T>
): U where U : Arithmetic<U>, U : Plus<U, U> {
    return sumOfParallelly(resolveArithmeticConstants<U>("Fold"), extractor)
}

suspend inline fun <T, U> Iterable<T>.trySumOfParallelly(
    constants: ArithmeticConstants<U>,
    crossinline extractor: SuspendTryExtractor<U, T>
): Ret<U> where U : Arithmetic<U>, U : Plus<U, U> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<U>>>()
        for (element in this@trySumOfParallelly) {
            promises.add(async(Dispatchers.Default) { extractor(element) })
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

suspend inline fun <T, reified U> Iterable<T>.trySumOfParallelly(
    crossinline extractor: SuspendTryExtractor<U, T>
): Ret<U> where U : Arithmetic<U>, U : Plus<U, U> {
    return trySumOfParallelly(resolveArithmeticConstants<U>("Fold"), extractor)
}

suspend inline fun <T, U> Iterable<T>.exTrySumOfParallelly(
    constants: ArithmeticConstants<U>,
    crossinline extractor: SuspendTryExtractor<U, T>
): fuookami.ospf.kotlin.utils.functional.ExRet<U> where U : Arithmetic<U>, U : Plus<U, U> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<U>>>()
        for (element in this@exTrySumOfParallelly) {
            promises.add(async(Dispatchers.Default) { extractor(element) })
        }
        val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error>()
        var sum = constants.zero
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> sum = sum + ret.value
                is Failed, is Fatal -> errors.appendFrom(ret)
            }
        }
        exResultOf(sum, errors)
    }
}

suspend inline fun <T, reified U> Iterable<T>.exTrySumOfParallelly(
    crossinline extractor: SuspendTryExtractor<U, T>
): fuookami.ospf.kotlin.utils.functional.ExRet<U> where U : Arithmetic<U>, U : Plus<U, U> {
    return exTrySumOfParallelly(resolveArithmeticConstants<U>("Fold"), extractor)
}
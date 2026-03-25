package fuookami.ospf.kotlin.utils.parallel

import kotlin.reflect.full.companionObjectInstance
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
import fuookami.ospf.kotlin.utils.math.Arithmetic
import fuookami.ospf.kotlin.utils.math.ArithmeticConstants
import fuookami.ospf.kotlin.utils.math.UInt64
import fuookami.ospf.kotlin.utils.operator.Plus

@Suppress("UNCHECKED_CAST")
suspend inline fun <T, reified U> Iterable<T>.sumOfParallelly(
    crossinline extractor: SuspendExtractor<U, T>
): U where U : Arithmetic<U>, U : Plus<U, U> {
    return coroutineScope {
        val promises = ArrayList<Deferred<U>>()
        for (element in this@sumOfParallelly) {
            promises.add(async(Dispatchers.Default) { extractor(element) })
        }
        var sum = (U::class.companionObjectInstance as ArithmeticConstants<U>).zero
        for (promise in promises) {
            sum = sum + promise.await()
        }
        sum
    }
}

@Suppress("UNCHECKED_CAST")
suspend inline fun <T, reified U> Iterable<T>.trySumOfParallelly(
    crossinline extractor: SuspendTryExtractor<U, T>
): Ret<U> where U : Arithmetic<U>, U : Plus<U, U> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<U>>>()
        for (element in this@trySumOfParallelly) {
            promises.add(async(Dispatchers.Default) { extractor(element) })
        }
        var sum = (U::class.companionObjectInstance as ArithmeticConstants<U>).zero
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

suspend inline fun <T> Iterable<T>.foldParallelly(
    initial: T,
    crossinline operation: suspend (acc: T, T) -> T
): T {
    return foldParallelly(segment = UInt64.ten, initial = initial, operation = operation)
}

suspend inline fun <T> Iterable<T>.foldParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: suspend (acc: T, T) -> T
): T {
    var accumulator = initial
    for (element in this) {
        accumulator = operation(accumulator, element)
    }
    return accumulator
}

suspend inline fun <T> Iterable<T>.tryFoldParallelly(
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    return tryFoldParallelly(segment = UInt64.ten, initial = initial, operation = operation)
}

suspend inline fun <T> Iterable<T>.tryFoldParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    var accumulator = initial
    for (element in this) {
        when (val ret = operation(accumulator, element)) {
            is Ok -> accumulator = ret.value
            is Failed -> return Failed(ret.error)
            is Fatal -> return Fatal(ret.errors)
        }
    }
    return Ok(accumulator)
}

suspend inline fun <T> Iterable<T>.foldIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    return foldIndexedParallelly(segment = UInt64.ten, initial = initial, operation = operation)
}

suspend inline fun <T> Iterable<T>.foldIndexedParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    var accumulator = initial
    for ((index, element) in withIndex()) {
        accumulator = operation(index, accumulator, element)
    }
    return accumulator
}

suspend inline fun <T> Iterable<T>.tryFoldIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    return tryFoldIndexedParallelly(segment = UInt64.ten, initial = initial, operation = operation)
}

suspend inline fun <T> Iterable<T>.tryFoldIndexedParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    var accumulator = initial
    for ((index, element) in withIndex()) {
        when (val ret = operation(index, accumulator, element)) {
            is Ok -> accumulator = ret.value
            is Failed -> return Failed(ret.error)
            is Fatal -> return Fatal(ret.errors)
        }
    }
    return Ok(accumulator)
}

suspend inline fun <T> Iterable<T>.foldRightParallelly(
    initial: T,
    crossinline operation: suspend (acc: T, T) -> T
): T {
    return foldRightParallelly(segment = UInt64.ten, initial = initial, operation = operation)
}

suspend inline fun <T> Iterable<T>.foldRightParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: suspend (acc: T, T) -> T
): T {
    var accumulator = initial
    for (element in toList().asReversed()) {
        accumulator = operation(accumulator, element)
    }
    return accumulator
}

suspend inline fun <T> Iterable<T>.tryFoldRightParallelly(
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    return tryFoldRightParallelly(segment = UInt64.ten, initial = initial, operation = operation)
}

suspend inline fun <T> Iterable<T>.tryFoldRightParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    var accumulator = initial
    for (element in toList().asReversed()) {
        when (val ret = operation(accumulator, element)) {
            is Ok -> accumulator = ret.value
            is Failed -> return Failed(ret.error)
            is Fatal -> return Fatal(ret.errors)
        }
    }
    return Ok(accumulator)
}

suspend inline fun <T> Iterable<T>.foldRightIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    return foldRightIndexedParallelly(segment = UInt64.ten, initial = initial, operation = operation)
}

suspend inline fun <T> Iterable<T>.foldRightIndexedParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    var accumulator = initial
    val elements = toList()
    for (index in elements.indices.reversed()) {
        accumulator = operation(index, accumulator, elements[index])
    }
    return accumulator
}

suspend inline fun <T> Iterable<T>.tryFoldRightIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    return tryFoldRightIndexedParallelly(segment = UInt64.ten, initial = initial, operation = operation)
}

suspend inline fun <T> Iterable<T>.tryFoldRightIndexedParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    var accumulator = initial
    val elements = toList()
    for (index in elements.indices.reversed()) {
        when (val ret = operation(index, accumulator, elements[index])) {
            is Ok -> accumulator = ret.value
            is Failed -> return Failed(ret.error)
            is Fatal -> return Fatal(ret.errors)
        }
    }
    return Ok(accumulator)
}
